package com.saathi.app.ui.screens.sos

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import android.content.Intent
import android.net.Uri
import com.saathi.app.service.EmergencyDispatcher
import com.saathi.app.service.EmergencyRecorder
import com.saathi.app.service.EmergencyService
import com.saathi.app.ui.theme.ErrorSOS
import kotlinx.coroutines.flow.first
import com.saathi.app.util.LocationHelper
import com.saathi.app.util.LocationResult
import com.saathi.app.util.SaathiStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

@Composable
fun SOSActivationScreen(
    navController: NavController,
    initialDescription: String = "",
    initialCategory: String = "SOS",
    isDemoMode: Boolean? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var countdown by remember { mutableIntStateOf(3) }
    var isCancelled by remember { mutableStateOf(false) }

    val preferencesManager = remember { PreferencesManager(context) }
    val isDemoModePref by preferencesManager.isDemoMode.collectAsState(initial = false)
    val effectiveDemoMode = isDemoMode ?: isDemoModePref
    val language by preferencesManager.language.collectAsState(initial = "English")
    val isHi = SaathiStrings.isHindi(language)

    var capturedLocationResult by remember { mutableStateOf<LocationResult?>(null) }
    var capturedAddress by remember { mutableStateOf<String?>(null) }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Attempt to acquire fresh high-accuracy location as early as countdown starts
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val locRes = LocationHelper.getFreshLocation(context)
                withContext(Dispatchers.Main) {
                    capturedLocationResult = locRes
                }
                if (locRes != null) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(locRes.latitude, locRes.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val addrText = (0..addr.maxAddressLineIndex).joinToString(", ") { addr.getAddressLine(it) }
                            withContext(Dispatchers.Main) {
                                capturedAddress = addrText
                            }
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(key1 = true) {
        while (countdown > 0) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            } catch (_: Exception) {}
            delay(1000)
            countdown--
        }

        if (countdown == 0 && !isCancelled) {
            val eventId = UUID.randomUUID().toString().take(8).uppercase()
            coroutineScope.launch {
                try {
                    val db = SaathiDatabase.getDatabase(context)

                    // In REAL MODE: Use actual location coordinates only (never fake/demo coordinates!)
                    // In DEMO MODE: Mark explicitly as demo simulation
                    val finalLocation = capturedLocationResult
                        ?: LocationHelper.getBestAvailableLocation(context)
                        ?: com.saathi.app.util.LiveLocationTracker.currentLocation.value

                    val lat = if (effectiveDemoMode) {
                        20.3541
                    } else {
                        finalLocation?.latitude
                    }

                    val lng = if (effectiveDemoMode) {
                        85.8145
                    } else {
                        finalLocation?.longitude
                    }

                    val locAccuracy = if (effectiveDemoMode) 10f else finalLocation?.accuracy

                    val locName = if (effectiveDemoMode) {
                        "DEMO SIMULATION: KIIT Campus, Bhubaneswar"
                    } else if (capturedAddress != null) {
                        capturedAddress
                    } else if (lat != null && lng != null) {
                        "GPS Location Captured (±${locAccuracy?.toInt() ?: 0}m)"
                    } else {
                        "GPS Signal Pending Acquisition"
                    }

                    val desc = initialDescription.ifEmpty {
                        if (effectiveDemoMode) "DEMO MODE — Emergency simulation drill" else "Instant SOS triggered by user"
                    }

                    val event = EmergencyEvent(
                        eventId = eventId,
                        type = initialCategory,
                        status = "ACTIVE",
                        latitude = lat,
                        longitude = lng,
                        locationName = locName,
                        description = desc,
                        timestamp = System.currentTimeMillis(),
                        isDemo = effectiveDemoMode
                    )
                    db.emergencyEventDao().insertEmergencyEvent(event)

                    val isLockModeEnabled = preferencesManager.isEmergencyLockMode.first()
                    val targetRoute = if (isLockModeEnabled) "emergency_lock/$eventId" else "emergency_active/$eventId"

                    val appContext = context.applicationContext

                    // Launch critical emergency background processes in an Application IO Scope
                    // so they will NEVER be cancelled by UI navigation destroying the screen
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            com.saathi.app.util.LiveLocationTracker.startTracking(
                                context = appContext,
                                eventId = eventId,
                                isDemo = effectiveDemoMode
                            )
                        } catch (_: Exception) {}

                        try {
                            EmergencyService.startEmergency(appContext, eventId)
                        } catch (_: Exception) {}

                        try {
                            EmergencyDispatcher.dispatchSOSAlert(
                                context = appContext,
                                event = event,
                                accuracyMeters = locAccuracy,
                                isDemoMode = effectiveDemoMode
                            )
                        } catch (_: Exception) {}

                        try {
                            val isRecordingEnabled = preferencesManager.isEmergencyRecording.first()
                            if (isRecordingEnabled) {
                                EmergencyRecorder.startAutomaticEmergencyRecording(appContext, eventId, isDemo = effectiveDemoMode)
                            }
                        } catch (_: Exception) {}
                    }

                    // Auto-initiate emergency call if not demo mode
                    if (!effectiveDemoMode) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val callNumber = preferencesManager.emergencyCallNumber.first()
                                val contacts = db.trustedContactDao().getAllTrustedContacts().first()
                                val primaryPhone = contacts.find { it.isPrimary }?.phone ?: callNumber
                                withContext(Dispatchers.Main) {
                                    try {
                                        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$primaryPhone")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        appContext.startActivity(callIntent)
                                    } catch (_: Exception) {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$primaryPhone")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        appContext.startActivity(dialIntent)
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    navController.navigate("emergency_active/$eventId") {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF8B0000), // Dark Red
                        ErrorSOS
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (effectiveDemoMode) {
                    Badge(
                        containerColor = Color(0xFFFFC107),
                        contentColor = Color.Black
                    ) {
                        Text(
                            "DEMO MODE — NO REAL ALERTS WILL BE SENT",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = SaathiStrings.activatingSos(isHi),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isHi) "सुरक्षा चक्र को अलर्ट भेजने की तैयारी..." else "Preparing to broadcast emergency alert...",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // Pulsing Countdown Circle
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale)
                    .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color.White, shape = CircleShape)
                ) {
                    Text(
                        text = "$countdown",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = ErrorSOS
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = {
                        isCancelled = true
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = SaathiStrings.cancel(isHi),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorSOS
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isHi) "रद्द करने के लिए यहाँ दबाएं" else "Tap CANCEL if activated by accident",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
