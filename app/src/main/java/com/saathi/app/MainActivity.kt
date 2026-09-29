package com.saathi.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.saathi.app.data.local.PreferencesManager
import com.saathi.app.ui.theme.SaathiTheme
import com.saathi.app.util.ShakeDetector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private var shakeDetector: ShakeDetector? = null
    private var emergencyTrigger by mutableLongStateOf(0L)
    private lateinit var preferencesManager: PreferencesManager

    // Hardware Volume Down Trigger State
    private val mainHandler = Handler(Looper.getMainLooper())
    private var volumeDownHoldRunnable: Runnable? = null
    private var isVolumeDownActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable display over Android lock screen and turn screen on during emergency
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        preferencesManager = PreferencesManager(applicationContext)

        // Check if launched by hardware key trigger from lock screen
        if (intent?.getBooleanExtra(com.saathi.app.service.SaathiAccessibilityService.EXTRA_LOCK_SCREEN_TRIGGER, false) == true) {
            emergencyTrigger = System.currentTimeMillis()
        }

        // Setup hardware shake detector
        shakeDetector = ShakeDetector(this) {
            lifecycleScope.launch {
                val enabled = preferencesManager.isShakeSOS.first()
                if (enabled) {
                    emergencyTrigger = System.currentTimeMillis()
                }
            }
        }

        setContent {
            val themePreference by preferencesManager.theme.collectAsState(initial = "System")
            val isHighContrast by preferencesManager.isHighContrast.collectAsState(initial = false)
            val isLargeText by preferencesManager.isLargeText.collectAsState(initial = false)

            SaathiTheme(
                themePreference = themePreference,
                isHighContrast = isHighContrast,
                isLargeText = isLargeText
            ) {
                SaathiApp(shakeTrigger = emergencyTrigger)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.getBooleanExtra(com.saathi.app.service.SaathiAccessibilityService.EXTRA_LOCK_SCREEN_TRIGGER, false) == true) {
            emergencyTrigger = System.currentTimeMillis()
        }
    }

    override fun onResume() {
        super.onResume()
        shakeDetector?.start()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector?.stop()
        cancelVolumeDownTimer()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (!isVolumeDownActive) {
                isVolumeDownActive = true
                lifecycleScope.launch {
                    val isEnabled = preferencesManager.isVolumeDownSOS.first()
                    val holdDurationSec = preferencesManager.volumeDownHoldSeconds.first()

                    if (isEnabled) {
                        val holdDurationMs = holdDurationSec * 1000L
                        volumeDownHoldRunnable = Runnable {
                            emergencyTrigger = System.currentTimeMillis()
                        }
                        mainHandler.postDelayed(volumeDownHoldRunnable!!, holdDurationMs)
                    }
                }
            }
            // Return false to allow system volume change if released before threshold
            return super.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            cancelVolumeDownTimer()
            isVolumeDownActive = false
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun cancelVolumeDownTimer() {
        volumeDownHoldRunnable?.let {
            mainHandler.removeCallbacks(it)
            volumeDownHoldRunnable = null
        }
    }
}
