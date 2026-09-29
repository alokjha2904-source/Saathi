package com.saathi.app.ui.screens.safeplaces

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.saathi.app.data.local.PreferencesManager
import com.saathi.app.data.model.SafePlace
import com.saathi.app.ui.theme.ErrorSOS
import com.saathi.app.ui.theme.PrimaryTeal
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SecondaryAmber
import com.saathi.app.util.LocationHelper
import com.saathi.app.util.LocationResult
import com.saathi.app.util.SaathiStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafePlacesScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    val isDemoMode by preferencesManager.isDemoMode.collectAsState(initial = false)
    val language by preferencesManager.language.collectAsState(initial = "English")
    val isHi = SaathiStrings.isHindi(language)

    var userLocation by remember { mutableStateOf<LocationResult?>(null) }
    var isLocating by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val loc = LocationHelper.getFreshLocation(context)
            withContext(Dispatchers.Main) {
                userLocation = loc
                isLocating = false
            }
        }
    }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Hospital", "Police Station", "Campus Security", "Pharmacy", "Safe Location")

    // Isolated demo dataset strictly for Demo Mode
    val demoSafePlaces = remember {
        listOf(
            SafePlace(1, "KIIT Hospital", "HOSPITAL", 20.3541, 85.8145, "KIIT Campus, Patia", "+91 674 2725113", 0.3f),
            SafePlace(2, "Chandrasekharpur Police Station", "POLICE", 20.3385, 85.8185, "Chandrasekharpur, Bhubaneswar", "100", 1.2f),
            SafePlace(3, "KIIT Campus Security", "CAMPUS_SECURITY", 20.3534, 85.8149, "Gate 1, KIIT Campus", "+91 674 2725000", 0.1f),
            SafePlace(4, "Apollo Pharmacy", "PHARMACY", 20.3510, 85.8190, "Patia Square", "+91 674 2350000", 0.8f),
            SafePlace(5, "Student Wellness Center", "SAFE_LOCATION", 20.3545, 85.8155, "Campus 15, KIIT", "+91 674 2725050", 0.2f),
            SafePlace(6, "Women's Helpdesk", "SAFE_LOCATION", 20.3530, 85.8160, "Admin Block, KIIT", "+91 674 2725100", 0.4f)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isHi) "नजदीकी सुरक्षित स्थान" else "Nearby Safe Places", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mode Banner
            if (isDemoMode) {
                Surface(
                    color = Color(0xFFFFC107).copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DEMO MODE — Showing sample campus locations for evaluation",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                    }
                }
            }

            // Categories horizontal bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            if (isDemoMode) {
                // Render Demo list with filter
                val filteredDemo = if (selectedCategory == "All") {
                    demoSafePlaces
                } else {
                    demoSafePlaces.filter { it.category.replace("_", " ").equals(selectedCategory, ignoreCase = true) }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredDemo) { place ->
                        SafePlaceCard(
                            place = place,
                            onNavigate = {
                                val uri = Uri.parse("geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}(${place.name})")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            onCall = {
                                if (place.phoneNumber.isNotEmpty()) {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${place.phoneNumber}")))
                                }
                            }
                        )
                    }
                }
            } else {
                // REAL MODE: Calculate dynamically from user coordinates!
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Location Header Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isHi) "आपका वास्तविक स्थान" else "Your GPS Location",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    val loc = userLocation
                                    if (loc != null) {
                                        Text(
                                            text = "${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)} (Accuracy ±${loc.accuracy.toInt()}m)",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else if (isLocating) {
                                        Text(
                                            text = "Acquiring live GPS fix...",
                                            fontSize = 12.sp,
                                            color = SecondaryAmber
                                        )
                                    } else {
                                        Text(
                                            text = "Location permission needed or searching for satellites",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Google Maps Navigation Discoveries centered on user's REAL coordinates
                    item {
                        Text(
                            text = if (isHi) "नजदीकी आपातकालीन सहायता केंद्र खोजें" else "Find Verified Emergency Havens Near You",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    val userLat = userLocation?.latitude
                    val userLng = userLocation?.longitude

                    val searchQueries = listOf(
                        Triple("Police Stations", "Find official local police stations & PCR booths near your current coordinates", Icons.Default.LocalPolice to "police+station"),
                        Triple("Hospitals & Trauma Care", "Find nearest emergency rooms, casualty departments & medical centers", Icons.Default.LocalHospital to "hospital"),
                        Triple("24/7 Pharmacies", "Locate open chemist shops and medical dispensaries", Icons.Default.LocalPharmacy to "pharmacy"),
                        Triple("Women's Help Desks", "Find dedicated women's police stations and security helplines", Icons.Default.Security to "women+police+station"),
                        Triple("Emergency Help Points", "Find verified emergency public safety posts and transit booths", Icons.Default.Home to "emergency+service")
                    )

                    val filteredQueries = if (selectedCategory == "All") {
                        searchQueries
                    } else {
                        searchQueries.filter { it.first.contains(selectedCategory, ignoreCase = true) || selectedCategory.contains(it.first, ignoreCase = true) }
                    }

                    items(filteredQueries) { (title, subtitle, pair) ->
                        val (icon, query) = pair
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        val uriString = if (userLat != null && userLng != null) {
                                            "geo:$userLat,$userLng?q=$query"
                                        } else {
                                            "geo:0,0?q=$query"
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Find Nearest on Google Maps", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafePlaceCard(
    place: SafePlace,
    onNavigate: () -> Unit,
    onCall: () -> Unit
) {
    val icon = when (place.category) {
        "HOSPITAL" -> Icons.Default.LocalHospital
        "POLICE" -> Icons.Default.LocalPolice
        "PHARMACY" -> Icons.Default.LocalPharmacy
        "CAMPUS_SECURITY" -> Icons.Default.Security
        else -> Icons.Default.Home
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = place.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    SuggestionChip(
                        onClick = { },
                        label = { Text(place.category.replace("_", " ")) },
                        modifier = Modifier.height(24.dp)
                    )
                }
                Text(text = "${place.distanceKm} km away", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = place.address, style = MaterialTheme.typography.bodyMedium)

            if (place.phoneNumber.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = place.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNavigate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate")
                }

                if (place.phoneNumber.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onCall,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call")
                    }
                }
            }
        }
    }
}
