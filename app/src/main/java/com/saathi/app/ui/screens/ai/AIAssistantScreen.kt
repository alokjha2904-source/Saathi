package com.saathi.app.ui.screens.ai

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.saathi.app.data.local.PreferencesManager
import com.saathi.app.ui.theme.ErrorSOS
import com.saathi.app.ui.theme.PrimaryTeal
import com.saathi.app.ui.theme.SecondaryAmber
import com.saathi.app.util.SaathiStrings

data class AIAnalysisResult(
    val category: String,
    val urgency: String,
    val summary: String,
    val recommendedActions: List<String>
)

fun String.containsAny(vararg words: String) = words.any { this.contains(it) }

fun analyzeIncident(description: String): AIAnalysisResult {
    val lowerDesc = description.lowercase()

    val category = when {
        lowerDesc.containsAny("follow", "stalk", "watching", "creep", "पीछा") -> "Harassment/Stalking"
        lowerDesc.containsAny("fell", "fall", "hurt", "injury", "bleed", "unconscious", "faint", "चोट", "रक्त") -> "Medical Emergency"
        lowerDesc.containsAny("fire", "smoke", "burning", "आग", "धुआं") -> "Fire Emergency"
        lowerDesc.containsAny("theft", "stolen", "rob", "snatch", "चोरी") -> "Theft/Robbery"
        lowerDesc.containsAny("accident", "crash", "collision", "दुर्घटना") -> "Road Accident"
        lowerDesc.containsAny("unsafe", "danger", "threat", "scared", "afraid", "डर", "खतरा") -> "Personal Safety Threat"
        lowerDesc.containsAny("lost", "stranded", "stuck", "खो गया", "रास्ता") -> "Lost/Stranded"
        else -> "General Safety Concern"
    }

    val urgency = when {
        lowerDesc.containsAny("unconscious", "not responding", "bleed", "fire", "weapon", "attack") -> "CRITICAL"
        lowerDesc.containsAny("follow", "stalk", "hurt", "accident", "rob") -> "HIGH"
        lowerDesc.containsAny("unsafe", "scared", "lost") -> "MODERATE"
        else -> "MODERATE"
    }

    val actions = when (category) {
        "Harassment/Stalking" -> listOf(
            "Move toward a crowded, well-lit public or campus area immediately.",
            "Do not travel directly to your private room or home alone.",
            "Activate SAATHI SOS to broadcast live coordinates to your Safety Circle.",
            "Contact campus security or call 112 immediately."
        )
        "Medical Emergency" -> listOf(
            "Do not move the injured person unless there is immediate danger (e.g. fire).",
            "Call local emergency medical services or campus ambulance right away.",
            "Apply direct, firm pressure to any bleeding wounds with clean cloth.",
            "Stay on the line with the emergency dispatcher until help arrives."
        )
        "Fire Emergency" -> listOf(
            "Evacuate the area immediately using designated stairwells, never elevators.",
            "Stay low to the ground to minimize toxic smoke inhalation.",
            "Trigger the nearest fire pull alarm if safely accessible.",
            "Dial 101/112 once you reach a safe outdoor assembly area."
        )
        "Theft/Robbery" -> listOf(
            "Prioritize personal physical safety over material belongings.",
            "Move to safety before attempting to document or report.",
            "Contact police or campus security to file an official report.",
            "Immediately freeze or block compromised debit/credit cards."
        )
        "Road Accident" -> listOf(
            "Move safely away from incoming vehicular traffic.",
            "Check for personal injuries and call for ambulance support.",
            "Exchange contact and vehicle registration details calmly.",
            "Take photo evidence of the scene once safely off the roadway."
        )
        "Lost/Stranded" -> listOf(
            "Remain in a well-lit public location rather than wandering.",
            "Share your live location with your Safety Circle using SAATHI.",
            "Keep phone battery conserved by turning off unnecessary background apps.",
            "Contact a verified primary contact or local helpdesk for assistance."
        )
        else -> listOf(
            "Assess your physical surroundings and ensure immediate safety.",
            "Trigger SOS if you feel immediate personal threat.",
            "Reach out to your trusted contacts or local authorities."
        )
    }

    return AIAnalysisResult(
        category = category,
        urgency = urgency,
        summary = "Based on your input, this situation represents a $category with $urgency urgency   level.",
        recommendedActions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(navController: NavController) {
    var description by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<AIAnalysisResult?>(null) }
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val language by preferencesManager.language.collectAsState(initial = "English")
    val isHi = SaathiStrings.isHindi(language)

    val quickPrompts = if (isHi) {
        listOf(
            "कोई मेरा पीछा कर रहा है",
            "प्रयोगशाला में चोट लग गई और कोई नहीं है",
            "देर रात सुनसान रास्ते पर हूँ",
            "हॉस्टल में आग का अलार्म बज रहा है"
        )
    } else {
        listOf(
            "Someone has been following me",
            "Friend fell and is unconscious in lab",
            "Alone on deserted dark road",
            "Fire alarm ringing in building"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isHi) "AI आपातकालीन सहायक" else "AI Emergency Assistant", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isHi) "बताएं कि क्या हो रहा है। साथी AI तुरंत स्थिति का विश्लेषण कर कदम सुझाएगा।" else "Describe what is happening. SAATHI AI analyzes the situation and suggests rapid actionable next steps.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHi) "महत्वपूर्ण: AI स्थिति समझने में मदद करता है, लेकिन यह आपातकालीन सेवाओं का विकल्प नहीं है। तत्काल खतरे में SOS दबाएं।" else "Important: AI assists with situational triage but never replaces emergency services. In direct danger, activate SOS immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            item {
                Text(if (isHi) "त्वरित स्थिति चुनें:" else "Quick Scenarios:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickPrompts) { prompt ->
                        AssistChip(
                            onClick = {
                                description = prompt
                                analysisResult = analyzeIncident(prompt)
                            },
                            label = { Text(prompt, fontSize = 12.sp) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    label = { Text(if (isHi) "स्थिति का विवरण दें..." else "Describe what happened...") },
                    placeholder = {
                        Text(
                            if (isHi) "उदा. बस स्टॉप से कोई मेरा पीछा कर रहा है" else "e.g., Someone has been following me since the bus stop",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    },
                    maxLines = 5
                )
            }

            item {
                Button(
                    onClick = {
                        if (description.isNotBlank()) {
                            analysisResult = analyzeIncident(description)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHi) "स्थिति का विश्लेषण करें" else "Analyze Situation", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (analysisResult != null) {
                val result = analysisResult!!
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(if (isHi) "AI-समीक्षित स्थिति मूल्यांकन" else "AI-Assisted Incident Assessment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(if (isHi) "श्रेणी (CATEGORY)" else "CATEGORY", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(result.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
 
                        val urgencyColor = when (result.urgency) {
                            "CRITICAL" -> ErrorSOS
                            "HIGH" -> SecondaryAmber
                            else -> PrimaryTeal
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = urgencyColor)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(if (isHi) "गंभीरता (URGENCY)" else "URGENCY", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(result.urgency, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(if (isHi) "सारांश" else "SUMMARY", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(result.summary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(if (isHi) "अनुशंसित त्वरित कदम" else "RECOMMENDED IMMEDIATE ACTIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            result.recommendedActions.forEachIndexed { index, action ->
                                Text("${index + 1}. $action", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val encodedDesc = Uri.encode("${result.category}: ${result.summary}")
                                val encodedCat = Uri.encode(result.category)
                                navController.navigate("sos_activation?description=$encodedDesc&category=$encodedCat")
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorSOS)
                        ) {
                            Text(if (isHi) "SOS सक्रिय करें" else "ACTIVATE SOS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(if (isHi) "112 डायल करें" else "CALL 112", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "🚨 SAATHI Safety Alert: Situation [${result.category} / Urgency: ${result.urgency}]. ${result.summary}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Situation"))
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isHi) "स्थिति साझा करें" else "SHARE SITUATION")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Label: AI-assisted summary — not a legal or medical report. Developed for situational guidance and triage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
