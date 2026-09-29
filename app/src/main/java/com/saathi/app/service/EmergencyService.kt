package com.saathi.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.saathi.app.MainActivity
import com.saathi.app.R
import com.saathi.app.SaathiApplication
import com.saathi.app.data.local.SaathiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmergencyService : Service() {

    companion object {
        const val NOTIFICATION_ID = 911
        const val ACTION_START = "ACTION_START_EMERGENCY"
        const val ACTION_STOP = "ACTION_STOP_EMERGENCY"
        const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"

        fun startEmergency(context: Context, eventId: String) {
            val intent = Intent(context, EmergencyService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_EVENT_ID, eventId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopEmergency(context: Context) {
            val intent = Intent(context, EmergencyService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var activeEventId: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: activeEventId
                if (eventId.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = SaathiDatabase.getDatabase(applicationContext)
                            val existing = db.emergencyEventDao().getEmergencyEventByEventId(eventId)
                            if (existing != null) {
                                db.emergencyEventDao().updateEmergencyEvent(
                                    existing.copy(status = "RESOLVED", endTimestamp = System.currentTimeMillis())
                                )
                            }
                        } catch (_: Exception) {}
                    }
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START -> {
                activeEventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: "ACTIVE"
                startForegroundNotification(activeEventId)
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification(eventId: String) {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
        val callPendingIntent = PendingIntent.getActivity(
            this,
            1,
            callIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, EmergencyService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, SaathiApplication.EMERGENCY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 SAATHI SOS IS ACTIVE")
            .setContentText("Live GPS broadcast in progress • Event #$eventId")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "CALL 112", callPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "I'M SAFE", stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}
