package com.saathi.app

import com.saathi.app.data.model.EmergencyEvent
import com.saathi.app.data.model.TrustedContact
import com.saathi.app.service.RecordingFile
import com.saathi.app.service.RecordingType
import com.saathi.app.util.LocationResult
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EmergencyFeaturesTest {

    @Test
    fun testTrustedContactLiveLocationDefaultsToTrue() {
        val contact = TrustedContact(
            id = 1,
            name = "Anjali Sharma",
            phone = "+919876543210",
            relationship = "Parent"
        )
        assertTrue("Live location sharing should default to true for safety", contact.receiveLiveLocation)
        assertTrue("SOS notification should default to true", contact.notifyOnSOS)
        assertFalse("Primary should default to false unless explicitly set", contact.isPrimary)
    }

    @Test
    fun testEmergencyEventInitialization() {
        val now = System.currentTimeMillis()
        val event = EmergencyEvent(
            eventId = "SAATHI-7891",
            type = "Harassment/Stalking",
            status = "ACTIVE",
            latitude = 20.3541,
            longitude = 85.8145,
            locationName = "KIIT Campus, Bhubaneswar",
            description = "User requested immediate assistance",
            timestamp = now,
            isDemo = false
        )

        assertEquals("SAATHI-7891", event.eventId)
        assertEquals("ACTIVE", event.status)
        assertEquals(20.3541, event.latitude!!, 0.0001)
        assertEquals(85.8145, event.longitude!!, 0.0001)
        assertFalse(event.isDemo)
        assertNull("Event should not have an end timestamp while active", event.endTimestamp)
    }

    @Test
    fun testRecordingFileDurationAndFormat() {
        val dummyFile = File("sample_audio.m4a")
        val recording = RecordingFile(
            file = dummyFile,
            eventId = "EVT-1001",
            type = RecordingType.AUDIO,
            durationMs = 125_000L, // 2 minutes 5 seconds
            createdAt = System.currentTimeMillis()
        )

        assertEquals(RecordingType.AUDIO, recording.type)
        assertEquals("EVT-1001", recording.eventId)
        val minutes = (recording.durationMs / 1000) / 60
        val seconds = (recording.durationMs / 1000) % 60
        assertEquals(2L, minutes)
        assertEquals(5L, seconds)
    }

    @Test
    fun testLocationResultTelemetryIntegrity() {
        val loc = LocationResult(
            latitude = 28.6139,
            longitude = 77.2090,
            accuracy = 8.5f,
            timestamp = System.currentTimeMillis(),
            isFresh = true,
            provider = "gps"
        )

        assertTrue("Location should be marked fresh when acquired recently", loc.isFresh)
        assertTrue("Accuracy under 15m is high precision", loc.accuracy < 15f)
        assertEquals("gps", loc.provider)
    }
}
