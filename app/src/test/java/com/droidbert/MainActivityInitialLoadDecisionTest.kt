package com.droidbert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityInitialLoadDecisionTest {

    @Test
    fun `uses first historical comic on first launch when no date is stored`() {
        val request = MainActivity.resolveInitialComicRequest(null)

        assertEquals("1989-04-16", request.date)
        assertTrue(request.fallbackToLatestIfMissing)
    }

    @Test
    fun `uses first historical comic when stored date is blank`() {
        val request = MainActivity.resolveInitialComicRequest("   ")

        assertEquals("1989-04-16", request.date)
        assertTrue(request.fallbackToLatestIfMissing)
    }

    @Test
    fun `resumes last viewed comic when stored date exists`() {
        val request = MainActivity.resolveInitialComicRequest("1992-01-15")

        assertEquals("1992-01-15", request.date)
        assertFalse(request.fallbackToLatestIfMissing)
    }
}
