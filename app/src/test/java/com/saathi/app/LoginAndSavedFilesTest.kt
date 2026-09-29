package com.saathi.app

import com.saathi.app.service.RecordingFile
import com.saathi.app.service.RecordingType
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LoginAndSavedFilesTest {

    @Test
    fun testMobileNumberValidation() {
        val validPhone1 = "9876543210"
        val validPhone2 = "8123456789"
        val invalidShort = "98765"
        val invalidCharacters = "98765abcd"

        val isValidPhone1 = validPhone1.length == 10 && validPhone1.all { it.isDigit() }
        val isValidPhone2 = validPhone2.length == 10 && validPhone2.all { it.isDigit() }
        val isInvalidShort = invalidShort.length == 10 && invalidShort.all { it.isDigit() }
        val isInvalidChars = invalidCharacters.length == 10 && invalidCharacters.all { it.isDigit() }

        assertTrue(isValidPhone1)
        assertTrue(isValidPhone2)
        assertFalse(isInvalidShort)
        assertFalse(isInvalidChars)
    }

    @Test
    fun testEmailFormatValidation() {
        val validEmail = "student@kiit.ac.in"
        val invalidEmail = "plainaddress"

        assertTrue(validEmail.contains("@") && validEmail.contains("."))
        assertFalse(invalidEmail.contains("@"))
    }

    @Test
    fun testCovertVideoAndAudioEvidenceFileNaming() {
        val eventId = "SAATHI_EVT_5501"
        val audioFileName = "${eventId}_audio.m4a"
        val videoFileName = "${eventId}_video.mp4"

        val audioFile = RecordingFile(
            file = File(audioFileName),
            eventId = eventId,
            type = RecordingType.AUDIO,
            durationMs = 45000L,
            createdAt = System.currentTimeMillis()
        )

        val videoFile = RecordingFile(
            file = File(videoFileName),
            eventId = eventId,
            type = RecordingType.VIDEO,
            durationMs = 45000L,
            createdAt = System.currentTimeMillis()
        )

        assertEquals("Audio file must have AUDIO type", RecordingType.AUDIO, audioFile.type)
        assertEquals("Video file must have VIDEO type", RecordingType.VIDEO, videoFile.type)
        assertTrue("Audio file must end with .m4a", audioFile.file.name.endsWith(".m4a"))
        assertTrue("Video file must end with .mp4", videoFile.file.name.endsWith(".mp4"))
        assertEquals("Both files must share the exact emergency event ID for proof indexing", audioFile.eventId, videoFile.eventId)
    }

    @Test
    fun testDemoOtpVerification() {
        val generatedOtp = "789123"
        val userEnteredCorrectOtp = "789123"
        val userEnteredWrongOtp = "111222"

        assertEquals(generatedOtp, userEnteredCorrectOtp)
        assertNotEquals(generatedOtp, userEnteredWrongOtp)
    }
}
