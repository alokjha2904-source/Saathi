package com.saathi.app

import com.saathi.app.util.LocationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationHelperTest {

    @Test
    fun testValidCoordinates() {
        assertTrue(LocationHelper.isValidCoordinate(28.6139, 77.2090)) // New Delhi
        assertTrue(LocationHelper.isValidCoordinate(19.0760, 72.8777)) // Mumbai
        assertTrue(LocationHelper.isValidCoordinate(-33.8688, 151.2093)) // Sydney
        assertTrue(LocationHelper.isValidCoordinate(0.1, 0.1))
    }

    @Test
    fun testInvalidCoordinates() {
        assertFalse(LocationHelper.isValidCoordinate(null, 77.2090))
        assertFalse(LocationHelper.isValidCoordinate(28.6139, null))
        assertFalse(LocationHelper.isValidCoordinate(null, null))
        assertFalse(LocationHelper.isValidCoordinate(0.0, 0.0)) // Null Island rejection
        assertFalse(LocationHelper.isValidCoordinate(95.0, 77.0)) // Latitude > 90
        assertFalse(LocationHelper.isValidCoordinate(-91.0, 77.0)) // Latitude < -90
        assertFalse(LocationHelper.isValidCoordinate(28.0, 185.0)) // Longitude > 180
        assertFalse(LocationHelper.isValidCoordinate(28.0, -185.0)) // Longitude < -180
        assertFalse(LocationHelper.isValidCoordinate(Double.NaN, 77.0))
    }
}
