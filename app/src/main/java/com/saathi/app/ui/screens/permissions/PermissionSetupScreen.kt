package com.saathi.app.ui.screens.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.saathi.app.Screen
import com.saathi.app.ui.theme.PrimaryTeal
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SecondaryAmber

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionSetupScreen(navController: NavController) {
    val context = LocalContext.current

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val smsPermissionState = rememberPermissionState(Manifest.permission.SEND_SMS)
    val phonePermissionState = rememberPermissionState(Manifest.permission.CALL_PHONE)
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val notificationsPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.PermissionSetup.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue to Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You can manage or change permissions anytime in Settings.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "App Permissions",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SAATHI requires essential permissions to capture your location and automatically dispatch emergency alerts to your safety circle.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Location
            item {
                PermissionCard(
                    icon = Icons.Filled.LocationOn,
                    title = "Location (High Accuracy)",
                    description = "Used to pinpoint and transmit your live GPS coordinates to trusted contacts when SOS is active.",
                    isGranted = locationPermissionState.status.isGranted,
                    shouldShowRationale = locationPermissionState.status.shouldShowRationale,
                    onRequestPermission = { locationPermissionState.launchPermissionRequest() },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                )
            }

            // SMS Dispatch
            item {
                PermissionCard(
                    icon = Icons.AutoMirrored.Filled.Send,
                    title = "Emergency SMS Dispatch",
                    description = "Enables automatic carrier SMS dispatch with your live location link directly to your Safety Circle contacts during an SOS trigger.",
                    isGranted = smsPermissionState.status.isGranted,
                    shouldShowRationale = smsPermissionState.status.shouldShowRationale,
                    onRequestPermission = { smsPermissionState.launchPermissionRequest() },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                )
            }

            // Notifications
            if (notificationsPermissionState != null) {
                item {
                    PermissionCard(
                        icon = Icons.Filled.Notifications,
                        title = "Notifications & Alerts",
                        description = "Enables ongoing lock-screen emergency tracking and travel check-in status notifications.",
                        isGranted = notificationsPermissionState.status.isGranted,
                        shouldShowRationale = notificationsPermissionState.status.shouldShowRationale,
                        onRequestPermission = { notificationsPermissionState.launchPermissionRequest() },
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Phone Calls
            item {
                PermissionCard(
                    icon = Icons.Filled.Phone,
                    title = "Direct Calling",
                    description = "Enables rapid direct dialing to your primary contact or official emergency services (112) with one tap.",
                    isGranted = phonePermissionState.status.isGranted,
                    shouldShowRationale = phonePermissionState.status.shouldShowRationale,
                    onRequestPermission = { phonePermissionState.launchPermissionRequest() },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                )
            }

            // Audio Evidence Recording (Covert background recording during SOS)
            item {
                PermissionCard(
                    icon = Icons.Filled.Mic,
                    title = "Automatic Audio Evidence",
                    description = "Allows SAATHI to automatically record audio in the background during an emergency SOS without drawing attention, stored securely in 'Saved Files' for legal protection.",
                    isGranted = audioPermissionState.status.isGranted,
                    shouldShowRationale = audioPermissionState.status.shouldShowRationale,
                    onRequestPermission = { audioPermissionState.launchPermissionRequest() },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                )
            }

            // Covert Video Evidence Capture (Camera)
            item {
                PermissionCard(
                    icon = Icons.Filled.Videocam,
                    title = "Covert Video Proof Capture",
                    description = "Allows SAATHI to capture emergency video evidence in the background during SOS so attackers or bystanders remain unaware, accessible anytime in 'Saved Files'.",
                    isGranted = cameraPermissionState.status.isGranted,
                    shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PrimaryTeal.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryTeal)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)

                Spacer(modifier = Modifier.height(8.dp))

                if (isGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Granted",
                            tint = SafeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Granted", color = SafeGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (shouldShowRationale) {
                    Text("Permission required for automatic dispatch", color = SecondaryAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Not Granted", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isGranted) {
                if (shouldShowRationale) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PrimaryTeal)
                    }
                } else {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant")
                    }
                }
            }
        }
    }
}
