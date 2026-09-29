package com.saathi.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.saathi.app.ui.theme.PrimaryTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val userName by preferencesManager.userName.collectAsState(initial = "")
    val isSilentSOS by preferencesManager.isSilentSOS.collectAsState(initial = false)
    val isFourTapSOS by preferencesManager.isFourTapSOS.collectAsState(initial = true)
    val isEmergencyRecording by preferencesManager.isEmergencyRecording.collectAsState(initial = false)
    val isEmergencyLockMode by preferencesManager.isEmergencyLockMode.collectAsState(initial = true)
    val isLiveLocationEnabled by preferencesManager.isLiveLocationEnabled.collectAsState(initial = true)
    val emergencyCallNumber by preferencesManager.emergencyCallNumber.collectAsState(initial = "112")
    var showCallNumberDialog by remember { mutableStateOf(false) }
    var tempCallNumber by remember { mutableStateOf("") }
    val isShakeSOS by preferencesManager.isShakeSOS.collectAsState(initial = true)
    val isVolumeDownSOS by preferencesManager.isVolumeDownSOS.collectAsState(initial = true)
    val volumeDownHoldSeconds by preferencesManager.volumeDownHoldSeconds.collectAsState(initial = 3)
    val theme by preferencesManager.theme.collectAsState(initial = "System")
    val language by preferencesManager.language.collectAsState(initial = "English")

    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { SettingsSectionTitle("User Profile") }
            item {
                SettingsItem(
                    title = "View Full Profile",
                    subtitle = "Name, phone, email, emergency settings",
                    onClick = { navController.navigate(Screen.Profile.route) }
                )
                SettingsItem(
                    title = "Your Name",
                    subtitle = userName.ifEmpty { "Tap to set name (e.g. Alok Sharma)" },
                    onClick = {
                        tempName = userName
                        showNameDialog = true
                    }
                )
                HorizontalDivider()
            }

            item { SettingsSectionTitle("Emergency Mode Settings") }
            item {
                SettingsSwitchItem(
                    title = "Emergency Lock Mode",
                    subtitle = "Protects user privacy by locking the screen into a minimal dark interface during an active SOS",
                    checked = isEmergencyLockMode,
                    onCheckedChange = { scope.launch { preferencesManager.setEmergencyLockMode(it) } }
                )
                SettingsSwitchItem(
                    title = "Live Location Sharing",
                    subtitle = "Continuously broadcasts GPS coordinates to your Safety Circle during an active emergency",
                    checked = isLiveLocationEnabled,
                    onCheckedChange = { scope.launch { preferencesManager.setLiveLocationEnabled(it) } }
                )
                SettingsSwitchItem(
                    title = "Emergency Recording",
                    subtitle = "Automatically records audio evidence during SOS events. Stored securely on this device.",
                    checked = isEmergencyRecording,
                    onCheckedChange = { scope.launch { preferencesManager.setEmergencyRecording(it) } }
                )
                SettingsSwitchItem(
                    title = "4-Tap SOS",
                    subtitle = "Rapidly tap screen 4+ times to activate emergency SOS when SAATHI is open",
                    checked = isFourTapSOS,
                    onCheckedChange = { scope.launch { preferencesManager.setFourTapSOS(it) } }
                )
                SettingsSwitchItem(
                    title = "Volume Trigger",
                    subtitle = "Hold the hardware volume-down button for $volumeDownHoldSeconds seconds to activate emergency SOS",
                    checked = isVolumeDownSOS,
                    onCheckedChange = { scope.launch { preferencesManager.setVolumeDownSOS(it) } }
                )
                if (isVolumeDownSOS) {
                    SettingsItem(
                        title = "Volume Down Hold Duration",
                        subtitle = "$volumeDownHoldSeconds seconds (Tap to cycle: 2s, 3s, 5s)",
                        onClick = {
                            val nextSec = when (volumeDownHoldSeconds) {
                                2 -> 3
                                3 -> 5
                                else -> 2
                            }
                            scope.launch { preferencesManager.setVolumeDownHoldSeconds(nextSec) }
                        }
                    )
                }
                SettingsItem(
                    title = "Emergency Call Number",
                    subtitle = emergencyCallNumber.ifEmpty { "112" },
                    onClick = { 
                        tempCallNumber = emergencyCallNumber
                        showCallNumberDialog = true 
                    }
                )
                SettingsSwitchItem(
                    title = "Silent SOS Mode",
                    subtitle = "Triggers SOS without siren, flash, or loud alarms",
                    checked = isSilentSOS,
                    onCheckedChange = { scope.launch { preferencesManager.setSilentSOS(it) } }
                )
                SettingsSwitchItem(
                    title = "Shake Phone for SOS",
                    subtitle = "Vigorously shake your device to instantly trigger emergency SOS",
                    checked = isShakeSOS,
                    onCheckedChange = { scope.launch { preferencesManager.setShakeSOS(it) } }
                )
                val isAccessibilityEnabled = remember(context) {
                    com.saathi.app.service.SaathiAccessibilityService.isServiceEnabled(context)
                }
                SettingsItem(
                    title = "Lock Screen Hardware SOS (Accessibility)",
                    subtitle = if (isAccessibilityEnabled) {
                        "ACTIVE: Volume button triggers SOS even when screen is locked or off (Tap to review)"
                    } else {
                        "DISABLED: Tap to enable in Android Accessibility Settings for lock-screen volume trigger"
                    },
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
                SettingsItem(
                    title = "Emergency Countdown",
                    subtitle = "3 seconds (Standard cancel window)",
                    onClick = { }
                )
                HorizontalDivider()
            }

            item { SettingsSectionTitle("SOS Trigger Capabilities") }
            item {
                val isAccessibilityEnabled = remember(context) {
                    com.saathi.app.service.SaathiAccessibilityService.isServiceEnabled(context)
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("4-TAP SOS: " + if (isFourTapSOS) "SUPPORTED (Foreground Active)" else "DISABLED", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("LOCK SCREEN TOUCH: RESTRICTED by Android OS security sandbox to prevent unauthorized touch interception.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("LOCK SCREEN HARDWARE TRIGGER: " + if (isAccessibilityEnabled) "ACTIVE via Accessibility Service" else "REQUIRES PERMISSION (Tap setting above to enable)", fontWeight = FontWeight.Bold, color = if (isAccessibilityEnabled) com.saathi.app.ui.theme.SafeGreen else com.saathi.app.ui.theme.SecondaryAmber)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("DISPLAY OVER LOCK SCREEN: FULLY SUPPORTED (Wakes screen and shows SAATHI without unlocking PIN).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }

            item { SettingsSectionTitle("Trusted Contacts") }
            item {
                SettingsItem(
                    title = "Manage Safety Circle",
                    subtitle = "Add, edit, or set primary contact",
                    onClick = { navController.navigate(Screen.TrustedContacts.route) }
                )
                SettingsItem(
                    title = "Safety Circle Visualization",
                    subtitle = "View network diagram of trusted relations",
                    onClick = { navController.navigate(Screen.SafetyCircle.route) }
                )
                HorizontalDivider()
            }

            item { SettingsSectionTitle("Appearance & Language") }
            item {
                SettingsItem(
                    title = "Theme",
                    subtitle = theme,
                    onClick = {
                        val nextTheme = when (theme) {
                            "Light" -> "Dark"
                            "Dark" -> "System"
                            else -> "Light"
                        }
                        scope.launch { preferencesManager.setTheme(nextTheme) }
                    }
                )
                SettingsItem(
                    title = "Language / भाषा",
                    subtitle = language,
                    onClick = {
                        val nextLang = if (language == "English") "हिन्दी" else "English"
                        scope.launch { preferencesManager.setLanguage(nextLang) }
                    }
                )
                HorizontalDivider()
            }

            item { SettingsSectionTitle("Accessibility") }
            item {
                SettingsItem(
                    title = "Accessibility Options",
                    subtitle = "Large text, high contrast, reduced motion",
                    onClick = { navController.navigate(Screen.AccessibilitySettings.route) }
                )
                HorizontalDivider()
            }

            item { SettingsSectionTitle("Privacy & Safety Policy") }
            item {
                SettingsItem(
                    title = "Data Collection & Privacy",
                    subtitle = "Location is only captured during SOS or travel checks",
                    onClick = { showPrivacyDialog = true }
                )
                SettingsItem(
                    title = "Live Location Test",
                    subtitle = "View GPS coordinates & Google Maps link",
                    onClick = { navController.navigate(Screen.LiveLocation.route) }
                )
                HorizontalDivider()
            }

            item { SettingsSectionTitle("Demo & Help") }
            item {
                SettingsItem(
                    title = "My Emergency Files",
                    subtitle = "View, play, share, or delete emergency recordings",
                    onClick = { navController.navigate(Screen.EmergencyFiles.route) }
                )
                SettingsItem(
                    title = "Demo Mode",
                    subtitle = "Test SOS and check-in without alerting real people",
                    onClick = { navController.navigate(Screen.DemoMode.route) }
                )
                SettingsItem(
                    title = "How SAATHI Works",
                    subtitle = "Guide and safety recommendations",
                    onClick = { navController.navigate(Screen.HelpScreen.route) }
                )
                HorizontalDivider()
            }

            item { SettingsSectionTitle("About SAATHI") }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SAATHI — Smart Assistance & Threat Handling Interface", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Designed for women, students, and lone travelers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Version 1.0.0 (Demo Prototype Build)", style = MaterialTheme.typography.labelSmall, color = PrimaryTeal)
                }
            }
        }

        if (showCallNumberDialog) {
            AlertDialog(
                onDismissRequest = { showCallNumberDialog = false },
                title = { Text("Update Emergency Call Number") },
                text = {
                    OutlinedTextField(
                        value = tempCallNumber,
                        onValueChange = { tempCallNumber = it },
                        label = { Text("Emergency Number") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            preferencesManager.setEmergencyCallNumber(tempCallNumber)
                            showCallNumberDialog = false
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCallNumberDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showNameDialog) {
            AlertDialog(
                onDismissRequest = { showNameDialog = false },
                title = { Text("Update Name") },
                text = {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Your Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            preferencesManager.setUserName(tempName)
                            showNameDialog = false
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy & Security Principles") },
                text = {
                    Column {
                        Text("• Location: Captured ONLY during SOS or when explicitly sharing in Safety Check-In.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• No Secret Tracking: SAATHI never runs secret continuous tracking without user awareness.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Local Storage: Contacts and incident records are securely stored on your local device.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Demo Separation: Demo simulations never ping real phone numbers.", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("Understood")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = PrimaryTeal,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsItem(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .defaultMinSize(minHeight = 48.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsSwitchItem(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
