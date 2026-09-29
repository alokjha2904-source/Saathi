package com.saathi.app

import com.saathi.app.ui.screens.ai.analyzeIncident
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIIncidentAnalysisTest {

    @Test
    fun testStalkingIncidentTriage() {
        val result = analyzeIncident("A strange person has been following me near the hostel")
        assertEquals("Harassment/Stalking", result.category)
        assertEquals("HIGH", result.urgency)
        assertTrue(result.recommendedActions.isNotEmpty())
    }

    @Test
    fun testCriticalMedicalEmergencyTriage() {
        val result = analyzeIncident("My friend fell down stairs, is bleeding heavily and unconscious")
        assertEquals("Medical Emergency", result.category)
        assertEquals("CRITICAL", result.urgency)
        assertTrue(result.recommendedActions.any { it.contains("ambulance", ignoreCase = true) })
    }

    @Test
    fun testFireEmergencyTriage() {
        val result = analyzeIncident("Thick black smoke and fire burning on the third floor")
        assertEquals("Fire Emergency", result.category)
        assertEquals("CRITICAL", result.urgency)
        assertTrue(result.recommendedActions.any { it.contains("evacuate", ignoreCase = true) })
    }

    @Test
    fun testTheftIncidentTriage() {
        val result = analyzeIncident("Someone snatched my purse and stolen my phone at the bus stop")
        assertEquals("Theft/Robbery", result.category)
        assertTrue(result.recommendedActions.any { it.contains("police", ignoreCase = true) })
    }

    @Test
    fun testRoadAccidentTriage() {
        val result = analyzeIncident("Two bikes collided, severe accident on main avenue")
        assertEquals("Road Accident", result.category)
        assertTrue(result.recommendedActions.isNotEmpty())
    }

    @Test
    fun testHindiInputTriage() {
        val result = analyzeIncident("कोई मेरा पीछा कर रहा है")
        assertEquals("Harassment/Stalking", result.category)
        assertTrue(result.recommendedActions.isNotEmpty())
    }
}
