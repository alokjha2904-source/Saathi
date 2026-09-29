package com.saathi.app

import com.saathi.app.util.MultiTapDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiTapDetectorTest {

    @Test
    fun testFourRapidTapsTriggersSOS() {
        var triggeredCount = 0
        val detector = MultiTapDetector(requiredTaps = 4, windowMs = 2000L, cooldownMs = 3000L) {
            triggeredCount++
        }

        // Tap 4 times
        detector.onTap()
        detector.onTap()
        detector.onTap()
        detector.onTap()

        assertEquals("4 taps within window must trigger callback exactly once", 1, triggeredCount)
    }

    @Test
    fun testThreeTapsDoesNotTriggerSOS() {
        var triggeredCount = 0
        val detector = MultiTapDetector(requiredTaps = 4, windowMs = 2000L, cooldownMs = 3000L) {
            triggeredCount++
        }

        // Tap 3 times only
        detector.onTap()
        detector.onTap()
        detector.onTap()

        assertEquals("3 taps must not trigger 4-tap callback", 0, triggeredCount)
    }

    @Test
    fun testResetClearsTapHistory() {
        var triggeredCount = 0
        val detector = MultiTapDetector(requiredTaps = 4, windowMs = 2000L, cooldownMs = 3000L) {
            triggeredCount++
        }

        detector.onTap()
        detector.onTap()
        detector.reset()
        detector.onTap()
        detector.onTap()

        assertEquals("Reset must clear prior taps, preventing false trigger", 0, triggeredCount)
    }

    @Test
    fun testCooldownPreventsImmediateRetrigger() {
        var triggeredCount = 0
        val detector = MultiTapDetector(requiredTaps = 4, windowMs = 2000L, cooldownMs = 3000L) {
            triggeredCount++
        }

        // First trigger
        detector.onTap()
        detector.onTap()
        detector.onTap()
        detector.onTap()
        assertEquals(1, triggeredCount)

        // Immediate subsequent taps during cooldown should be ignored
        detector.onTap()
        detector.onTap()
        detector.onTap()
        detector.onTap()
        assertEquals("Cooldown must prevent immediate duplicate triggering", 1, triggeredCount)
    }
}
