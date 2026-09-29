package com.saathi.app

import com.saathi.app.service.EmergencyDispatcher
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyDispatcherTest {

    @Test
    fun testExactMessageFormatWithCoordinates() {
        val message = EmergencyDispatcher.buildEmergencyMessage(
            userName = "Alok Sharma",
            userPhone = "+91 9876543210",
            latitude = 28.6139,
            longitude = 77.2090,
            accuracyMeters = 12.5f,
            timestamp = 1700000000000L,
            incidentType = "SOS activated",
            eventId = "ABC12345"
        )

        assertTrue("Message must start with SAATHI EMERGENCY ALERT", message.contains("SAATHI EMERGENCY ALERT"))
        assertTrue("Message must contain user's name", message.contains("Name: Alok Sharma"))
        assertTrue("Message must contain user's phone", message.contains("Phone: +91 9876543210"))
        assertTrue("Message must contain help text", message.contains("I may be in danger and need immediate help."))
        assertTrue("Message must contain exact Google Maps URL", message.contains("https://maps.google.com/?q=28.6139,77.209"))
        assertTrue("Message must contain accuracy in meters", message.contains("12 meters"))
        assertTrue("Message must contain incident type", message.contains("Incident: SOS activated"))
        assertTrue("Message must contain event ID", message.contains("SAATHI Event ID: ABC12345"))
        assertTrue("Message must end with contact request", message.contains("Please contact me immediately."))
    }

    @Test
    fun testMessageFormatWhenLocationPending() {
        val message = EmergencyDispatcher.buildEmergencyMessage(
            userName = "",
            userPhone = "",
            latitude = null,
            longitude = null,
            accuracyMeters = null,
            timestamp = 1700000000000L,
            incidentType = "",
            eventId = "XYZ99999"
        )

        assertTrue("Fallback user name must be used", message.contains("Name: User"))
        assertTrue("Fallback phone must be used", message.contains("Phone: Not set"))
        assertTrue("Pending location must be noted", message.contains("GPS location acquisition pending"))
        assertTrue("Default incident must be SOS activated", message.contains("Incident: SOS activated"))
        assertTrue("Event ID must be present", message.contains("SAATHI Event ID: XYZ99999"))
    }
}
