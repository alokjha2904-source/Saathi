package com.saathi.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult as GmsLocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.saathi.app.data.local.SaathiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

object LiveLocationTracker {

    private const val UPDATE_INTERVAL_MS = 10_000L // 10 seconds
    private const val FASTEST_INTERVAL_MS = 5_000L // 5 seconds
    private const val STALE_THRESHOLD_MS = 30_000L // 30 seconds

    private var fusedClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var trackerScope = CoroutineScope(Dispatchers.IO)
    private var demoJob: Job? = null
    private var activeEventId: String? = null

    private val _currentLocation = MutableStateFlow<LocationResult?>(null)
    val currentLocation: StateFlow<LocationResult?> = _currentLocation.asStateFlow()

    private val _locationHistory = MutableStateFlow<List<LocationResult>>(emptyList())
    val locationHistory: StateFlow<List<LocationResult>> = _locationHistory.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _updateCount = MutableStateFlow(0)
    val updateCount: StateFlow<Int> = _updateCount.asStateFlow()

    private val _lastUpdateTimestamp = MutableStateFlow(0L)
    val lastUpdateTimestamp: StateFlow<Long> = _lastUpdateTimestamp.asStateFlow()

    private val _isStale = MutableStateFlow(false)
    val isStale: StateFlow<Boolean> = _isStale.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startTracking(context: Context, eventId: String? = null, isDemo: Boolean = false) {
        if (_isTracking.value) return
        activeEventId = eventId
        _isTracking.value = true

        if (isDemo) {
            startDemoSimulation(context, eventId)
            return
        }

        try {
            val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
            fusedClient = client

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: GmsLocationResult) {
                    val lastLoc: Location? = result.lastLocation
                    if (lastLoc != null && LocationHelper.isValidCoordinate(lastLoc.latitude, lastLoc.longitude)) {
                        val locRes = LocationResult(
                            latitude = lastLoc.latitude,
                            longitude = lastLoc.longitude,
                            accuracy = lastLoc.accuracy,
                            timestamp = System.currentTimeMillis(),
                            isFresh = true,
                            provider = lastLoc.provider ?: "gps"
                        )
                        recordLocationUpdate(context, locRes, eventId)
                    }
                }
            }

            locationCallback = callback
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())

            // Initial immediate fetch
            trackerScope.launch {
                val initial = LocationHelper.getFreshLocation(context)
                if (initial != null) {
                    recordLocationUpdate(context, initial, eventId)
                }
            }
        } catch (_: SecurityException) {
            _isTracking.value = false
        } catch (_: Exception) {
            _isTracking.value = false
        }
    }

    private fun startDemoSimulation(context: Context, eventId: String?) {
        demoJob?.cancel()
        demoJob = trackerScope.launch {
            var baseLat = 20.3541
            var baseLng = 85.8145
            while (isActive && _isTracking.value) {
                // Realistic slight movement jitter around campus (~10-25m)
                val jitterLat = (Random.nextDouble(-0.00015, 0.00015))
                val jitterLng = (Random.nextDouble(-0.00015, 0.00015))
                baseLat += jitterLat
                baseLng += jitterLng
                val accuracy = Random.nextFloat() * 4f + 4f // 4m to 8m accuracy

                val simulated = LocationResult(
                    latitude = baseLat,
                    longitude = baseLng,
                    accuracy = accuracy,
                    timestamp = System.currentTimeMillis(),
                    isFresh = true,
                    provider = "demo_gps"
                )
                recordLocationUpdate(context, simulated, eventId)
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun recordLocationUpdate(context: Context, loc: LocationResult, eventId: String?) {
        _currentLocation.value = loc
        _lastUpdateTimestamp.value = loc.timestamp
        _isStale.value = false
        _updateCount.value += 1

        val currentList = _locationHistory.value.toMutableList()
        currentList.add(loc)
        if (currentList.size > 50) {
            currentList.removeAt(0)
        }
        _locationHistory.value = currentList

        // Update database active event if eventId is set
        if (eventId != null) {
            trackerScope.launch {
                try {
                    val db = SaathiDatabase.getDatabase(context)
                    val existing = db.emergencyEventDao().getEmergencyEventByEventId(eventId)
                    if (existing != null) {
                        db.emergencyEventDao().updateEmergencyEvent(
                            existing.copy(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                locationName = "Live Tracking: ±${loc.accuracy.toInt()}m"
                            )
                        )
                    }
                    com.saathi.app.service.EmergencyDispatcher.dispatchLocationUpdateIfPending(context, eventId, loc)
                } catch (_: Exception) {}
            }
        }
    }

    fun stopTracking() {
        _isTracking.value = false
        demoJob?.cancel()
        demoJob = null
        try {
            locationCallback?.let {
                fusedClient?.removeLocationUpdates(it)
            }
        } catch (_: Exception) {}
        locationCallback = null
        fusedClient = null
    }

    fun reset() {
        stopTracking()
        _currentLocation.value = null
        _locationHistory.value = emptyList()
        _updateCount.value = 0
        _lastUpdateTimestamp.value = 0L
        _isStale.value = false
    }
}
