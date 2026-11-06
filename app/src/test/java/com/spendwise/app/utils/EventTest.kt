package com.spendwise.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventTest {

    @Test
    fun `content is delivered once`() {
        val event = Event("signed in")
        assertEquals("signed in", event.getContentIfNotHandled())
        assertNull(event.getContentIfNotHandled())
    }

    @Test
    fun `peek always returns the content`() {
        val event = Event(42)
        event.getContentIfNotHandled()
        assertEquals(42, event.peekContent())
    }
}
