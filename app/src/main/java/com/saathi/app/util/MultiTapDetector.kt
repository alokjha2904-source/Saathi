package com.saathi.app.util

class MultiTapDetector(
    private val requiredTaps: Int = 4,
    private val windowMs: Long = 2000L,
    private val cooldownMs: Long = 3000L,
    private val onMultiTap: () -> Unit
) {
    private val tapTimestamps = mutableListOf<Long>()
    private var lastTriggerTime = 0L

    fun onTap() {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < cooldownMs) return
        tapTimestamps.removeAll { now - it > windowMs }
        tapTimestamps.add(now)
        if (tapTimestamps.size >= requiredTaps) {
            lastTriggerTime = now
            tapTimestamps.clear()
            onMultiTap()
        }
    }

    fun reset() {
        tapTimestamps.clear()
    }
}
