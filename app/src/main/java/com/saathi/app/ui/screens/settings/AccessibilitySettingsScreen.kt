package com.saathi.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.saathi.app.data.local.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val isLargeText by preferencesManager.isLargeText.collectAsState(initial = false)
    val isHighContrast by preferencesManager.isHighContrast.collectAsState(initial = false)
    val isReducedMotion by preferencesManager.isReducedMotion.collectAsState(initial = false)
    val language by preferencesManager.language.collectAsState(initial = "English")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessibility") },
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
            item { SettingsSectionTitle("Visual") }
            item {
                SettingsSwitchItem(
                    title = "Large Text",
                    subtitle = "Increase text size throughout the app",
                    checked = isLargeText,
                    onCheckedChange = { scope.launch { preferencesManager.setLargeText(it) } }
                )
                SettingsSwitchItem(
                    title = "High Contrast",
                    subtitle = "Enhance visual contrast for better readability",
                    checked = isHighContrast,
                    onCheckedChange = { scope.launch { preferencesManager.setHighContrast(it) } }
                )
                HorizontalDivider()
            }

            item { SettingsSectionTitle("Motion") }
            item {
                SettingsSwitchItem(
                    title = "Reduced Motion",
                    subtitle = "Minimize animations and transitions",
                    checked = isReducedMotion,
                    onCheckedChange = { scope.launch { preferencesManager.setReducedMotion(it) } }
                )
                HorizontalDivider()
            }

            item { SettingsSectionTitle("Interaction") }
            item {
                InfoCard("All emergency buttons meet minimum 48dp touch targets")
                InfoCard("Screen reader labels are provided for all actions")
                HorizontalDivider()
            }

            item { SettingsSectionTitle("Language") }
            item {
                SettingsItem(
                    title = "App Language",
                    subtitle = language,
                    onClick = {
                        val nextLang = if (language == "English") "हिन्दी" else "English"
                        scope.launch { preferencesManager.setLanguage(nextLang) }
                    }
                )
                HorizontalDivider()
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = "SAATHI is designed to be accessible to everyone. Emergency functions work with minimal interaction.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            Icons.Default.Info, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
