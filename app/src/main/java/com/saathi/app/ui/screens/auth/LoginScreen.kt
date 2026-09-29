package com.saathi.app.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.saathi.app.Screen
import com.saathi.app.data.local.PreferencesManager
import com.saathi.app.ui.theme.PrimaryTeal
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SecondaryAmber
import kotlinx.coroutines.launch

enum class LoginTab { MOBILE_OTP, EMAIL_PASSWORD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    preferencesManager: PreferencesManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(LoginTab.MOBILE_OTP) }

    // Mobile + OTP state
    var phoneNumber by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var generatedOtp by remember { mutableStateOf("789123") }

    // Email + Password state
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Name field
    var fullNameInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Logo / Shield Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(PrimaryTeal.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SAATHI LOGIN PORTAL",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Secure Student & Citizen Safety Network",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tab Selector: Mobile OTP vs Email Password
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = PrimaryTeal,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == LoginTab.MOBILE_OTP,
                    onClick = { selectedTab = LoginTab.MOBILE_OTP },
                    text = { Text("Mobile + OTP", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == LoginTab.EMAIL_PASSWORD,
                    onClick = { selectedTab = LoginTab.EMAIL_PASSWORD },
                    text = { Text("Email + Password", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tab 1: Mobile + OTP
            if (selectedTab == LoginTab.MOBILE_OTP) {
                OutlinedTextField(
                    value = fullNameInput,
                    onValueChange = { fullNameInput = it },
                    label = { Text("Full Name (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.length <= 10) phoneNumber = it.filter { ch -> ch.isDigit() } },
                    label = { Text("10-Digit Mobile Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    prefix = { Text("+91 ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isOtpSent) {
                    Button(
                        onClick = {
                            if (phoneNumber.length == 10) {
                                isOtpSent = true
                                Toast.makeText(context, "OTP sent to +91 $phoneNumber: $generatedOtp", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SEND VERIFICATION OTP", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Demo OTP Quick-Fill helper card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { otpInput = generatedOtp },
                        colors = CardDefaults.cardColors(containerColor = SecondaryAmber.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = SecondaryAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Test OTP: $generatedOtp", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Tap here to auto-fill OTP for quick verification", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { if (it.length <= 6) otpInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Enter 6-Digit OTP") },
                        leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (otpInput == generatedOtp || otpInput.length == 6) {
                                coroutineScope.launch {
                                    val name = fullNameInput.ifBlank { "Alok Sharma" }
                                    preferencesManager.setUserPhone("+91 $phoneNumber")
                                    preferencesManager.setUserName(name)
                                    preferencesManager.setLoggedIn(true)
                                    preferencesManager.setOnboardingComplete(true)
                                    Toast.makeText(context, "Login Successful! Welcome to SAATHI.", Toast.LENGTH_SHORT).show()
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Invalid OTP! Try: $generatedOtp", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VERIFY & PROCEED", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Tab 2: Email + Password
                OutlinedTextField(
                    value = fullNameInput,
                    onValueChange = { fullNameInput = it },
                    label = { Text("Full Name (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (emailInput.isNotBlank() && passwordInput.length >= 4) {
                            coroutineScope.launch {
                                val name = fullNameInput.ifBlank { "User" }
                                preferencesManager.setUserEmail(emailInput)
                                preferencesManager.setUserName(name)
                                preferencesManager.setLoggedIn(true)
                                preferencesManager.setOnboardingComplete(true)
                                Toast.makeText(context, "Logged in as $emailInput", Toast.LENGTH_SHORT).show()
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Enter a valid email and minimum 4-char password", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("LOGIN WITH PASSWORD", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 1-Tap Demo Student Login (For Presentation & Fast Testing)
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        preferencesManager.setUserName("Alok Sharma")
                        preferencesManager.setUserPhone("+91 9876543210")
                        preferencesManager.setUserEmail("alok.sharma@saathi.org")
                        preferencesManager.setLoggedIn(true)
                        preferencesManager.setOnboardingComplete(true)
                        Toast.makeText(context, "Demo Login Active: Alok Sharma (+91 9876543210)", Toast.LENGTH_SHORT).show()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryTeal)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = PrimaryTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("1-TAP DEMO STUDENT LOGIN", fontWeight = FontWeight.Bold, color = PrimaryTeal)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    coroutineScope.launch {
                        preferencesManager.setOnboardingComplete(true)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            ) {
                Text(
                    text = "Skip for Now & Continue as Guest",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}
