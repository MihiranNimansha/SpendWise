package com.spendwise.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryStylesTest {

    private val hexColor = Regex("#[0-9A-Fa-f]{6}")

    @Test
    fun `every category has an emoji and valid colours`() {
        Constants.CATEGORIES.forEach { category ->
            val style = CategoryStyles.of(category)
            assertTrue("$category emoji", style.emoji.isNotEmpty())
            assertTrue("$category pastel", hexColor.matches(style.pastelHex))
            assertTrue("$category chart", hexColor.matches(style.chartHex))
        }
    }

    @Test
    fun `every category has its own chart colour`() {
        val colours = Constants.CATEGORIES.map { CategoryStyles.of(it).chartHex }
        assertEquals(colours.size, colours.toSet().size)
    }

    @Test
    fun `unknown or missing categories fall back to Other`() {
        assertEquals(CategoryStyles.of("Other"), CategoryStyles.of("Gifts"))
        assertEquals(CategoryStyles.of("Other"), CategoryStyles.of(null))
    }
}
