package com.saathi.app.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_events")
data class EmergencyEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: String = "",
    val type: String = "SOS", // SOS, CHECK_IN_MISSED, TIMER_EXPIRED
    val status: String = "ACTIVE", // ACTIVE, CANCELLED, RESOLVED, TEST
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val endTimestamp: Long? = null,
    val description: String? = null,
    val isDemo: Boolean = false
) {
    @get:Ignore
    val locationString: String
        get() = locationName ?: if (latitude != null && longitude != null) "$latitude, $longitude" else "Location unavailable"

    @get:Ignore
    val durationMinutes: Long
        get() = if (endTimestamp != null && endTimestamp > timestamp) (endTimestamp - timestamp) / 60000 else 0
}
