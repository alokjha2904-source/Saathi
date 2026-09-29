package com.saathi.app.ui.screens.files

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.saathi.app.service.EmergencyRecorder
import com.saathi.app.service.RecordingFile
import com.saathi.app.service.RecordingType
import com.saathi.app.ui.theme.ErrorSOS
import com.saathi.app.ui.theme.PrimaryTeal
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SecondaryAmber
import java.text.SimpleDateFormat
import java.util.*

import android.media.MediaPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyFilesScreen(navController: NavController) {
    val context = LocalContext.current
    var recordings by remember { mutableStateOf<List<RecordingFile>>(emptyList()) }
    var recordingToDelete by remember { mutableStateOf<RecordingFile?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var currentlyPlayingPath by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    LaunchedEffect(Unit) {
        recordings = EmergencyRecorder.getAllRecordings(context)
    }

    val filteredRecordings = remember(recordings, selectedFilter) {
        when (selectedFilter) {
            "VIDEO" -> recordings.filter { it.type == RecordingType.VIDEO }
            "AUDIO" -> recordings.filter { it.type == RecordingType.AUDIO }
            else -> recordings
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Emergency Files", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
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
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All Evidence (${recordings.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "VIDEO",
                    onClick = { selectedFilter = "VIDEO" },
                    label = { Text("🎥 Videos (${recordings.count { it.type == RecordingType.VIDEO }})") }
                )
                FilterChip(
                    selected = selectedFilter == "AUDIO",
                    onClick = { selectedFilter = "AUDIO" },
                    label = { Text("🎙️ Audios (${recordings.count { it.type == RecordingType.AUDIO }})") }
                )
            }

            if (filteredRecordings.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = PrimaryTeal
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No saved emergency files yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Audio and covert video evidence are captured automatically during an active SOS and preserved here securely as proof.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRecordings) { recording ->
                        val isPlaying = currentlyPlayingPath == recording.file.absolutePath
                        RecordingCard(
                            recording = recording,
                            isPlaying = isPlaying,
                            onPlay = {
                                if (recording.type == RecordingType.AUDIO) {
                                    if (isPlaying) {
                                        try {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                        } catch (_: Exception) {}
                                        mediaPlayer = null
                                        currentlyPlayingPath = null
                                    } else {
                                        try {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                        } catch (_: Exception) {}
                                        try {
                                            val player = MediaPlayer().apply {
                                                setDataSource(recording.file.absolutePath)
                                                prepare()
                                                start()
                                                setOnCompletionListener {
                                                    currentlyPlayingPath = null
                                                }
                                            }
                                            mediaPlayer = player
                                            currentlyPlayingPath = recording.file.absolutePath
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Playback error: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    try {
                                        val uri = FileProvider.getUriForFile(context, "com.saathi.app.fileprovider", recording.file)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "video/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Open Evidence"))
                                    } catch (_: Exception) {
                                        android.widget.Toast.makeText(context, "No media app available to preview file", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onShare = {
                                try {
                                    val uri = FileProvider.getUriForFile(context, "com.saathi.app.fileprovider", recording.file)
                                    val mimeType = if (recording.type == RecordingType.AUDIO) "audio/*" else "video/*"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = mimeType
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Evidence File"))
                                } catch (_: Exception) {
                                    android.widget.Toast.makeText(context, "Cannot share evidence file", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                if (isPlaying) {
                                    try {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                    } catch (_: Exception) {}
                                    mediaPlayer = null
                                    currentlyPlayingPath = null
                                }
                                recordingToDelete = recording
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    recordingToDelete?.let { recording ->
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Delete Emergency Evidence?") },
            text = { Text("Are you sure you want to delete this evidence file? This action is permanent.") },
            confirmButton = {
                TextButton(onClick = {
                    if (EmergencyRecorder.deleteRecording(recording.file)) {
                        recordings = EmergencyRecorder.getAllRecordings(context)
                    }
                    recordingToDelete = null
                }) {
                    Text("Delete", color = ErrorSOS, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecordingCard(
    recording: RecordingFile,
    isPlaying: Boolean = false,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(recording.createdAt))
    val minutes = (recording.durationMs / 1000) / 60
    val seconds = (recording.durationMs / 1000) % 60
    val durationString = String.format("%02d:%02d", minutes, seconds)
    val isVideo = recording.type == RecordingType.VIDEO

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isVideo) SecondaryAmber.copy(alpha = 0.2f) else PrimaryTeal.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isVideo) SecondaryAmber else PrimaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isVideo) "VIDEO EVIDENCE" else "AUDIO RECORDING",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isVideo) SecondaryAmber else PrimaryTeal
                        )
                        Text(
                            text = "Event #${recording.eventId}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = if (recording.durationMs > 0) durationString else "Captured",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Recorded: $dateString",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onPlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isPlaying) ErrorSOS else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "STOP" else "PLAY",
                        color = if (isPlaying) ErrorSOS else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SHARE")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = ErrorSOS)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DELETE", color = ErrorSOS)
                }
            }
        }
    }
}
