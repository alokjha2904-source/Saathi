package com.saathi.app.ui.screens.contacts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.saathi.app.data.model.TrustedContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedContactsScreen(
    navController: NavController,
    viewModel: TrustedContactsViewModel = viewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<TrustedContact?>(null) }
    var contactToDelete by remember { mutableStateOf<TrustedContact?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Trusted Contacts", fontWeight = FontWeight.Bold)
                        Text(
                            "Your Safety Circle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                contactToEdit = null
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    ) { paddingValues ->
        if (contacts.isEmpty()) {
            EmptyStateView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onAddClick = {
                    contactToEdit = null
                    showAddDialog = true
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts, key = { it.id }) { contact ->
                    ContactCard(
                        contact = contact,
                        onEdit = {
                            contactToEdit = contact
                            showAddDialog = true
                        },
                        onDelete = {
                            contactToDelete = contact
                        },
                        onSetPrimary = {
                            viewModel.setPrimaryContact(contact)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        ContactBottomSheet(
            contact = contactToEdit,
            onDismiss = {
                showAddDialog = false
                contactToEdit = null
            },
            onSave = { updatedContact ->
                if (contactToEdit == null) {
                    viewModel.addContact(updatedContact)
                } else {
                    viewModel.updateContact(updatedContact)
                }
                showAddDialog = false
                contactToEdit = null
            }
        )
    }

    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Remove Contact?") },
            text = { Text("Remove ${contact.name} from your Safety Circle?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteContact(contact)
                    contactToDelete = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmptyStateView(modifier: Modifier = Modifier, onAddClick: () -> Unit) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No trusted contacts yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add people you trust to your safety circle.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Contact")
        }
    }
}

@Composable
fun ContactCard(
    contact: TrustedContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetPrimary: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val primaryColor = Color(0xFF009688)

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (contact.isPrimary) BorderStroke(2.dp, primaryColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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
                    .background(getRelationshipColor(contact.relationship), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (contact.isPrimary) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Primary",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.height(24.dp)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = contact.relationship,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    if (contact.receiveLiveLocation) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = com.saathi.app.ui.theme.PrimaryTeal.copy(alpha = 0.15f),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = com.saathi.app.ui.theme.PrimaryTeal,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Live GPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.saathi.app.ui.theme.PrimaryTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    if (!contact.isPrimary) {
                        DropdownMenuItem(
                            text = { Text("Set as Primary") },
                            onClick = {
                                showMenu = false
                                onSetPrimary()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

fun getRelationshipColor(relationship: String): Color {
    return when (relationship.lowercase()) {
        "parent" -> Color(0xFFE91E63)
        "sibling" -> Color(0xFF9C27B0)
        "friend" -> Color(0xFF2196F3)
        "guardian" -> Color(0xFFFF9800)
        "campus security" -> Color(0xFF1565C0)
        "faculty" -> Color(0xFF009688)
        "roommate" -> Color(0xFF795548)
        else -> Color(0xFF607D8B)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactBottomSheet(
    contact: TrustedContact?,
    onDismiss: () -> Unit,
    onSave: (TrustedContact) -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "Friend") }
    var notifyOnSOS by remember { mutableStateOf(contact?.notifyOnSOS ?: true) }
    var notifyOnCheckIn by remember { mutableStateOf(contact?.notifyOnCheckIn ?: false) }
    var receiveLiveLocation by remember { mutableStateOf(contact?.receiveLiveLocation ?: true) }

    var relationshipExpanded by remember { mutableStateOf(false) }
    val relationshipOptions = listOf("Parent", "Sibling", "Friend", "Guardian", "Campus Security", "Faculty", "Roommate", "Other")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (contact == null) "Add Contact" else "Edit Contact",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val digitsOnly = phone.replace(Regex("[^0-9+]"), "").replace("+", "")
            val isPhoneValid = digitsOnly.length in 7..15
            val isPhoneError = phone.isNotBlank() && !isPhoneValid

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = isPhoneError,
                supportingText = if (isPhoneError) {
                    { Text("Enter a valid phone number (7-15 digits)", color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = relationshipExpanded,
                onExpandedChange = { relationshipExpanded = !relationshipExpanded }
            ) {
                OutlinedTextField(
                    value = relationship,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Relationship") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relationshipExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = relationshipExpanded,
                    onDismissRequest = { relationshipExpanded = false }
                ) {
                    relationshipOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                relationship = selectionOption
                                relationshipExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Notify on SOS")
                Switch(checked = notifyOnSOS, onCheckedChange = { notifyOnSOS = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Notify on missed Check-in")
                Switch(checked = notifyOnCheckIn, onCheckedChange = { notifyOnCheckIn = it })
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Live Location During Emergency", fontWeight = FontWeight.Medium)
                    Switch(checked = receiveLiveLocation, onCheckedChange = { receiveLiveLocation = it })
                }
                Text(
                    text = "When enabled, trusted contacts may receive location updates during an active emergency.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank() && isPhoneValid) {
                            onSave(
                                TrustedContact(
                                    id = contact?.id ?: 0,
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    relationship = relationship,
                                    isPrimary = contact?.isPrimary ?: false,
                                    notifyOnSOS = notifyOnSOS,
                                    notifyOnCheckIn = notifyOnCheckIn,
                                    receiveLiveLocation = receiveLiveLocation
                                )
                            )
                        }
                    },
                    enabled = name.isNotBlank() && isPhoneValid
                ) {
                    Text("Save")
                }
            }
        }
    }
}
