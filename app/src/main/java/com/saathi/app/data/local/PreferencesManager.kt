package com.saathi.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    companion object {
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
        val IS_DEMO_MODE = booleanPreferencesKey("is_demo_mode")
        val IS_SILENT_SOS = booleanPreferencesKey("is_silent_sos")
        val IS_SHAKE_SOS = booleanPreferencesKey("is_shake_sos")
        val IS_VOLUME_DOWN_SOS = booleanPreferencesKey("is_volume_down_sos")
        val VOLUME_DOWN_HOLD_SECONDS = intPreferencesKey("volume_down_hold_seconds")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val IS_HIGH_CONTRAST = booleanPreferencesKey("is_high_contrast")
        val IS_LARGE_TEXT = booleanPreferencesKey("is_large_text")
        val IS_REDUCED_MOTION = booleanPreferencesKey("is_reduced_motion")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_PHONE = stringPreferencesKey("user_phone")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_FOUR_TAP_SOS = booleanPreferencesKey("is_four_tap_sos")
        val IS_EMERGENCY_RECORDING = booleanPreferencesKey("is_emergency_recording")
        val EMERGENCY_CALL_NUMBER = stringPreferencesKey("emergency_call_number")
        val IS_EMERGENCY_LOCK_MODE = booleanPreferencesKey("is_emergency_lock_mode")
        val IS_LIVE_LOCATION_ENABLED = booleanPreferencesKey("is_live_location_enabled")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[IS_ONBOARDING_COMPLETE] ?: false }
    val isOnboardingCompleteFlow: Flow<Boolean> = isOnboardingComplete
    val isDemoMode: Flow<Boolean> = context.dataStore.data.map { it[IS_DEMO_MODE] ?: false }
    val isDemoModeFlow: Flow<Boolean> = isDemoMode
    val isSilentSOS: Flow<Boolean> = context.dataStore.data.map { it[IS_SILENT_SOS] ?: false }
    val isShakeSOS: Flow<Boolean> = context.dataStore.data.map { it[IS_SHAKE_SOS] ?: true }
    val isVolumeDownSOS: Flow<Boolean> = context.dataStore.data.map { it[IS_VOLUME_DOWN_SOS] ?: true }
    val volumeDownHoldSeconds: Flow<Int> = context.dataStore.data.map { it[VOLUME_DOWN_HOLD_SECONDS] ?: 3 }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "English" }
    val theme: Flow<String> = context.dataStore.data.map { it[THEME] ?: "System" }
    val isHighContrast: Flow<Boolean> = context.dataStore.data.map { it[IS_HIGH_CONTRAST] ?: false }
    val isLargeText: Flow<Boolean> = context.dataStore.data.map { it[IS_LARGE_TEXT] ?: false }
    val isReducedMotion: Flow<Boolean> = context.dataStore.data.map { it[IS_REDUCED_MOTION] ?: false }
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME] ?: "" }
    val userPhone: Flow<String> = context.dataStore.data.map { it[USER_PHONE] ?: "" }
    val userEmail: Flow<String> = context.dataStore.data.map { it[USER_EMAIL] ?: "" }
    val isFourTapSOS: Flow<Boolean> = context.dataStore.data.map { it[IS_FOUR_TAP_SOS] ?: true }
    val isEmergencyRecording: Flow<Boolean> = context.dataStore.data.map { it[IS_EMERGENCY_RECORDING] ?: true }
    val emergencyCallNumber: Flow<String> = context.dataStore.data.map { it[EMERGENCY_CALL_NUMBER] ?: "112" }
    val isEmergencyLockMode: Flow<Boolean> = context.dataStore.data.map { it[IS_EMERGENCY_LOCK_MODE] ?: true }
    val isLiveLocationEnabled: Flow<Boolean> = context.dataStore.data.map { it[IS_LIVE_LOCATION_ENABLED] ?: true }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }

    suspend fun setOnboardingComplete(completed: Boolean) {
        context.dataStore.edit { it[IS_ONBOARDING_COMPLETE] = completed }
    }

    suspend fun setDemoMode(isDemo: Boolean) {
        context.dataStore.edit { it[IS_DEMO_MODE] = isDemo }
    }

    suspend fun setSilentSOS(isSilent: Boolean) {
        context.dataStore.edit { it[IS_SILENT_SOS] = isSilent }
    }

    suspend fun setShakeSOS(enabled: Boolean) {
        context.dataStore.edit { it[IS_SHAKE_SOS] = enabled }
    }

    suspend fun setVolumeDownSOS(enabled: Boolean) {
        context.dataStore.edit { it[IS_VOLUME_DOWN_SOS] = enabled }
    }

    suspend fun setVolumeDownHoldSeconds(seconds: Int) {
        context.dataStore.edit { it[VOLUME_DOWN_HOLD_SECONDS] = seconds }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE] = lang }
    }

    suspend fun setTheme(themeValue: String) {
        context.dataStore.edit { it[THEME] = themeValue }
    }

    suspend fun setHighContrast(isHigh: Boolean) {
        context.dataStore.edit { it[IS_HIGH_CONTRAST] = isHigh }
    }

    suspend fun setLargeText(isLarge: Boolean) {
        context.dataStore.edit { it[IS_LARGE_TEXT] = isLarge }
    }

    suspend fun setReducedMotion(isReduced: Boolean) {
        context.dataStore.edit { it[IS_REDUCED_MOTION] = isReduced }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[USER_NAME] = name }
    }

    suspend fun setUserPhone(phone: String) {
        context.dataStore.edit { it[USER_PHONE] = phone }
    }

    suspend fun setUserEmail(email: String) {
        context.dataStore.edit { it[USER_EMAIL] = email }
    }

    suspend fun setFourTapSOS(enabled: Boolean) {
        context.dataStore.edit { it[IS_FOUR_TAP_SOS] = enabled }
    }

    suspend fun setEmergencyRecording(enabled: Boolean) {
        context.dataStore.edit { it[IS_EMERGENCY_RECORDING] = enabled }
    }

    suspend fun setEmergencyCallNumber(number: String) {
        context.dataStore.edit { it[EMERGENCY_CALL_NUMBER] = number }
    }

    suspend fun setEmergencyLockMode(enabled: Boolean) {
        context.dataStore.edit { it[IS_EMERGENCY_LOCK_MODE] = enabled }
    }

    suspend fun setLiveLocationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_LIVE_LOCATION_ENABLED] = enabled }
    }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { it[IS_LOGGED_IN] = loggedIn }
    }
}
