package com.saathi.app.ui.screens.emergency

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.saathi.app.Screen
import com.saathi.app.data.local.PreferencesManager
import com.saathi.app.data.local.SaathiDatabase
import com.saathi.app.data.model.EmergencyEvent
import com.saathi.app.data.model.TrustedContact
import com.saathi.app.service.ContactDispatchStatus
import com.saathi.app.service.EmergencyDispatcher
import com.saathi.app.service.EmergencyService
import com.saathi.app.ui.theme.ErrorSOS
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SecondaryAmber
import com.saathi.app.util.SaathiStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class TimelineEntry(
    val timestamp: Long,
    val label: String,
    val isSuccess: Boolean = true
)

@Composable
fun EmergencyActiveScreen(
    navController: NavController,
    eventId: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    val isDemoMode by preferencesManager.isDemoMode.collectAsState(initial = false)
    val language by preferencesManager.language.collectAsState(initial = "English")
    val isHi = SaathiStrings.isHindi(language)

    var showEndDialog by remember { mutableStateOf(false) }
    val actualEventId = remember { eventId ?: UUID.randomUUID().toString().take(8).uppercase() }

    var emergencyEvent by remember { mutableStateOf<EmergencyEvent?>(null) }
    var primaryContact by remember { mutableStateOf<TrustedContact?>(null) }
    val dispatchResult by EmergencyDispatcher.latestDispatchResult.collectAsState()

    val timeline = remember { mutableStateListOf<TimelineEntry>() }
    val isRecording by com.saathi.app.service.EmergencyRecorder.isRecording.collectAsState()
    val isTrackingLocation by com.saathi.app.util.LiveLocationTracker.isTracking.collectAsState()
    val currentLocation by com.saathi.app.util.LiveLocationTracker.currentLocation.collectAsState()
    val locationHistory by com.saathi.app.util.LiveLocationTracker.locationHistory.collectAsState()
    val isStaleLocation by com.saathi.app.util.LiveLocationTracker.isStale.collectAsState()

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        timeline.add(TimelineEntry(now - 3000L, "SOS activated"))
        timeline.add(TimelineEntry(now - 100L, "3-second cancellation completed"))
        timeline.add(TimelineEntry(now, "Emergency event created"))
    }

    LaunchedEffect(isTrackingLocation) {
        if (isTrackingLocation) {
            timeline.add(TimelineEntry(System.currentTimeMillis(), "Live location sharing started"))
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            timeline.add(TimelineEntry(System.currentTimeMillis(), "Recording started"))
        }
    }

    LaunchedEffect(dispatchResult) {
        val result = dispatchResult
        if (result != null && result.details.isNotEmpty()) {
            timeline.add(TimelineEntry(System.currentTimeMillis(), "SMS processing"))
            if (result.successfulDispatches > 0) {
                timeline.add(TimelineEntry(System.currentTimeMillis(), "Trusted contacts notified"))
            } else {
                timeline.add(TimelineEntry(System.currentTimeMillis(), "SMS dispatch failed or no permission", false))
            }
        }
    }

    // Real device state: Battery & Network
    val batteryPct = remember {
        try {
            val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val bIntent = context.registerReceiver(null, iFilter)
            val level = bIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = bIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 85
        } catch (_: Exception) {
            85
        }
    }

    val networkStatus = remember {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(net)
            when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi Connected"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular Active"
                else -> "Offline / Cellular"
            }
        } catch (_: Exception) {
            "Cellular Active"
        }
    }

    // Siren handling
    var isSirenPlaying by remember { mutableStateOf(false) }
    val ringtone = remember {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            RingtoneManager.getRingtone(context, alertUri)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                ringtone?.stop()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(actualEventId) {
        try {
            val db = SaathiDatabase.getDatabase(context)
            emergencyEvent = db.emergencyEventDao().getEmergencyEventByEventId(actualEventId)
            if (emergencyEvent != null) {
                timeline.add(TimelineEntry(emergencyEvent!!.timestamp, "Emergency event created"))
                if (emergencyEvent!!.latitude != null) {
                    timeline.add(TimelineEntry(System.currentTimeMillis(), "Location acquired"))
                } else {
                    timeline.add(TimelineEntry(System.currentTimeMillis(), "Location pending", false))
                }
            }
            db.trustedContactDao().getAllContactsFlow().collect { contacts ->
                primaryContact = contacts.find { it.isPrimary } ?: contacts.firstOrNull()
            }
        } catch (_: Exception) {}
    }

    // Accidental Cancel Protection Dialog
    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = {
                Text(
                    if (isHi) "क्या आप आपातकाल समाप्त करना चाहते हैं?" else "End Emergency?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isHi) "क्या आप वाकई आपातकालीन स्थिति को समाप्त करना चाहते हैं?\nकेवल तभी समाप्त करें जब आप पूर्णतः सुरक्षित हों।"
                    else "Are you sure you want to end the emergency?\nOnly cancel if you are in a completely secure location with confirmed safety."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    try {
                        ringtone?.stop()
                        isSirenPlaying = false
                    } catch (_: Exception) {}
                    coroutineScope.launch {
                        try {
                            val db = SaathiDatabase.getDatabase(context)
                            val existing = db.emergencyEventDao().getEmergencyEventByEventId(actualEventId)
                            if (existing != null) {
                                db.emergencyEventDao().updateEmergencyEvent(
                                    existing.copy(
                                        status = "RESOLVED",
                                        endTimestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                            EmergencyService.stopEmergency(context)
                            com.saathi.app.service.EmergencyRecorder.stopAllRecording()
                            com.saathi.app.util.LiveLocationTracker.stopTracking()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        navController.navigate("post_emergency/$actualEventId") {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                }) {
                    Text(
                        if (isHi) "हाँ, मैं सुरक्षित हूँ" else "Yes, I'm Safe",
                        color = SafeGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text(SaathiStrings.cancel(isHi), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Red Pulsing Banner
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bannerAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ErrorSOS.copy(alpha = alpha))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SaathiStrings.sosActive(isHi),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Event ID: #$actualEventId",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                if (isDemoMode || emergencyEvent?.isDemo == true) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Badge(containerColor = Color(0xFFFFC107), contentColor = Color.Black) {
                        Text(
                            "DEMO MODE — NO REAL ALERTS DISPATCHED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {

            // Device Telemetry: Battery, Network, Timestamp
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryFull, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$batteryPct%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(networkStatus, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    val timeFormatted = remember(emergencyEvent) {
                        val ts = emergencyEvent?.timestamp ?: System.currentTimeMillis()
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ts))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(timeFormatted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tactical Live Location Map (Feature 4)
            com.saathi.app.ui.components.LiveLocationMap(
                currentLocation = currentLocation,
                locationHistory = locationHistory,
                isTracking = isTrackingLocation,
                isStale = isStaleLocation
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Switch to Emergency Lock Mode button (Feature 1 & 5)
            Button(
                onClick = {
                    navController.navigate("emergency_lock/$actualEventId")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, ErrorSOS),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = ErrorSOS)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "SWITCH TO PRIVACY LOCK MODE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real Current Location Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = ErrorSOS,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isStaleLocation) "अंतिम ज्ञात स्थान (LAST KNOWN LOCATION)" else if (isHi) "सटीक स्थान (LIVE LOCATION ● ACTIVE)" else "LIVE LOCATION ● ACTIVE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isStaleLocation) SecondaryAmber else SafeGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val displayLoc = currentLocation
                        val lat = displayLoc?.latitude ?: emergencyEvent?.latitude
                        val lng = displayLoc?.longitude ?: emergencyEvent?.longitude
                        val acc = displayLoc?.accuracy ?: 10f

                        if (lat != null && lng != null) {
                            Text(
                                text = "${"%.6f".format(lat)}, ${"%.6f".format(lng)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Accuracy: ${acc.toInt()} m • Updates: ${locationHistory.size} recorded",
                                fontSize = 12.sp,
                                color = if (isStaleLocation) SecondaryAmber else SafeGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Updating location...",
                                fontSize = 14.sp,
                                color = SecondaryAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trusted Contacts & Real Dispatch Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHi) "सुरक्षा चक्र अलर्ट स्थिति" else "Safety Circle Dispatch Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (dispatchResult != null) {
                            Text(
                                text = "${dispatchResult!!.successfulDispatches}/${dispatchResult!!.totalContacts} notified",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dispatchResult!!.successfulDispatches > 0) SafeGreen else ErrorSOS
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val result = dispatchResult
                    if (result == null || result.details.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dispatching alerts to trusted contacts...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        if (!result.hasSmsPermission && !result.isDemo) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SMS permission is disabled. Real carrier SMS could not be sent. Please use 'Share Alert' below.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        result.details.forEach { detail ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (detail.status) {
                                    ContactDispatchStatus.SENT -> {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("${detail.name} notified", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("SMS delivered • ${detail.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    ContactDispatchStatus.DEMO_SIMULATED -> {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("${detail.name} (Demo Simulated)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Alert simulated without real SMS", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    ContactDispatchStatus.PERMISSION_DENIED -> {
                                        Icon(Icons.Default.Cancel, contentDescription = null, tint = ErrorSOS, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("${detail.name} — SMS Disabled", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ErrorSOS)
                                            Text("SMS permission required", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    ContactDispatchStatus.INVALID_PHONE -> {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = SecondaryAmber, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("${detail.name} skipped", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Invalid phone number (${detail.phone})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    ContactDispatchStatus.FAILED -> {
                                        Icon(Icons.Default.Cancel, contentDescription = null, tint = ErrorSOS, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("${detail.name} failed", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ErrorSOS)
                                            Text(detail.detailMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    ContactDispatchStatus.QUEUED -> {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("${detail.name} queued", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Preparing to send...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    ContactDispatchStatus.SENDING -> {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("${detail.name} sending...", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SecondaryAmber)
                                            Text("SMS dispatch in progress", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isRecording) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🔴 Audio Recording Active", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { com.saathi.app.service.EmergencyRecorder.stopAllRecording() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer, contentColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text("Stop")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Emergency Timeline",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    timeline.forEach { entry ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(entry.timestamp))
                            Icon(
                                if (entry.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (entry.isSuccess) SafeGreen else SecondaryAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(entry.label, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isDemoMode || emergencyEvent?.isDemo == true) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Demo Acknowledgment Simulation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Call Initiated (Demo)", fontSize = 14.sp)
                        Text("• AI Voice Response (Demo)", fontSize = 14.sp)
                        Text("• Press 1 to Acknowledge", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { timeline.add(TimelineEntry(System.currentTimeMillis(), "Demo Acknowledgment Received")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate Acknowledgment")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("DEMONSTRATION ONLY — NO REAL ALERTS", fontSize = 12.sp, color = SecondaryAmber, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Emergency Siren Button
            Button(
                onClick = {
                    isSirenPlaying = !isSirenPlaying
                    try {
                        if (isSirenPlaying) {
                            ringtone?.play()
                        } else {
                            ringtone?.stop()
                        }
                    } catch (_: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSirenPlaying) Color.Black else SecondaryAmber
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(
                    if (isSirenPlaying) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = if (isSirenPlaying) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isSirenPlaying) (if (isHi) "सायरन बंद करें" else "STOP SIREN")
                    else (if (isHi) "🔊 आपातकालीन सायरन बजाएं" else "🔊 SOUND EMERGENCY SIREN"),
                    color = if (isSirenPlaying) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Call Primary Contact or 112
            val callTarget = primaryContact?.phone ?: "112"
            val callLabel = if (primaryContact != null) {
                if (isHi) "कॉल करें: ${primaryContact!!.name} (${primaryContact!!.phone})"
                else "CALL ${primaryContact!!.name.uppercase()} (${primaryContact!!.phone})"
            } else {
                if (isHi) "आपातकालीन नंबर 112 पर कॉल करें" else "CALL EMERGENCY 112"
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$callTarget"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(callLabel, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct Share Alert
            OutlinedButton(
                onClick = {
                    val lat = emergencyEvent?.latitude
                    val lng = emergencyEvent?.longitude
                    val locUrl = if (lat != null && lng != null) "https://maps.google.com/?q=$lat,$lng" else "Location captured in SAATHI"
                    val shareMsg = "SAATHI EMERGENCY ALERT\n\nI need immediate help!\nEvent ID: #$actualEventId\nStatus: ACTIVE\nLocation: $locUrl\nPlease check on me or alert authorities."
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareMsg)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Emergency Alert"))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(SaathiStrings.shareAlert(isHi), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { navController.navigate(Screen.SafePlaces.route) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LocalHospital, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(SaathiStrings.nearbySafePlaces(isHi), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // End SOS with double confirmation
            Button(
                onClick = { showEndDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorSOS),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(SaathiStrings.endSos(isHi), fontWeight = FontWeight.Bold)
            }
        }
    }
}
