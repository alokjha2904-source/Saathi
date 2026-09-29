package com.saathi.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.saathi.app.Screen
import com.saathi.app.data.local.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    preferencesManager: PreferencesManager = PreferencesManager(androidx.compose.ui.platform.LocalContext.current)
) {
    val coroutineScope = rememberCoroutineScope()
    
    val phone by preferencesManager.userPhone.collectAsState(initial = "")
    val email by preferencesManager.userEmail.collectAsState(initial = "")
    // Assuming userName might exist, or use email prefix. 
    // We'll use local state for simplicity if it's not in the contract, but will try to store it.
    var nameState by remember { mutableStateOf("User") }
    
    var showEditDialog by remember { mutableStateOf(false) }
    var editField by remember { mutableStateOf("") }
    var editValue by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nameState.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ProfileField(label = "Full Name", value = nameState) {
                editField = "Name"
                editValue = nameState
                showEditDialog = true
            }
            ProfileField(label = "Phone Number", value = phone) {
                editField = "Phone"
                editValue = phone
                showEditDialog = true
            }
            ProfileField(label = "Email (Optional)", value = email) {
                editField = "Email"
                editValue = email
                showEditDialog = true
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            NavigationCard(title = "MY SAFETY CIRCLE") { navController.navigate(Screen.TrustedContacts.route) }
            NavigationCard(title = "EMERGENCY HISTORY") { navController.navigate(Screen.IncidentHistory.route) }
            NavigationCard(title = "MY EMERGENCY FILES") { navController.navigate(Screen.EmergencyFiles.route) }
            NavigationCard(title = "SAFETY SETTINGS") { navController.navigate(Screen.Settings.route) }
            NavigationCard(title = "PRIVACY SETTINGS") { showPrivacyDialog = true }
        }
    }
    
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit $editField") },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text(editField) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        when (editField) {
                            "Name" -> nameState = editValue
                            "Phone" -> preferencesManager.setUserPhone(editValue)
                            "Email" -> preferencesManager.setUserEmail(editValue)
                        }
                    }
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Settings") },
            text = {
                Text("Your privacy matters. Location is only shared during an active SOS. There is no background tracking, and all recordings are stored locally on your device.")
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ProfileField(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value.ifEmpty { "Not set" }, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun NavigationCard(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        PaddingValues(16.dp)
        Text(
            text = title,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
