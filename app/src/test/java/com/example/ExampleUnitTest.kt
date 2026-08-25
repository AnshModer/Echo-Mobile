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
}
