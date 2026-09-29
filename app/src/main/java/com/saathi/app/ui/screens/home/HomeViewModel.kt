package com.saathi.app.ui.screens.home

import android.app.Application
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.saathi.app.data.local.PreferencesManager
import com.saathi.app.data.local.SaathiDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SaathiDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _safetyStatus = MutableStateFlow(SafetyStatus.SAFE)
    val safetyStatus = _safetyStatus.asStateFlow()

    private val _activeEventId = MutableStateFlow<String?>(null)
    val activeEventId = _activeEventId.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val _isNetworkConnected = MutableStateFlow(true)
    val isNetworkConnected = _isNetworkConnected.asStateFlow()

    private val _trustedContactCount = MutableStateFlow(0)
    val trustedContactCount = _trustedContactCount.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode = _isDemoMode.asStateFlow()

    private val _language = MutableStateFlow("English")
    val language = _language.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Collect demo mode continuously
        viewModelScope.launch {
            try {
                preferencesManager.isDemoMode.collect { isDemo ->
                    _isDemoMode.value = isDemo
                }
            } catch (_: Exception) { }
        }

        // Collect language continuously
        viewModelScope.launch {
            try {
                preferencesManager.language.collect { lang ->
                    _language.value = lang
                }
            } catch (_: Exception) { }
        }

        // Collect contacts count continuously
        viewModelScope.launch {
            try {
                database.trustedContactDao().getAllTrustedContacts().collect { contacts ->
                    _trustedContactCount.value = contacts.size
                }
            } catch (_: Exception) { }
        }

        // Collect active emergency events dynamically
        viewModelScope.launch {
            try {
                database.emergencyEventDao().getAllEmergencyEvents().collect { events ->
                    val activeEvent = events.firstOrNull { it.status.equals("ACTIVE", ignoreCase = true) }
                    if (activeEvent != null) {
                        _safetyStatus.value = SafetyStatus.EMERGENCY_ACTIVE
                        _activeEventId.value = activeEvent.eventId.ifEmpty { activeEvent.id.toString() }
                    } else if (_safetyStatus.value == SafetyStatus.EMERGENCY_ACTIVE) {
                        _safetyStatus.value = SafetyStatus.SAFE
                        _activeEventId.value = null
                    }
                }
            } catch (_: Exception) { }
        }

        updateBatteryLevel()
        updateNetworkStatus()
        updateLocation()
    }

    fun setSafetyStatus(status: SafetyStatus) {
        _safetyStatus.value = status
    }

    private fun updateBatteryLevel() {
        try {
            val bm = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            _batteryLevel.value = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (_: Exception) { }
    }

    private fun updateNetworkStatus() {
        try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)
            _isNetworkConnected.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (_: Exception) { }
    }

    private fun updateLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                _currentLocation.value = location
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) { }
    }
}

enum class SafetyStatus {
    SAFE, CHECK_IN_ACTIVE, EMERGENCY_ACTIVE
}
