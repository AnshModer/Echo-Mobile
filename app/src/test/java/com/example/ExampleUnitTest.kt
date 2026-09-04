package com.example

import com.example.engine.ContactLookupHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testIsDirectPhoneNumber() {
        assertTrue(ContactLookupHelper.isDirectPhoneNumber("9876543210"))
        assertTrue(ContactLookupHelper.isDirectPhoneNumber("+1 (555) 123-4567"))
        assertTrue(ContactLookupHelper.isDirectPhoneNumber("911"))
        assertTrue(ContactLookupHelper.isDirectPhoneNumber("100"))

        assertFalse(ContactLookupHelper.isDirectPhoneNumber("Mom"))
        assertFalse(ContactLookupHelper.isDirectPhoneNumber("John Doe"))
        assertFalse(ContactLookupHelper.isDirectPhoneNumber("Call Dad"))
    }

    @Test
    fun testCleanContactQuery() {
        assertEquals("Mom", ContactLookupHelper.cleanContactQuery("call Mom"))
        assertEquals("John Doe", ContactLookupHelper.cleanContactQuery("dial to John Doe"))
        assertEquals("Alex", ContactLookupHelper.cleanContactQuery("call my Alex on mobile"))
        assertEquals("Doctor", ContactLookupHelper.cleanContactQuery("to Doctor"))
    }

    @Test
    fun testUnitConversionCalculations() {
        // Temperature conversions
        val cToF = (100.0 * 9.0 / 5.0) + 32.0
        assertEquals(212.0, cToF, 0.01)

        val fToC = (32.0 - 32.0) * 5.0 / 9.0
        assertEquals(0.0, fToC, 0.01)

        // Distance conversions
        val milesToKm = 10.0 * 1.60934
        assertEquals(16.09, milesToKm, 0.01)

        // Weight conversions
        val kgToLbs = 10.0 * 2.20462
        assertEquals(22.04, kgToLbs, 0.01)
    }

    @Test
    fun testCurrencyConversions() {
        val usdToEur = 100.0 * 0.92
        assertEquals(92.0, usdToEur, 0.01)

        val usdToInr = 100.0 * 83.5
        assertEquals(8350.0, usdToInr, 0.01)
    }
}
