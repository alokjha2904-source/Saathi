package com.saathi.app.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.saathi.app.data.local.PreferencesManager
import com.saathi.app.data.local.SaathiDatabase
import com.saathi.app.data.model.EmergencyEvent
import com.saathi.app.data.model.TrustedContact
import com.saathi.app.util.LiveLocationTracker
import com.saathi.app.util.LocationHelper
import com.saathi.app.util.LocationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ContactDispatchStatus {
    QUEUED,
    SENDING,
    SENT,
    DEMO_SIMULATED,
    PERMISSION_DENIED,
    INVALID_PHONE,
    FAILED
}

data class ContactDispatchDetail(
    val contactId: Int,
    val name: String,
    val phone: String,
    val status: ContactDispatchStatus,
    val detailMessage: String
)

data class DispatchResult(
    val totalContacts: Int,
    val successfulDispatches: Int,
    val isDemo: Boolean,
    val hasSmsPermission: Boolean,
    val details: List<ContactDispatchDetail>,
    val generalError: String? = null
)

object EmergencyDispatcher {

    private val _latestDispatchResult = MutableStateFlow<DispatchResult?>(null)
    val latestDispatchResult: StateFlow<DispatchResult?> = _latestDispatchResult.asStateFlow()

    fun resetDispatchResult() {
        _latestDispatchResult.value = null
    }

    /**
     * Formats the exact emergency SMS message per Phase 3 specifications
     */
    fun buildEmergencyMessage(
        userName: String,
        userPhone: String,
        latitude: Double?,
        longitude: Double?,
        accuracyMeters: Float?,
        timestamp: Long,
        incidentType: String,
        eventId: String
    ): String {
        val nameStr = userName.ifBlank { "User" }
        val phoneStr = userPhone.ifBlank { "Not set" }

        val locationStr = if (latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
        ) {
            "https://maps.google.com/?q=$latitude,$longitude"
        } else {
            "GPS location acquisition pending"
        }

        val accuracyStr = if (accuracyMeters != null && accuracyMeters > 0f) {
            "${accuracyMeters.toInt()} meters"
        } else {
            "Accuracy pending"
        }

        val timeStr = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        val incidentStr = incidentType.ifBlank { "SOS activated" }

        return "SAATHI EMERGENCY ALERT\n\n" +
                "Name: $nameStr\n" +
                "Phone: $phoneStr\n\n" +
                "I may be in danger and need immediate help.\n\n" +
                "Incident: $incidentStr\n" +
                "Time: $timeStr\n\n" +
                "Current Location:\n$locationStr\n\n" +
                "Location Accuracy: $accuracyStr\n\n" +
                "Please contact me immediately.\n" +
                "If I cannot answer, please use the location above and contact emergency services if necessary.\n\n" +
                "SAATHI Event ID: $eventId"
    }

    private var pendingLocationEventId: String? = null

