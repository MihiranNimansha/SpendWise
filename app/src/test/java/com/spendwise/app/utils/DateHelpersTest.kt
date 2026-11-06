package com.spendwise.app.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone

class DateHelpersTest {

    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    private fun millis(zone: String, y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0, s: Int = 0, nano: Int = 0) =
        ZonedDateTime.of(y, m, d, h, min, s, nano, ZoneId.of(zone)).toInstant().toEpochMilli()

    @Test
    fun `month boundaries of a 28 day February`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Colombo"))
        val middle = millis("Asia/Colombo", 2026, 2, 15, 10)
        assertEquals(millis("Asia/Colombo", 2026, 2, 1), monthStartOf(middle))
        assertEquals(millis("Asia/Colombo", 2026, 2, 28, 23, 59, 59, 999_000_000), monthEndOf(middle))
    }

    @Test
    fun `leap year February ends on the 29th`() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val end = monthEndOf(millis("UTC", 2028, 2, 3))
        assertEquals(millis("UTC", 2028, 2, 29, 23, 59, 59, 999_000_000), end)
    }

    @Test
    fun `december rolls into the right end of month`() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val end = monthEndOf(millis("UTC", 2026, 12, 31, 23, 59))
        assertEquals(millis("UTC", 2026, 12, 31, 23, 59, 59, 999_000_000), end)
    }

    @Test
    fun `an expense later in the month is inside the month range`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Colombo"))
        val now = millis("Asia/Colombo", 2026, 11, 6, 9)
        val laterThisMonth = millis("Asia/Colombo", 2026, 11, 20, 18)
        assert(laterThisMonth in monthStartOf(now)..monthEndOf(now))
    }

    @Test
    fun `date picker selection for a local evening in a UTC-minus zone is still that day`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        // 20:00 on 21 Mar in Los Angeles is already 22 Mar in UTC
        val local = millis("America/Los_Angeles", 2026, 3, 21, 20)
        assertEquals(Instant.parse("2026-03-21T00:00:00Z").toEpochMilli(), localMillisToDatePickerSelection(local))
    }

    @Test
    fun `date picker selection for a local morning in a UTC-plus zone is still that day`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Colombo"))
        // 01:00 on 21 Mar in Colombo is still 20 Mar in UTC
        val local = millis("Asia/Colombo", 2026, 3, 21, 1)
        assertEquals(Instant.parse("2026-03-21T00:00:00Z").toEpochMilli(), localMillisToDatePickerSelection(local))
    }

    @Test
    fun `picked day keeps the time of day of the previous value`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        val picked = Instant.parse("2026-03-21T00:00:00Z").toEpochMilli() // what the picker returns
        val before = millis("America/Los_Angeles", 2026, 3, 1, 15, 30, 45)
        assertEquals(
            millis("America/Los_Angeles", 2026, 3, 21, 15, 30, 45),
            datePickerSelectionToLocalMillis(picked, before)
        )
    }
}
