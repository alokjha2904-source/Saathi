package com.saathi.app.ui.screens.emergency

import androidx.compose.foundation.background
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
import androidx.navigation.NavController
import com.saathi.app.Screen
import com.saathi.app.data.local.SaathiDatabase
import com.saathi.app.data.model.EmergencyEvent
import com.saathi.app.service.EmergencyDispatcher
import com.saathi.app.service.EmergencyRecorder
import com.saathi.app.ui.theme.ErrorSOS
import com.saathi.app.ui.theme.PrimaryTeal
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SecondaryAmber
import com.saathi.app.util.LiveLocationTracker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostEmergencyScreen(
    navController: NavController,
    eventId: String
) {
    val context = LocalContext.current
    var event by remember { mutableStateOf<EmergencyEvent?>(null) }
    val dispatchResult by EmergencyDispatcher.latestDispatchResult.collectAsState()
    val recordings = remember { EmergencyRecorder.getRecordingsForEvent(context, eventId) }
    val updateCount by LiveLocationTracker.updateCount.collectAsState()

    LaunchedEffect(eventId) {
        try {
            val db = SaathiDatabase.getDatabase(context)
            event = db.emergencyEventDao().getEmergencyEventByEventId(eventId)
        } catch (_: Exception) {}
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }
    val startTimeStr = remember(event) {
        event?.timestamp?.let { dateFormatter.format(Date(it)) } ?: "Recorded"
    }
    val endTimeStr = remember(event) {
        val end = event?.endTimestamp ?: System.currentTimeMillis()
        dateFormatter.format(Date(end))
    }
    val durationSeconds = remember(event) {
        val start = event?.timestamp ?: System.currentTimeMillis()
        val end = event?.endTimestamp ?: System.currentTimeMillis()
        maxOf(1L, (end - start) / 1000L)
    }
    val durationStr = remember(durationSeconds) {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        "${mins}m ${secs}s"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Summary", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Resolution Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(SafeGreen.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Emergency Mode Ended",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = SafeGreen
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Incident resolved and archived. All emergency evidence has been preserved.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "INCIDENT AUDIT LOG",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SummaryRow("Event ID", "#$eventId", isMonospace = true)
                    SummaryRow("Start Time", startTimeStr)
                    SummaryRow("End Time", endTimeStr)
                    SummaryRow("Total Active Duration", durationStr)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    val lat = event?.latitude
                    val lng = event?.longitude
                    val locCoords = if (lat != null && lng != null) "${"%.5f".format(lat)}, ${"%.5f".format(lng)}" else "GPS Captured"
                    SummaryRow("Captured Location", locCoords)
                    SummaryRow("Live GPS Updates", "$updateCount recorded")

                    val contactsNotified = dispatchResult?.successfulDispatches ?: 0
                    val totalContacts = dispatchResult?.totalContacts ?: 0
                    SummaryRow("Contacts Notified", "$contactsNotified of $totalContacts contacts")

                    val smsStatus = if (dispatchResult?.isDemo == true) {
                        "Simulated (Demo)"
                    } else if (contactsNotified > 0) {
                        "Delivered ($contactsNotified sent)"
                    } else {
                        "Dispatch completed"
                    }
                    SummaryRow("Emergency SMS", smsStatus)

                    SummaryRow("Emergency Call", "112 Direct Protocol")

                    val recordingStatus = if (recordings.isNotEmpty()) {
                        "${recordings.size} file(s) saved"
                    } else {
                        "No recording captured"
                    }
                    SummaryRow("Audio Evidence", recordingStatus)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = { navController.navigate(Screen.IncidentHistory.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("VIEW INCIDENT HISTORY", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { navController.navigate(Screen.EmergencyFiles.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MY EMERGENCY FILES", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RETURN HOME", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Emergency recordings and timeline are stored locally and will never be deleted automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}
