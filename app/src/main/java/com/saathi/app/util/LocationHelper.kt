package com.saathi.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.SystemClock
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val isFresh: Boolean,
    val provider: String = "gps"
)

object LocationHelper {

    private const val MAX_STALE_AGE_MS = 120_000L // 2 minutes
    private const val LOCATION_TIMEOUT_MS = 7_000L // 7 seconds timeout for fresh fix

    fun isValidCoordinate(latitude: Double?, longitude: Double?): Boolean {
        if (latitude == null || longitude == null) return false
        if (latitude.isNaN() || longitude.isNaN()) return false
        if (latitude == 0.0 && longitude == 0.0) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener {
            if (cont.isActive) cont.resume(null)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.resume(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(context: Context): LocationResult? {
        var bestLocation: Location? = null

        // 1. Check Android's Native LocationManager (Network, GPS, Passive)
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            if (lm != null) {
                val providers = listOf(
                    android.location.LocationManager.GPS_PROVIDER,
                    android.location.LocationManager.NETWORK_PROVIDER,
                    android.location.LocationManager.PASSIVE_PROVIDER
                )
                for (p in providers) {
                    try {
                        if (lm.isProviderEnabled(p)) {
                            val l = lm.getLastKnownLocation(p)
                            if (l != null && isValidCoordinate(l.latitude, l.longitude)) {
                                if (bestLocation == null || l.time > bestLocation.time) {
                                    bestLocation = l
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        if (bestLocation != null) {
            val isRecent = (System.currentTimeMillis() - bestLocation.time) < MAX_STALE_AGE_MS
            return LocationResult(
                latitude = bestLocation.latitude,
                longitude = bestLocation.longitude,
                accuracy = bestLocation.accuracy,
                timestamp = bestLocation.time,
                isFresh = isRecent,
                provider = bestLocation.provider ?: "system"
            )
        }
        return null
    }

    @SuppressLint("MissingPermission")
    suspend fun getBestAvailableLocation(context: Context): LocationResult? {
        val sysLoc = getLastKnownLocation(context)
        if (sysLoc != null && sysLoc.isFresh) {
            return sysLoc
        }

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val lastLoc: Location? = fusedLocationClient.lastLocation.awaitTask()
            if (lastLoc != null && isValidCoordinate(lastLoc.latitude, lastLoc.longitude)) {
                if (sysLoc == null || lastLoc.time > sysLoc.timestamp) {
                    return LocationResult(
                        latitude = lastLoc.latitude,
                        longitude = lastLoc.longitude,
                        accuracy = lastLoc.accuracy,
                        timestamp = lastLoc.time,
                        isFresh = (System.currentTimeMillis() - lastLoc.time) < MAX_STALE_AGE_MS,
                        provider = lastLoc.provider ?: "fused_cache"
                    )
                }
            }
        } catch (_: Exception) {}

        return sysLoc
    }

    @SuppressLint("MissingPermission")
    suspend fun getFreshLocation(context: Context, timeoutMs: Long = 2500L): LocationResult? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Pre-fetch best cached location immediately as instant fallback
        val cachedFallback = getBestAvailableLocation(context)

        // Try to fetch fresh high-accuracy location within timeout
        try {
            val cts = CancellationTokenSource()
            val freshLocation: Location? = withTimeoutOrNull(timeoutMs) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).awaitTask()
            }

            if (freshLocation != null && isValidCoordinate(freshLocation.latitude, freshLocation.longitude)) {
                return LocationResult(
                    latitude = freshLocation.latitude,
                    longitude = freshLocation.longitude,
                    accuracy = freshLocation.accuracy,
                    timestamp = freshLocation.time,
                    isFresh = true,
                    provider = freshLocation.provider ?: "fused_fresh"
                )
            }
        } catch (_: SecurityException) {
            return cachedFallback
        } catch (_: Exception) {
            // Fall through to cached location
        }

        return cachedFallback
    }

    /**
     * Calculates distance in kilometers between two coordinates
     */
    fun calculateDistanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000f
    }
}