    fun getSmsManager(context: Context): SmsManager? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val subId = SubscriptionManager.getDefaultSmsSubscriptionId()
                val sysManager = context.getSystemService(SmsManager::class.java)
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && sysManager != null) {
                    sysManager.createForSubscriptionId(subId)
                } else {
                    sysManager ?: @Suppress("DEPRECATION") SmsManager.getDefault()
                }
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun createShareIntent(message: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createSmsIntent(phone: String, message: String): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    suspend fun dispatchLocationUpdateIfPending(context: Context, eventId: String, loc: LocationResult) {
        if (pendingLocationEventId != eventId) return
        if (!LocationHelper.isValidCoordinate(loc.latitude, loc.longitude)) return
        pendingLocationEventId = null

        withContext(Dispatchers.IO) {
            try {
                val db = SaathiDatabase.getDatabase(context)
                val allContacts = db.trustedContactDao().getAllTrustedContacts().first()
                val sosContacts = allContacts.filter { it.notifyOnSOS }
                if (sosContacts.isEmpty()) return@withContext

                val hasSmsPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasSmsPermission) return@withContext

                val smsManager = getSmsManager(context) ?: return@withContext

                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(loc.timestamp))
                val updateMessage = "SAATHI LIVE LOCATION UPDATE:\n" +
                        "https://maps.google.com/?q=${loc.latitude},${loc.longitude}\n" +
                        "Accuracy: ±${loc.accuracy.toInt()}m\n" +
                        "Time: $timeStr | Event ID: $eventId"

                for (contact in sosContacts) {
                    val cleanPhone = contact.phone.replace(Regex("[^0-9+]"), "")
                    val digitsOnly = cleanPhone.replace("+", "")
                    if (digitsOnly.length in 7..15) {
                        try {
                            val parts = smsManager.divideMessage(updateMessage)
                            if (parts.size > 1) {
                                smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
                            } else {
                                smsManager.sendTextMessage(cleanPhone, null, updateMessage, null, null)
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun dispatchSOSAlert(
        context: Context,
        event: EmergencyEvent,
        accuracyMeters: Float? = null,
        isDemoMode: Boolean = false
    ): DispatchResult = withContext(Dispatchers.IO) {
        try {
            val db = SaathiDatabase.getDatabase(context)
            val preferencesManager = PreferencesManager(context)
            val userName = preferencesManager.userName.first()
            val userPhone = preferencesManager.userPhone.first()

            val allContacts = db.trustedContactDao().getAllTrustedContacts().first()
            val sosContacts = allContacts.filter { it.notifyOnSOS }

            if (sosContacts.isEmpty()) {
                val emptyResult = DispatchResult(
                    totalContacts = 0,
                    successfulDispatches = 0,
                    isDemo = isDemoMode,
                    hasSmsPermission = false,
                    details = emptyList(),
                    generalError = "No trusted contacts in Safety Circle configured for SOS alerts."
                )
                _latestDispatchResult.value = emptyResult
                return@withContext emptyResult
            }

            // Location Guarantee: If coordinates are missing or invalid, resolve immediately!
            var targetLat = event.latitude
            var targetLng = event.longitude
            var targetAcc = accuracyMeters

            if (isDemoMode) {
                if (!LocationHelper.isValidCoordinate(targetLat, targetLng)) {
                    targetLat = 20.3541
                    targetLng = 85.8145
                    targetAcc = 8f
                }
            } else if (!LocationHelper.isValidCoordinate(targetLat, targetLng)) {
                // 1. Try LiveLocationTracker current location
                val liveLoc = LiveLocationTracker.currentLocation.value
                if (liveLoc != null && LocationHelper.isValidCoordinate(liveLoc.latitude, liveLoc.longitude)) {
                    targetLat = liveLoc.latitude
                    targetLng = liveLoc.longitude
                    targetAcc = liveLoc.accuracy
                } else {
                    // 2. Try best available immediate cached/network location
                    val bestLoc = LocationHelper.getBestAvailableLocation(context)
                    if (bestLoc != null && LocationHelper.isValidCoordinate(bestLoc.latitude, bestLoc.longitude)) {
                        targetLat = bestLoc.latitude
                        targetLng = bestLoc.longitude
                        targetAcc = bestLoc.accuracy
                    } else {
                        // 3. Quick fresh fix wait (up to 2 seconds)
                        val freshLoc = LocationHelper.getFreshLocation(context, timeoutMs = 2000L)
                        if (freshLoc != null && LocationHelper.isValidCoordinate(freshLoc.latitude, freshLoc.longitude)) {
                            targetLat = freshLoc.latitude
                            targetLng = freshLoc.longitude
                            targetAcc = freshLoc.accuracy
                        }
                    }
                }

                // If coordinates were acquired now, update database record and LiveLocationTracker
                if (LocationHelper.isValidCoordinate(targetLat, targetLng)) {
                    try {
                        val existing = db.emergencyEventDao().getEmergencyEventByEventId(event.eventId)
                        if (existing != null) {
                            db.emergencyEventDao().updateEmergencyEvent(
                                existing.copy(
                                    latitude = targetLat,
                                    longitude = targetLng,
                                    locationName = "Location Acquired: ±${targetAcc?.toInt() ?: 0}m"
                                )
                            )
                        }
                    } catch (_: Exception) {}
                }
            }

            val hasValidLocation = LocationHelper.isValidCoordinate(targetLat, targetLng)
            if (!hasValidLocation && !isDemoMode) {
                pendingLocationEventId = event.eventId
            } else {
                pendingLocationEventId = null
            }

            val alertText = buildEmergencyMessage(
                userName = userName,
                userPhone = userPhone,
                latitude = targetLat,
                longitude = targetLng,
                accuracyMeters = targetAcc,
                timestamp = event.timestamp,
                incidentType = event.type,
                eventId = event.eventId
            )

            // 1. DEMO MODE: NEVER send real SMS
            if (isDemoMode) {
                val demoDetails = sosContacts.map { contact ->
                    ContactDispatchDetail(
                        contactId = contact.id,
                        name = contact.name,
                        phone = contact.phone,
                        status = ContactDispatchStatus.DEMO_SIMULATED,
                        detailMessage = "Simulated alert sent (Demo Mode)"
                    )
                }
                val demoResult = DispatchResult(
                    totalContacts = sosContacts.size,
                    successfulDispatches = sosContacts.size,
                    isDemo = true,
                    hasSmsPermission = true,
                    details = demoDetails
                )
                _latestDispatchResult.value = demoResult
                return@withContext demoResult
            }

            // 2. REAL MODE: Check android.permission.SEND_SMS
            val hasSmsPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasSmsPermission) {
                val deniedDetails = sosContacts.map { contact ->
                    ContactDispatchDetail(
                        contactId = contact.id,
                        name = contact.name,
                        phone = contact.phone,
                        status = ContactDispatchStatus.PERMISSION_DENIED,
                        detailMessage = "SMS permission is disabled."
                    )
                }
                val permissionDeniedResult = DispatchResult(
                    totalContacts = sosContacts.size,
                    successfulDispatches = 0,
                    isDemo = false,
                    hasSmsPermission = false,
                    details = deniedDetails,
                    generalError = "SMS permission is disabled. Grant SMS permission or use manual share."
                )
                _latestDispatchResult.value = permissionDeniedResult
                return@withContext permissionDeniedResult
            }

            // 3. REAL MODE with permission granted: Send individual SMS with robust SmsManager
            val smsManager = getSmsManager(context)
            if (smsManager == null) {
                val noSmsMgrResult = DispatchResult(
                    totalContacts = sosContacts.size,
                    successfulDispatches = 0,
                    isDemo = false,
                    hasSmsPermission = true,
                    details = sosContacts.map {
                        ContactDispatchDetail(
                            contactId = it.id,
                            name = it.name,
                            phone = it.phone,
                            status = ContactDispatchStatus.FAILED,
                            detailMessage = "SMS service unavailable on device"
                        )
                    },
                    generalError = "SmsManager service unavailable"
                )
                _latestDispatchResult.value = noSmsMgrResult
                return@withContext noSmsMgrResult
            }

            val contactResults = mutableListOf<ContactDispatchDetail>()
            var successfulCount = 0

            for (contact in sosContacts) {
                val cleanPhone = contact.phone.replace(Regex("[^0-9+]"), "")

                // Validate phone number length (E.164: 7 to 15 digits)
                val digitsOnly = cleanPhone.replace("+", "")
                if (digitsOnly.length < 7 || digitsOnly.length > 15) {
                    contactResults.add(
                        ContactDispatchDetail(
                            contactId = contact.id,
                            name = contact.name,
                            phone = contact.phone,
                            status = ContactDispatchStatus.INVALID_PHONE,
                            detailMessage = "Invalid phone number format"
                        )
                    )
                    continue
                }

                try {
                    val parts = smsManager.divideMessage(alertText)
                    if (parts.size > 1) {
                        try {
                            smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
                        } catch (_: Exception) {
                            // Fallback to sending single text if multipart throws
                            smsManager.sendTextMessage(cleanPhone, null, alertText.take(160), null, null)
                        }
                    } else {
                        smsManager.sendTextMessage(cleanPhone, null, alertText, null, null)
                    }

                    successfulCount++
                    contactResults.add(
                        ContactDispatchDetail(
                            contactId = contact.id,
                            name = contact.name,
                            phone = cleanPhone,
                            status = ContactDispatchStatus.SENT,
                            detailMessage = "SMS dispatched successfully"
                        )
                    )
                } catch (e: Exception) {
                    contactResults.add(
                        ContactDispatchDetail(
                            contactId = contact.id,
                            name = contact.name,
                            phone = cleanPhone,
                            status = ContactDispatchStatus.FAILED,
                            detailMessage = e.localizedMessage ?: "Carrier dispatch failed"
                        )
                    )
                }
            }

            val realResult = DispatchResult(
                totalContacts = sosContacts.size,
                successfulDispatches = successfulCount,
                isDemo = false,
                hasSmsPermission = true,
                details = contactResults
            )
            _latestDispatchResult.value = realResult
            realResult
        } catch (e: Exception) {
            val errResult = DispatchResult(
                totalContacts = 0,
                successfulDispatches = 0,
                isDemo = isDemoMode,
                hasSmsPermission = false,
                details = emptyList(),
                generalError = e.localizedMessage
            )
            _latestDispatchResult.value = errResult
            errResult
        }
    }
}
