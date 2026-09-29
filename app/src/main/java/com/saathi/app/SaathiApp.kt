package com.saathi.app

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saathi.app.data.local.PreferencesManager
import com.saathi.app.ui.screens.ai.AIAssistantScreen
import com.saathi.app.ui.screens.checkin.SafetyCheckInScreen
import com.saathi.app.ui.screens.contacts.SafetyCircleScreen
import com.saathi.app.ui.screens.contacts.TrustedContactsScreen
import com.saathi.app.ui.screens.demo.DemoModeScreen
import com.saathi.app.ui.screens.emergency.EmergencyActiveScreen
import com.saathi.app.ui.screens.help.HelpScreen
import com.saathi.app.ui.screens.history.IncidentHistoryScreen
import com.saathi.app.ui.screens.home.HomeScreen
import com.saathi.app.ui.screens.location.LiveLocationScreen
import com.saathi.app.ui.screens.onboarding.OnboardingScreen
import com.saathi.app.ui.screens.permissions.PermissionSetupScreen
import com.saathi.app.ui.screens.safeplaces.SafePlacesScreen
import com.saathi.app.ui.screens.settings.AccessibilitySettingsScreen
import com.saathi.app.ui.screens.settings.SettingsScreen
import com.saathi.app.ui.screens.sos.SOSActivationScreen
import com.saathi.app.ui.screens.splash.SplashScreen
import com.saathi.app.ui.screens.profile.ProfileScreen
import com.saathi.app.ui.screens.files.EmergencyFilesScreen
import com.saathi.app.ui.screens.emergency.EmergencyLockScreen
import com.saathi.app.ui.screens.emergency.PostEmergencyScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object PermissionSetup : Screen("permission_setup")
    object Home : Screen("home")
    object SOSActivation : Screen("sos_activation")
    object EmergencyActive : Screen("emergency_active")
    object EmergencyLock : Screen("emergency_lock")
    object PostEmergency : Screen("post_emergency")
    object TrustedContacts : Screen("trusted_contacts")
    object SafetyCircle : Screen("safety_circle")
    object LiveLocation : Screen("live_location")
    object SafePlaces : Screen("safe_places")
    object IncidentHistory : Screen("incident_history")
    object SafetyCheckIn : Screen("safety_check_in")
    object AIAssistant : Screen("ai_assistant")
    object Settings : Screen("settings")
    object AccessibilitySettings : Screen("accessibility_settings")
    object DemoMode : Screen("demo_mode")
    object HelpScreen : Screen("help_screen")
    object Profile : Screen("profile")
    object EmergencyFiles : Screen("emergency_files")
    object Login : Screen("login")
}

@Composable
fun SaathiApp(shakeTrigger: Long = 0L) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val isDemoMode by preferencesManager.isDemoMode.collectAsState(initial = false)

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0L) {
            navController.navigate(Screen.SOSActivation.route)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash & Intro
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController, preferencesManager = preferencesManager)
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController = navController, preferencesManager = preferencesManager)
        }
        composable(Screen.PermissionSetup.route) {
            PermissionSetupScreen(navController = navController)
        }
        composable("permissions") {
            PermissionSetupScreen(navController = navController)
        }

        // Login Portal
        composable(Screen.Login.route) {
            com.saathi.app.ui.screens.auth.LoginScreen(
                navController = navController,
                preferencesManager = preferencesManager
            )
        }

        // Home
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable("Home") {
            HomeScreen(navController = navController)
        }

        // SOS & Emergency
        composable(Screen.SOSActivation.route) {
            SOSActivationScreen(navController = navController, isDemoMode = isDemoMode)
        }
        composable("SOSActivation") {
            SOSActivationScreen(navController = navController, isDemoMode = isDemoMode)
        }
        composable(
            route = "sos_activation?description={description}&category={category}",
            arguments = listOf(
                navArgument("description") { type = NavType.StringType; defaultValue = "" },
                navArgument("category") { type = NavType.StringType; defaultValue = "SOS" }
            )
        ) { backStackEntry ->
            val desc = backStackEntry.arguments?.getString("description") ?: ""
            val cat = backStackEntry.arguments?.getString("category") ?: "SOS"
            SOSActivationScreen(
                navController = navController,
                isDemoMode = isDemoMode,
                initialDescription = desc,
                initialCategory = cat
            )
        }

        composable(Screen.EmergencyActive.route) {
            EmergencyActiveScreen(navController = navController, eventId = null)
        }
        composable(
            route = "emergency_active/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            EmergencyActiveScreen(navController = navController, eventId = eventId)
        }
        composable(
            route = "EmergencyActive/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            EmergencyActiveScreen(navController = navController, eventId = eventId)
        }

        // Emergency Lock Mode & Post Emergency Summary
        composable(
            route = "emergency_lock/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EmergencyLockScreen(navController = navController, eventId = eventId)
        }
        composable(
            route = "post_emergency/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            PostEmergencyScreen(navController = navController, eventId = eventId)
        }

        // Contacts & Safety Circle
        composable(Screen.TrustedContacts.route) {
            TrustedContactsScreen(navController = navController)
        }
        composable("TrustedContacts") {
            TrustedContactsScreen(navController = navController)
        }

        composable(Screen.SafetyCircle.route) {
            SafetyCircleScreen(navController = navController)
        }
        composable("SafetyCircle") {
            SafetyCircleScreen(navController = navController)
        }

        // Location & Safe Places
        composable(Screen.LiveLocation.route) {
            LiveLocationScreen(navController = navController)
        }
        composable("LiveLocation") {
            LiveLocationScreen(navController = navController)
        }

        composable(Screen.SafePlaces.route) {
            SafePlacesScreen(navController = navController)
        }
        composable("SafePlaces") {
            SafePlacesScreen(navController = navController)
        }

        // Travel Check-In & History
        composable(Screen.SafetyCheckIn.route) {
            SafetyCheckInScreen(navController = navController)
        }
        composable("SafetyCheckIn") {
            SafetyCheckInScreen(navController = navController)
        }

        composable(Screen.IncidentHistory.route) {
            IncidentHistoryScreen(navController = navController)
        }
        composable("IncidentHistory") {
            IncidentHistoryScreen(navController = navController)
        }

        // AI Emergency Assistant
        composable(Screen.AIAssistant.route) {
            AIAssistantScreen(navController = navController)
        }
        composable("AIAssistant") {
            AIAssistantScreen(navController = navController)
        }

        // Settings, Accessibility, Demo & Help
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable("Settings") {
            SettingsScreen(navController = navController)
        }

        composable(Screen.AccessibilitySettings.route) {
            AccessibilitySettingsScreen(navController = navController)
        }
        composable("AccessibilitySettings") {
            AccessibilitySettingsScreen(navController = navController)
        }

        composable(Screen.DemoMode.route) {
            DemoModeScreen(navController = navController)
        }
        composable("DemoMode") {
            DemoModeScreen(navController = navController)
        }

        composable(Screen.HelpScreen.route) {
            HelpScreen(navController = navController)
        }
        composable("HelpScreen") {
            HelpScreen(navController = navController)
        }

        // Profile & Files
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable("Profile") {
            ProfileScreen(navController = navController)
        }
        composable(Screen.EmergencyFiles.route) {
            EmergencyFilesScreen(navController = navController)
        }
        composable("EmergencyFiles") {
            EmergencyFilesScreen(navController = navController)
        }
    }
}
