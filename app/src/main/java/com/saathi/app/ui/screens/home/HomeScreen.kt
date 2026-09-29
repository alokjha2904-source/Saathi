package com.saathi.app.ui.screens.home

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.DateFormat
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.saathi.app.Screen
import com.saathi.app.ui.theme.ErrorSOS
import com.saathi.app.ui.theme.PrimaryTeal
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SecondaryAmber
import com.saathi.app.util.SaathiStrings
import java.util.Date
import androidx.compose.ui.input.pointer.PointerEventPass
import com.saathi.app.util.MultiTapDetector
import com.saathi.app.data.local.PreferencesManager

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val safetyStatus by viewModel.safetyStatus.collectAsState()
    val activeEventId by viewModel.activeEventId.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val battery by viewModel.batteryLevel.collectAsState()
    val network by viewModel.isNetworkConnected.collectAsState()
    val contacts by viewModel.trustedContactCount.collectAsState()
    val language by viewModel.language.collectAsState()
    val isHi = SaathiStrings.isHindi(language)

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val scrollState = rememberScrollState()

    val preferencesManager = remember { PreferencesManager(context) }
    val isFourTapSOS by preferencesManager.isFourTapSOS.collectAsState(initial = true)
    val multiTapDetector = remember {
        MultiTapDetector(requiredTaps = 4, windowMs = 2000L, cooldownMs = 3000L) {
            navController.navigate(Screen.SOSActivation.route)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(isFourTapSOS) {
                if (isFourTapSOS) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed && !it.previousPressed }) {
                                multiTapDetector.onTap()
                            }
                        }
                    }
                }
            }
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = SaathiStrings.appName(isHi),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = PrimaryTeal
                )
                Text(
                    text = SaathiStrings.appTagline(isHi),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDemoMode) {
                    AssistChip(
                        onClick = { navController.navigate(Screen.DemoMode.route) },
                        label = { Text("DEMO ACTIVE", fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SecondaryAmber.copy(alpha = 0.25f),
                            labelColor = Color(0xFF8A5300)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safety Status Card
        SafetyStatusCard(
            status = safetyStatus,
            isHi = isHi,
            onClick = {
                if (safetyStatus == SafetyStatus.EMERGENCY_ACTIVE && activeEventId != null) {
                    navController.navigate("emergency_active/$activeEventId")
                }
            }
        )

        if (contacts == 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.TrustedContacts.route) },
                shape = RoundedCornerShape(12.dp),
                color = SecondaryAmber.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF8A5300), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHi) "सुरक्षा चक्र खाली है: संपर्क जोड़ें" else "Safety Circle empty: Tap to add trusted contacts",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8A5300)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Info Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoChip(icon = Icons.Default.BatteryFull, text = "$battery%")
            InfoChip(
                icon = if (network) Icons.Default.Wifi else Icons.Default.WifiOff,
                text = if (network) SaathiStrings.online(isHi) else SaathiStrings.offline(isHi)
            )
            InfoChip(
                icon = Icons.Default.Group, 
                text = SaathiStrings.inCircle(isHi, contacts)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // SOS Button
        SOSButton(
            holdText = SaathiStrings.holdForSos(isHi),
            onSOS = {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(200)
                    }
                } catch (_: Exception) {}
                navController.navigate(Screen.SOSActivation.route)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Quick Actions
        QuickActionsGrid(navController, isHi)

        Spacer(modifier = Modifier.height(20.dp))

        // Bottom Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = { navController.navigate(Screen.AIAssistant.route) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(SaathiStrings.aiAssistant(isHi))
            }
            Button(
                onClick = { navController.navigate(Screen.DemoMode.route) },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryAmber),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(SaathiStrings.demoMode(isHi), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SafetyStatusCard(
    status: SafetyStatus,
    isHi: Boolean = false,
    onClick: () -> Unit = {}
) {
    val isSafe = status == SafetyStatus.SAFE
    val color = if (isSafe) SafeGreen else ErrorSOS
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "cardBorderPulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSafe, onClick = onClick)
            .border(
                width = 2.dp,
                color = if (!isSafe) color.copy(alpha = pulseAlpha) else color,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isSafe) Icons.Default.Security else Icons.Default.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSafe) SaathiStrings.youAreSafe(isHi) else SaathiStrings.emergencyActive(isHi),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            val time = DateFormat.getTimeFormat(LocalContext.current).format(Date())
            Text(text = "Updated: $time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (isSafe) SaathiStrings.safetyNetworkReady(isHi) else "Tap to return to active emergency screen",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSafe) MaterialTheme.colorScheme.onSurfaceVariant else ErrorSOS,
                fontWeight = if (isSafe) FontWeight.Normal else FontWeight.Bold
            )
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SOSButton(
    holdText: String = "HOLD FOR SOS",
    onSOS: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sosPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ), label = "buttonScale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(scale)
                .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = ErrorSOS, spotColor = ErrorSOS)
                .clip(CircleShape)
                .background(ErrorSOS)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onSOS() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SOS",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = holdText,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun QuickActionsGrid(navController: NavController, isHi: Boolean = false) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                title = SaathiStrings.safetyCheckInTitle(isHi),
                subtitle = SaathiStrings.safetyCheckInSub(isHi),
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.SafetyCheckIn.route) }
            )
            ActionCard(
                icon = Icons.Default.Group,
                title = SaathiStrings.safetyCircleTitle(isHi),
                subtitle = SaathiStrings.safetyCircleSub(isHi),
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.TrustedContacts.route) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                icon = Icons.Default.LocalHospital,
                title = SaathiStrings.safePlacesTitle(isHi),
                subtitle = SaathiStrings.safePlacesSub(isHi),
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.SafePlaces.route) }
            )
            ActionCard(
                icon = Icons.Default.History,
                title = SaathiStrings.incidentHistoryTitle(isHi),
                subtitle = SaathiStrings.incidentHistorySub(isHi),
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.IncidentHistory.route) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                icon = Icons.Default.FolderOpen,
                title = if (isHi) "सुरक्षित फाइलें (Saved Files)" else "Saved Files",
                subtitle = if (isHi) "ऑडियो व वीडियो साक्ष्य" else "Audio & Video Evidence",
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.EmergencyFiles.route) }
            )
            ActionCard(
                icon = Icons.Default.AccountCircle,
                title = if (isHi) "मेरी प्रोफाइल" else "User Profile",
                subtitle = if (isHi) "आईडी और विवरण" else "Identity & Details",
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.Profile.route) }
            )
        }
    }
}

@Composable
fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
