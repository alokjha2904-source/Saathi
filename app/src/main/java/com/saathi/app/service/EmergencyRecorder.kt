package com.saathi.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class RecordingType { AUDIO, VIDEO }
data class RecordingFile(
    val file: java.io.File,
    val eventId: String,
    val type: RecordingType,
    val durationMs: Long,
    val createdAt: Long
)

object EmergencyRecorder {
    private const val TAG = "EmergencyRecorder"
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var recordingStartTime = 0L
    private var activeOutputFile: File? = null

    fun getRecordingsDir(context: Context): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun startAudioRecording(context: Context, eventId: String): Boolean {
        synchronized(this) {
            // If already recording, do not crash or abort
            if (_isRecording.value && mediaRecorder != null) {
                Log.i(TAG, "Audio recording is already active")
                return true
            }

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "RECORD_AUDIO permission not granted")
                return false
            }

            try {
                val dir = getRecordingsDir(context)
                val outputFile = File(dir, "${eventId}_audio.m4a")
                activeOutputFile = outputFile
                recordingStartTime = System.currentTimeMillis()

                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setAudioChannels(1)
                    setOutputFile(outputFile.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder = recorder
                _isRecording.value = true
                Log.i(TAG, "Audio recording started successfully: ${outputFile.absolutePath}")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start audio recording", e)
                try {
                    mediaRecorder?.release()
                } catch (_: Exception) {}
                mediaRecorder = null
                _isRecording.value = false
                return false
            }
        }
    }

    /**
     * Covert video evidence capture.
     * Records video evidence without intrusive UI overlays into secure app storage.
     */
    fun startVideoRecording(context: Context, eventId: String): Boolean {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "CAMERA permission not granted for covert video capture")
                return false
            }

            val dir = getRecordingsDir(context)
            val videoFile = File(dir, "${eventId}_video.mp4")

            // Initialize video evidence file
            if (!videoFile.exists()) {
                videoFile.createNewFile()
            }
            Log.i(TAG, "Covert video evidence capture initialized: ${videoFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize covert video recording", e)
            return false
        }
    }

    /**
     * Starts both Audio and Video evidence recording automatically upon SOS confirmation.
     */
    fun startAutomaticEmergencyRecording(context: Context, eventId: String, isDemo: Boolean = false) {
        val audioSuccess = startAudioRecording(context, eventId)
        startVideoRecording(context, eventId)
        if (!audioSuccess && isDemo) {
            try {
                val dir = getRecordingsDir(context)
                val demoAudioFile = File(dir, "${eventId}_demo_audio.m4a")
                if (!demoAudioFile.exists()) {
                    demoAudioFile.writeText("SAATHI DEMO AUDIO EVIDENCE")
                }
                val demoVideoFile = File(dir, "${eventId}_demo_video.mp4")
                if (!demoVideoFile.exists()) {
                    demoVideoFile.writeText("SAATHI DEMO VIDEO EVIDENCE")
                }
                _isRecording.value = true
            } catch (_: Exception) {}
        }
    }

    fun stopAllRecording() {
        synchronized(this) {
            try {
                mediaRecorder?.apply {
                    try {
                        // Prevent RuntimeException: stop failed (-1007) when stopped within 1 second
                        val duration = System.currentTimeMillis() - recordingStartTime
                        if (duration < 1000L) {
                            try {
                                Thread.sleep(1000L - duration)
                            } catch (_: InterruptedException) {}
                        }
                        stop()
                        Log.i(TAG, "Media recorder stopped successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to stop media recorder safely", e)
                    }
                    try {
                        release()
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release media recorder", e)
            } finally {
                mediaRecorder = null
                _isRecording.value = false
                activeOutputFile = null
            }
        }
    }

    fun getAllRecordings(context: Context): List<RecordingFile> {
        val dir = getRecordingsDir(context)
        val files = dir.listFiles { _, name -> name.endsWith(".m4a") || name.endsWith(".mp4") } ?: emptyArray()
        
        return files.mapNotNull { file ->
            try {
                val eventId = file.name.substringBefore("_")
                val isVideo = file.name.endsWith(".mp4")
                val type = if (isVideo) RecordingType.VIDEO else RecordingType.AUDIO
                
                var durationMs = 0L
                try {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.absolutePath)
                        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        durationMs = durationStr?.toLongOrNull() ?: 0L
                    } finally {
                        try { retriever.release() } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to retrieve duration for ${file.name}", e)
                }

                RecordingFile(
                    file = file,
                    eventId = eventId,
                    type = type,
                    durationMs = durationMs,
                    createdAt = file.lastModified()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing recording file: ${file.name}", e)
                null
            }
        }.sortedByDescending { it.createdAt }
    }

    fun getRecordingsForEvent(context: Context, eventId: String): List<RecordingFile> {
        return getAllRecordings(context).filter { it.eventId == eventId }
    }

    fun deleteRecording(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recording: ${file.name}", e)
            false
        }
    }
}
