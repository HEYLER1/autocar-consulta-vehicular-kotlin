package com.example.app_autocar

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun normalizesPeruvianPlate() {
        assertEquals("XD4345", PlateUtils.normalize("xd4345"))
        assertEquals("XD4345", PlateUtils.normalize("xd4-345"))
        assertEquals("ABC123", PlateUtils.normalize("abc-123"))
        assertEquals("ABC-123", PlateUtils.display("abc123"))
    }

    @Test
    fun validatesPeruvianPlate() {
        assertTrue(PlateUtils.isValidPeruvianPlate("ABC-123"))
        assertTrue(PlateUtils.isValidPeruvianPlate("A1B-234"))
        assertFalse(PlateUtils.isValidPeruvianPlate("ABC12"))
        assertFalse(PlateUtils.isValidPeruvianPlate("ABC-1234"))
    }
}
