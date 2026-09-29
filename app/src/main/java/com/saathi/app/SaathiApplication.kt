package com.saathi.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SaathiApplication : Application() {

    companion object {
        const val EMERGENCY_CHANNEL_ID = "saathi_emergency_channel"
        const val CHECKIN_CHANNEL_ID = "saathi_checkin_channel"
        const val RECORDING_CHANNEL_ID = "saathi_recording_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // High-priority emergency SOS channel
            val emergencyChannel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                "SAATHI Emergency SOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts and notifications during active SOS emergencies"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
                setShowBadge(true)
            }

            // Normal travel check-in / Walk With Me channel
            val checkInChannel = NotificationChannel(
                CHECKIN_CHANNEL_ID,
                "SAATHI Travel Check-In",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders and countdown updates for safety check-ins and Walk With Me journeys"
                enableVibration(true)
            }

            // Emergency Recording channel
            val recordingChannel = NotificationChannel(
                RECORDING_CHANNEL_ID,
                "SAATHI Emergency Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications during active emergency audio/video recording"
            }

            notificationManager.createNotificationChannel(emergencyChannel)
            notificationManager.createNotificationChannel(checkInChannel)
            notificationManager.createNotificationChannel(recordingChannel)
        }
    }
}
