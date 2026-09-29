package com.saathi.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.saathi.app.MainActivity

class SaathiAccessibilityService : AccessibilityService() {

    companion object {
        const val EXTRA_LOCK_SCREEN_TRIGGER = "EXTRA_LOCK_SCREEN_TRIGGER"

        fun isServiceEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${SaathiAccessibilityService::class.java.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                    componentName.contains(SaathiAccessibilityService::class.java.simpleName)
                ) {
                    return true
                }
            }
            return false
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var volumeDownPressStartTime = 0L
    private var isTriggered = false
    private var rapidPressCount = 0
    private var lastPressTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No UI manipulation needed; service only listens for key events
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val now = System.currentTimeMillis()

            if (event.action == KeyEvent.ACTION_DOWN) {
                if (volumeDownPressStartTime == 0L) {
                    volumeDownPressStartTime = now
                }

                // Check for 3 rapid presses within 1.5s
                if (now - lastPressTime < 600L) {
                    rapidPressCount++
                } else {
                    rapidPressCount = 1
                }
                lastPressTime = now

                if (rapidPressCount >= 3 && !isTriggered) {
                    triggerEmergencyFromLockScreen()
                    rapidPressCount = 0
                    return true
                }

                // Check long press (held for 2.5s)
                if (now - volumeDownPressStartTime >= 2500L && !isTriggered) {
                    triggerEmergencyFromLockScreen()
                    return true
                }
            } else if (event.action == KeyEvent.ACTION_UP) {
                volumeDownPressStartTime = 0L
                isTriggered = false
            }
        }
        return super.onKeyEvent(event)
    }

    private fun triggerEmergencyFromLockScreen() {
        isTriggered = true

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_LOCK_SCREEN_TRIGGER, true)
        }
        startActivity(launchIntent)
    }
}
