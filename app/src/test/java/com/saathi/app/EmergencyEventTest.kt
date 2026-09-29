package com.saathi.app

import com.saathi.app.data.model.EmergencyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EmergencyEventTest {

    @Test
    fun testDurationMinutesCalculation() {
        val start = 1700000000000L
        val end = start + (25 * 60 * 1000L) // 25 minutes later

        val event = EmergencyEvent(
            eventId = "TEST001",
            type = "SOS",
            status = "RESOLVED",
            timestamp = start,
            endTimestamp = end
        )

        assertEquals(25L, event.durationMinutes)
    }

    @Test
    fun testLocationStringWithCoordinates() {
        val event = EmergencyEvent(
            eventId = "TEST002",
            type = "SOS",
            latitude = 20.3541,
            longitude = 85.8145
        )

        assertEquals("20.3541, 85.8145", event.locationString)
    }

    @Test
    fun testLocationStringWithNamePriority() {
        val event = EmergencyEvent(
            eventId = "TEST003",
            type = "SOS",
            latitude = 20.3541,
            longitude = 85.8145,
            locationName = "KIIT Campus Gate 1"
        )

        assertEquals("KIIT Campus Gate 1", event.locationString)
    }

    @Test
    fun testDefaultValues() {
        val event = EmergencyEvent()

        assertEquals("SOS", event.type)
        assertEquals("ACTIVE", event.status)
        assertNotNull(event.timestamp)
    }
}
