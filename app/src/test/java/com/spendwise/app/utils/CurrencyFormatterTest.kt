package com.spendwise.app.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CurrencyFormatterTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US) // number formatting depends on the device locale
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `symbols for the supported currencies`() {
        assertEquals("Rs.", CurrencyFormatter.getSymbol("LKR"))
        assertEquals("$", CurrencyFormatter.getSymbol("USD"))
        assertEquals("€", CurrencyFormatter.getSymbol("EUR"))
        assertEquals("£", CurrencyFormatter.getSymbol("GBP"))
    }

    @Test
    fun `currency codes are case-insensitive and unknown codes are shown as-is`() {
        assertEquals("$", CurrencyFormatter.getSymbol("usd"))
        assertEquals("JPY", CurrencyFormatter.getSymbol("JPY"))
    }

    @Test
    fun `format groups thousands and keeps two decimals`() {
        assertEquals("Rs. 1,234.50", CurrencyFormatter.format(1234.5, "LKR"))
        assertEquals("$ 0.00", CurrencyFormatter.format(0.0, "USD"))
    }

    @Test
    fun `formatShort abbreviates thousands and millions`() {
        assertEquals("Rs. 999.50", CurrencyFormatter.formatShort(999.5, "LKR"))
        assertEquals("Rs. 1.5K", CurrencyFormatter.formatShort(1500.0, "LKR"))
        assertEquals("$ 2.5M", CurrencyFormatter.formatShort(2_500_000.0, "USD"))
    }

    @Test
    fun `parse strips the symbol and separators`() {
        assertEquals(1234.5, CurrencyFormatter.parse("Rs. 1,234.50"), 0.0)
        assertEquals(0.0, CurrencyFormatter.parse("abc"), 0.0)
    }
}
