package com.saathi.app.ui.screens.demo

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.saathi.app.Screen
import com.saathi.app.data.local.PreferencesManager
import com.saathi.app.data.local.SaathiDatabase
import com.saathi.app.data.model.TrustedContact
import kotlinx.coroutines.launch

class DemoModeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = SaathiDatabase.getDatabase(application)
    private val contactDao = db.trustedContactDao()
    private val prefs = PreferencesManager(application)

    fun setupDemoContacts() {
        viewModelScope.launch {
            prefs.setDemoMode(true)
            val demoContacts = listOf(
                TrustedContact(name = "Aadhya Sharma", relationship = "Parent", phone = "+91 9876543210", isPrimary = true, isDemo = true),
                TrustedContact(name = "Raj Patel", relationship = "Friend", phone = "+91 9876543211", isDemo = true),
                TrustedContact(name = "Dr. Mehra", relationship = "Guardian", phone = "+91 9876543212", isDemo = true)
            )
            demoContacts.forEach {
                contactDao.insertTrustedContact(it)
            }
        }
    }

    fun clearDemoContacts() {
        viewModelScope.launch {
            prefs.setDemoMode(false)
            contactDao.deleteDemoContacts()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoModeScreen(
    navController: NavController,
    viewModel: DemoModeViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val secondaryAmber = Color(0xFFFFC107)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Demo Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = secondaryAmber.copy(alpha = 0.2f)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                colors = CardDefaults.cardColors(containerColor = secondaryAmber.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = secondaryAmber,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DEMO MODE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Experience the complete SAATHI workflow without sending real alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Badge(
                        containerColor = secondaryAmber,
                        contentColor = Color.Black
                    ) {
                        Text(
                            "DEMONSTRATION ONLY — No real contacts will be notified",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Text("Demo Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Demo Controls
            DemoControlCard(
                title = "Simulate Emergency Response",
                description = "Experience the complete SOS flow: activation → location → notification → response",
                icon = Icons.Default.NotificationsActive,
                onClick = {
                    viewModel.setupDemoContacts()
                    navController.navigate(Screen.SOSActivation.route)
                }
            )

            DemoControlCard(
                title = "Simulate Safety Check-In",
                description = "See how travel check-in monitoring works",
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                onClick = {
                    viewModel.setupDemoContacts()
                    navController.navigate(Screen.SafetyCheckIn.route)
                }
            )

            DemoControlCard(
                title = "Try AI Emergency Assistant",
                description = "See how AI helps analyze emergency situations",
                icon = Icons.Default.SmartToy,
                onClick = {
                    viewModel.setupDemoContacts()
                    navController.navigate(Screen.AIAssistant.route)
                }
            )

            DemoControlCard(
                title = "Simulate Emergency Lock Mode",
                description = "Test the dark privacy mode, live breadcrumb radar map, and authenticated biometric exit drill",
                icon = Icons.Default.Lock,
                onClick = {
                    viewModel.setupDemoContacts()
                    navController.navigate("emergency_lock/DEMO9999")
                }
            )

            // Simulated User Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Simulated User Profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Name: Demo User")
                    Text("Safety Circle: Mother, Friend, Guardian")
                    Text("Location: KIIT University Campus, Bhubaneswar")
                }
            }

            // Footer
            OutlinedButton(
                onClick = {
                    viewModel.clearDemoContacts()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Exit Demo Mode")
            }

            Text(
                text = "Demo mode simulates all actions. No real alerts, calls, or messages are sent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DemoControlCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Start Demo")
        }
    }
}
