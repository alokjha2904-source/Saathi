package com.saathi.app.ui.screens.checkin

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.saathi.app.Screen
import com.saathi.app.data.local.PreferencesManager
import com.saathi.app.data.local.SaathiDatabase
import com.saathi.app.data.model.EmergencyEvent
import com.saathi.app.ui.theme.ErrorSOS
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SecondaryAmber
import com.saathi.app.util.SaathiStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class CheckInState {
    SETUP, ACTIVE, MISSED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCheckInScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    val language by preferencesManager.language.collectAsState(initial = "English")
    val isDemoMode by preferencesManager.isDemoMode.collectAsState(initial = false)
    val isHi = SaathiStrings.isHindi(language)

    var currentState by remember { mutableStateOf(CheckInState.SETUP) }
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var timeRemainingSeconds by remember { mutableIntStateOf(0) }
    var totalTimeSeconds by remember { mutableIntStateOf(0) }
    var isWalkWithMe by remember { mutableStateOf(false) }
    var missedEventId by remember { mutableStateOf<String?>(null) }

    // Timer effect
    LaunchedEffect(currentState, timeRemainingSeconds) {
        if (currentState == CheckInState.ACTIVE) {
            if (timeRemainingSeconds > 0) {
                delay(1000L)
                timeRemainingSeconds -= 1
            } else {
                currentState = CheckInState.MISSED
                // Automatically log missed check-in event to database
                val newEventId = UUID.randomUUID().toString().take(8).uppercase()
                missedEventId = newEventId
                coroutineScope.launch {
                    try {
                        val db = SaathiDatabase.getDatabase(context)
                        val start = origin.ifEmpty { "Current Location" }
                        val end = destination.ifEmpty { "Destination" }
                        val event = EmergencyEvent(
                            eventId = newEventId,
                            type = "CHECK_IN_MISSED",
                            status = "ACTIVE",
                            locationName = "$start → $end",
                            description = "Overdue travel check-in on route from $start to $end",
                            timestamp = System.currentTimeMillis(),
                            isDemo = isDemoMode
                        )
                        db.emergencyEventDao().insertEmergencyEvent(event)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isWalkWithMe) (if (isHi) "मेरे साथ चलें (Walk With Me)" else "Walk With Me") else SaathiStrings.safetyCheckInTitle(isHi), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Walk With Me", style = MaterialTheme.typography.labelSmall)
                        Switch(
                            checked = isWalkWithMe,
                            onCheckedChange = { isWalkWithMe = it },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (currentState) {
                CheckInState.SETUP -> {
                    Text(
                        if (isHi) "सुरक्षा चक्र को बताएं कि आप यात्रा कर रहे हैं" else "Let your Safety Circle know you're traveling",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        label = { Text(if (isHi) "आप अभी कहाँ हैं?" else "Where are you now?") },
                        placeholder = { Text(if (isHi) "उदा. कॉलेज लाइब्रेरी" else "e.g. College Library") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text(if (isHi) "आप कहाँ जा रहे हैं?" else "Where are you going?") },
                        placeholder = { Text(if (isHi) "उदा. हॉस्टल कैंपस 15" else "e.g. Hostel Campus 15") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(if (isHi) "अपेक्षित यात्रा समय" else "Expected travel time", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    val times = listOf(10, 15, 20, 30, 45, 60)
                    var selectedMinutes by remember { mutableIntStateOf(20) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        times.take(4).forEach { minutes ->
                            FilterChip(
                                selected = selectedMinutes == minutes,
                                onClick = { selectedMinutes = minutes },
                                label = { Text("${minutes}m") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (isHi) "$selectedMinutes मिनट में सुरक्षित आगमन अपेक्षित" else "Arrive safely in $selectedMinutes minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            totalTimeSeconds = selectedMinutes * 60
                            timeRemainingSeconds = totalTimeSeconds
                            currentState = CheckInState.ACTIVE
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isHi) "सुरक्षा चेक-इन शुरू करें" else "Start Safety Check-In")
                    }
                }

                CheckInState.ACTIVE -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).background(SafeGreen, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isHi) "चेक-इन सक्रिय" else "CHECK-IN ACTIVE", fontWeight = FontWeight.Bold, color = SafeGreen)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            val displayOrigin = if (origin.isEmpty()) (if (isHi) "प्रारंभिक बिंदु" else "Start Point") else origin
                            val displayDest = if (destination.isEmpty()) (if (isHi) "गंतव्य" else "Destination") else destination
                            Text("$displayOrigin → $displayDest", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(28.dp))

                            val mins = timeRemainingSeconds / 60
                            val secs = timeRemainingSeconds % 60
                            Text(
                                String.format("%02d:%02d", mins, secs),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(if (isHi) "चेक-इन के लिए शेष समय" else "remaining to check in", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(20.dp))
                            val progress = if (totalTimeSeconds > 0) 1f - (timeRemainingSeconds.toFloat() / totalTimeSeconds) else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = SafeGreen
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            Button(
                                onClick = {
                                    currentState = CheckInState.SETUP
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(SaathiStrings.arrivedSafely(isHi), fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val desc = Uri.encode("Manual SOS while tracking route: $origin to $destination")
                                    navController.navigate("sos_activation?description=$desc&category=SOS")
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorSOS)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("EMERGENCY SOS", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(onClick = { timeRemainingSeconds += 10 * 60; totalTimeSeconds += 10 * 60 }) {
                                Text(if (isHi) "समय बढ़ाएं (+10 मिनट)" else "Extend Time (+10 min)")
                            }
                        }
                    }
                }

                CheckInState.MISSED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SecondaryAmber.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = SecondaryAmber, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(SaathiStrings.checkInMissed(isHi), fontWeight = FontWeight.Bold, color = Color(0xFF8A5300), style = MaterialTheme.typography.titleLarge)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(if (isHi) "आपने गंतव्य पर पहुँचने की पुष्टि नहीं की है।" else "You haven't confirmed your arrival at your destination.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodyMedium)

                            Spacer(modifier = Modifier.height(24.dp))
                            Text(SaathiStrings.areYouSafe(isHi), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    // Resolve the logged event
                                    missedEventId?.let { id ->
                                        coroutineScope.launch {
                                            try {
                                                val db = SaathiDatabase.getDatabase(context)
                                                val existing = db.emergencyEventDao().getEmergencyEventByEventId(id)
                                                if (existing != null) {
                                                    db.emergencyEventDao().updateEmergencyEvent(existing.copy(status = "RESOLVED", endTimestamp = System.currentTimeMillis()))
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                            ) {
                                Text(SaathiStrings.yesSafe(isHi), fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val desc = Uri.encode("Missed Check-in on route $origin to $destination")
                                    navController.navigate("sos_activation?description=$desc&category=CHECK_IN_MISSED")
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorSOS)
                            ) {
                                Text(SaathiStrings.triggerSos(isHi), fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    timeRemainingSeconds = 10 * 60
                                    totalTimeSeconds = 10 * 60
                                    currentState = CheckInState.ACTIVE
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text(if (isHi) "टाइमर बढ़ाएं (+10 मिनट)" else "EXTEND TIMER (+10 MIN)")
                            }
                        }
                    }
                }
            }
        }
    }
}
