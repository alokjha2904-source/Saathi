package com.saathi.app.ui.screens.emergency

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.saathi.app.Screen
import com.saathi.app.data.local.PreferencesManager
import com.saathi.app.data.local.SaathiDatabase
import com.saathi.app.data.model.EmergencyEvent
import com.saathi.app.service.ContactDispatchStatus
import com.saathi.app.service.EmergencyDispatcher
import com.saathi.app.service.EmergencyRecorder
import com.saathi.app.service.EmergencyService
import com.saathi.app.ui.components.LiveLocationMap
import com.saathi.app.ui.theme.ErrorSOS
import com.saathi.app.ui.theme.PrimaryTeal
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SecondaryAmber
import com.saathi.app.util.BiometricHelper
import com.saathi.app.util.LiveLocationTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmergencyLockScreen(
    navController: NavController,
    eventId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    val isDemoMode by preferencesManager.isDemoMode.collectAsState(initial = false)

    var emergencyEvent by remember { mutableStateOf<EmergencyEvent?>(null) }
    val dispatchResult by EmergencyDispatcher.latestDispatchResult.collectAsState()
    val isRecording by EmergencyRecorder.isRecording.collectAsState()
    val isTrackingLocation by LiveLocationTracker.isTracking.collectAsState()
    val currentLocation by LiveLocationTracker.currentLocation.collectAsState()
    val locationHistory by LiveLocationTracker.locationHistory.collectAsState()
    val isStaleLocation by LiveLocationTracker.isStale.collectAsState()
    val lastLocationTimestamp by LiveLocationTracker.lastUpdateTimestamp.collectAsState()

    var showAuthenticatedDialog by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var showTacticalMap by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Elapsed timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    // Load event & ensure tracking, service, SMS dispatch, and recording are active
    LaunchedEffect(eventId, isDemoMode) {
        val db = SaathiDatabase.getDatabase(context)
        var event = emergencyEvent
        try {
            event = db.emergencyEventDao().getEmergencyEventByEventId(eventId)
            emergencyEvent = event
        } catch (_: Exception) {}

        if (!isTrackingLocation) {
            LiveLocationTracker.startTracking(context, eventId, isDemo = isDemoMode)
        }

        try {
            EmergencyService.startEmergency(context, eventId)
        } catch (_: Exception) {}

        // Fail-safe: ensure SMS dispatch is triggered if not already completed
        if (EmergencyDispatcher.latestDispatchResult.value == null) {
            val resolvedEvent = event ?: EmergencyEvent(
                eventId = eventId,
                type = "SOS_MANUAL",
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude,
                status = "ACTIVE",
                description = "SOS confirmed via Saathi",
                timestamp = System.currentTimeMillis(),
                isDemo = isDemoMode
            )
            EmergencyDispatcher.dispatchSOSAlert(
                context = context,
                event = resolvedEvent,
                accuracyMeters = currentLocation?.accuracy,
                isDemoMode = isDemoMode
            )
        }

        // Fail-safe: ensure recording is started if enabled and not already running
        try {
            val isRecordingEnabled = preferencesManager.isEmergencyRecording.first()
            if (isRecordingEnabled && !isRecording) {
                EmergencyRecorder.startAutomaticEmergencyRecording(context, eventId, isDemo = isDemoMode)
            }
        } catch (_: Exception) {}
    }

    val secondsAgo = remember(lastLocationTimestamp, elapsedSeconds) {
        if (lastLocationTimestamp > 0L) {
            maxOf(0L, (System.currentTimeMillis() - lastLocationTimestamp) / 1000L)
        } else {
            elapsedSeconds
        }
    }

    // Authenticated Exit Confirmation Dialog
    if (showAuthenticatedDialog) {
        AlertDialog(
            onDismissRequest = { showAuthenticatedDialog = false },
            title = {
                Text(
                    "Emergency Mode Active",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        "Authentication verified successfully.",
                        fontWeight = FontWeight.SemiBold,
                        color = SafeGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Are you sure you want to end the emergency?\nOnly cancel if you are in a completely secure location.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAuthenticatedDialog = false
                        coroutineScope.launch {
                            try {
                                val db = SaathiDatabase.getDatabase(context)
                                val existing = db.emergencyEventDao().getEmergencyEventByEventId(eventId)
                                if (existing != null) {
                                    db.emergencyEventDao().updateEmergencyEvent(
                                        existing.copy(
                                            status = "RESOLVED",
                                            endTimestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                                EmergencyService.stopEmergency(context)
                                EmergencyRecorder.stopAllRecording()
                                LiveLocationTracker.stopTracking()
                            } catch (_: Exception) {}

                            navController.navigate("post_emergency/$eventId") {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorSOS)
                ) {
                    Text("END EMERGENCY", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showAuthenticatedDialog = false
                        navController.navigate("emergency_active/$eventId")
                    }) {
                        Text("FULL CONTROLS")
                    }
                    TextButton(onClick = { showAuthenticatedDialog = false }) {
                        Text("KEEP ACTIVE", color = SafeGreen)
                    }
                }
            }
        )
    }

    // Ultra-Dark Minimal Privacy Canvas
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070707))
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))

                // Minimal Brand Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ErrorSOS, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAATHI EMERGENCY LOCK",
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "EMERGENCY ACTIVE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = ErrorSOS,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Event ID: #$eventId",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.6f)
                )

                if (isDemoMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = SecondaryAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryAmber.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "DEMONSTRATION ONLY — NO REAL ALERTS",
                            color = SecondaryAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Minimal Privacy HUD Cards
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Location sharing indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(SafeGreen, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LOCATION SHARING",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (isTrackingLocation) "ACTIVE" else "INITIALIZING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTrackingLocation) SafeGreen else SecondaryAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Trusted contacts indicator
                        val contactsDispatched = dispatchResult?.successfulDispatches ?: 0
                        val totalContacts = dispatchResult?.totalContacts ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(PrimaryTeal, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TRUSTED CONTACTS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = when {
                                    contactsDispatched > 0 -> "$contactsDispatched NOTIFIED"
                                    totalContacts == 0 && dispatchResult != null -> "0 CONTACTS"
                                    else -> "PROCESSING"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (contactsDispatched > 0) SafeGreen else SecondaryAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Audio recording indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(if (isRecording) ErrorSOS else Color.Gray, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AUDIO EVIDENCE RECORDING",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (isRecording) "RECORDING" else "IDLE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) ErrorSOS else Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val timeStr = if (secondsAgo < 60) "${secondsAgo}s ago" else "${secondsAgo / 60}m ago"
                            Text(
                                text = "Location updated: $timeStr",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            val accuracyStr = currentLocation?.let { "±${it.accuracy.toInt()}m" } ?: "Acquiring"
                            Text(
                                text = "Accuracy: $accuracyStr",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isStaleLocation) SecondaryAmber else SafeGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tactical Live Location Radar Map
                if (showTacticalMap) {
                    LiveLocationMap(
                        currentLocation = currentLocation,
                        locationHistory = locationHistory,
                        isTracking = isTrackingLocation,
                        isStale = isStaleLocation
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Verified Real System Status Checklist (Feature 6)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SYSTEM STATUS CHECKLIST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 1. SOS CONFIRMED
                        StatusItem("SOS CONFIRMED", statusText = "ACTIVE", statusType = StatusType.SUCCESS)

                        // 2. LOCATION ACQUIRED
                        val locationStatus = when {
                            currentLocation != null -> StatusType.SUCCESS to "ACQUIRED"
                            else -> StatusType.PENDING to "ACQUIRING..."
                        }
                        StatusItem("LOCATION ACQUIRED", statusText = locationStatus.second, statusType = locationStatus.first)

                        // 3. SMS DISPATCH
                        val hasSmsPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.SEND_SMS
                        ) == PackageManager.PERMISSION_GRANTED

                        val smsStatus = when {
                            dispatchResult == null -> StatusType.PENDING to "DISPATCHING..."
                            dispatchResult!!.successfulDispatches > 0 -> StatusType.SUCCESS to "DISPATCHED (${dispatchResult!!.successfulDispatches})"
                            dispatchResult!!.isDemo -> StatusType.SUCCESS to "SIMULATED (DEMO)"
                            !dispatchResult!!.hasSmsPermission || !hasSmsPermission -> StatusType.WARNING to "PERMISSION NEEDED"
                            dispatchResult!!.totalContacts == 0 -> StatusType.WARNING to "0 SOS CONTACTS"
                            else -> StatusType.WARNING to "FAILED"
                        }
                        StatusItem("SMS DISPATCH", statusText = smsStatus.second, statusType = smsStatus.first)

                        // 4. CONTACTS NOTIFIED
                        val contactsStatus = when {
                            dispatchResult == null -> StatusType.PENDING to "NOTIFYING..."
                            dispatchResult!!.successfulDispatches > 0 -> StatusType.SUCCESS to "${dispatchResult!!.successfulDispatches} NOTIFIED"
                            dispatchResult!!.isDemo -> StatusType.SUCCESS to "${dispatchResult!!.totalContacts} SIMULATED"
                            dispatchResult!!.totalContacts == 0 -> StatusType.WARNING to "0 IN CIRCLE"
                            !dispatchResult!!.hasSmsPermission || !hasSmsPermission -> StatusType.WARNING to "PERMISSION NEEDED"
                            else -> StatusType.WARNING to "NOT SENT"
                        }
                        StatusItem("CONTACTS NOTIFIED", statusText = contactsStatus.second, statusType = contactsStatus.first)

                        // 5. RECORDING ACTIVE
                        val hasAudioPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        val recordingStatus = when {
                            isRecording -> StatusType.SUCCESS to "ACTIVE (AUDIO+VIDEO)"
                            !hasAudioPermission && !isDemoMode -> StatusType.WARNING to "MIC PERMISSION NEEDED"
                            else -> StatusType.PENDING to "INITIALIZING..."
                        }
                        StatusItem("RECORDING ACTIVE", statusText = recordingStatus.second, statusType = recordingStatus.first)

                        // 6. LIVE LOCATION ACTIVE
                        val liveLocStatus = when {
                            isTrackingLocation -> StatusType.SUCCESS to "ACTIVE"
                            else -> StatusType.PENDING to "STARTING..."
                        }
                        StatusItem("LIVE LOCATION ACTIVE", statusText = liveLocStatus.second, statusType = liveLocStatus.first)
                    }
                }

                // 1-Tap Emergency Location Share Fallback if carrier SMS was not dispatched
                val isSmsSuccess = dispatchResult != null && (dispatchResult!!.successfulDispatches > 0 || isDemoMode)
                if (!isSmsSuccess && dispatchResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val lat = currentLocation?.latitude ?: emergencyEvent?.latitude
                            val lng = currentLocation?.longitude ?: emergencyEvent?.longitude
                            val locLink = if (lat != null && lng != null) "https://maps.google.com/?q=$lat,$lng" else "Location Acquiring..."
                            val shareText = "🚨 SAATHI SOS ALERT!\nI need immediate help!\nLocation: $locLink\nEvent ID: $eventId"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Emergency Location"))
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SHARE LOCATION VIA SMS / WHATSAPP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom Authentication Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (authErrorMessage != null) {
                    Text(
                        text = authErrorMessage!!,
                        color = SecondaryAmber,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        authErrorMessage = null
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(100)
                            }
                        } catch (_: Exception) {}

                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            BiometricHelper.authenticateToExit(
                                activity = activity,
                                title = "Authenticate to Exit Emergency Lock",
                                subtitle = "Scan fingerprint or enter device PIN to unlock SAATHI controls",
                                onSuccess = {
                                    showAuthenticatedDialog = true
                                },
                                onError = { error ->
                                    authErrorMessage = error
                                    // Fallback for emulators/devices without lock screen setup
                                    if (error.contains("unavailable", ignoreCase = true) || error.contains("none enrolled", ignoreCase = true)) {
                                        showAuthenticatedDialog = true
                                    }
                                }
                            )
                        } else {
                            // Activity fallback
                            showAuthenticatedDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ErrorSOS)
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = ErrorSOS,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AUTHENTICATE TO EXIT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Screen locked for user privacy. Emergency workflow active in background.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

enum class StatusType {
    SUCCESS,
    PENDING,
    WARNING,
    DISABLED
}

@Composable
private fun StatusItem(
    label: String,
    statusText: String,
    statusType: StatusType = StatusType.SUCCESS
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (statusType) {
                StatusType.SUCCESS -> {
                    Text("✓", color = SafeGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(statusText, color = SafeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                StatusType.PENDING -> {
                    Text("⏳", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(statusText, color = SecondaryAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                StatusType.WARNING -> {
                    Text("⚠️", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(statusText, color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                StatusType.DISABLED -> {
                    Text("—", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(statusText, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
        }
    }
}
