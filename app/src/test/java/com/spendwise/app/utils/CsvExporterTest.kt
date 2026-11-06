package com.spendwise.app.utils

import com.spendwise.app.data.local.entity.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CsvExporterTest {

    private fun day(year: Int, month: Int, dayOfMonth: Int): Long =
        Calendar.getInstance().apply { clear(); set(year, month, dayOfMonth, 12, 0, 0) }.timeInMillis

    private fun expense(title: String, amount: Double, note: String = "") = Expense(
        id = 7, title = title, amount = amount, category = "Food",
        date = day(2026, Calendar.MARCH, 21), note = note, userId = "a@b.c"
    )

    private fun csv(vararg expenses: Expense) = CsvExporter.buildCsv(
        expenses.toList(), "LKR", Date(day(2026, Calendar.NOVEMBER, 6)), Locale.US
    ).lines()

    @Test
    fun `header names the currency`() {
        assertEquals("ID,Title,Amount (LKR),Category,Date,Note", csv()[0])
    }

    @Test
    fun `an expense becomes one quoted row`() {
        assertEquals("7,\"Lunch\",850.0,Food,21 Mar 2026,\"Team\"", csv(expense("Lunch", 850.0, "Team"))[1])
    }

    @Test
    fun `quotes inside text are doubled`() {
        assertEquals("7,\"The \"\"best\"\" tea\",10.0,Food,21 Mar 2026,\"\"", csv(expense("The \"best\" tea", 10.0))[1])
    }

    @Test
    fun `text that looks like a formula is neutralised`() {
        val row = csv(expense("=HYPERLINK(\"http://x\")", 1.0, "+1 note"))[1]
        assertTrue(row, row.startsWith("7,\"'=HYPERLINK("))
        assertTrue(row, row.endsWith(",\"'+1 note\""))
    }

    @Test
    fun `summary counts and sums the expenses`() {
        val lines = csv(expense("A", 100.0), expense("B", 50.5))
        assertTrue(lines.contains("Total Expenses,2"))
        assertTrue(lines.contains("Total Amount,LKR 150.5"))
        assertTrue(lines.contains("Export Date,06 Nov 2026"))
    }

    @Test
    fun `an empty export still has a header and a summary`() {
        val lines = csv()
        assertEquals("ID,Title,Amount (LKR),Category,Date,Note", lines[0])
        assertTrue(lines.contains("Total Expenses,0"))
    }
}
