package com.saathi.app.ui.screens.contacts

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.saathi.app.Screen
import com.saathi.app.data.model.TrustedContact
import com.saathi.app.ui.theme.PrimaryTeal
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCircleScreen(
    navController: NavController,
    viewModel: TrustedContactsViewModel = viewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    var selectedContact by remember { mutableStateOf<TrustedContact?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Safety Circle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = { navController.navigate(Screen.TrustedContacts.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Contacts")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (contacts.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Your Safety Circle is empty",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Add contacts to build your safety network",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navController.navigate(Screen.TrustedContacts.route) }) {
                        Text("Add Contacts")
                    }
                }
            } else {
                SafetyCircleVisualization(
                    contacts = contacts,
                    onContactClick = { selectedContact = it }
                )
            }
        }
    }

    if (selectedContact != null) {
        ModalBottomSheet(onDismissRequest = { selectedContact = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(getRelationshipColor(selectedContact!!.relationship), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedContact!!.name.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = selectedContact!!.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedContact!!.relationship,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = selectedContact!!.phone)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (selectedContact!!.notifyOnSOS) {
                        SuggestionChip(onClick = {}, label = { Text("Notified on SOS") })
                    }
                    if (selectedContact!!.notifyOnCheckIn) {
                        SuggestionChip(onClick = {}, label = { Text("Notified on Check-in") })
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyCircleVisualization(
    contacts: List<TrustedContact>,
    onContactClick: (TrustedContact) -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val radius = (minOf(maxWidth.value, maxHeight.value) / 2f) * 0.6f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasCenter = Offset(size.width / 2, size.height / 2)
            val canvasRadius = minOf(size.width, size.height) / 2f * 0.6f

            drawCircle(
                color = lineColor,
                radius = canvasRadius,
                center = canvasCenter,
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            )

            contacts.forEachIndexed { index, _ ->
                val angle = (2 * Math.PI * index / contacts.size) - (Math.PI / 2)
                val x = canvasCenter.x + canvasRadius * cos(angle).toFloat()
                val y = canvasCenter.y + canvasRadius * sin(angle).toFloat()

                drawLine(
                    color = lineColor,
                    start = canvasCenter,
                    end = Offset(x, y),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(80.dp)
                .background(PrimaryTeal, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "YOU",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        contacts.forEachIndexed { index, contact ->
            val angle = (2 * Math.PI * index / contacts.size) - (Math.PI / 2)
            val offsetX = (radius * cos(angle)).dp
            val offsetY = (radius * sin(angle)).dp

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = offsetX, y = offsetY)
                    .size(56.dp)
                    .background(getRelationshipColor(contact.relationship), CircleShape)
                    .clickable { onContactClick(contact) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
