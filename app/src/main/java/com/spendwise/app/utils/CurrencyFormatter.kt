package com.spendwise.app.utils

/**
 * Utility object for formatting currency amounts
 */
object CurrencyFormatter {

    /**
     * Format an amount with the appropriate currency symbol
     * @param amount the numeric value
     * @param currency ISO currency code (LKR, USD, EUR, GBP)
     * @return formatted string with symbol and 2 decimal places
     */
    fun format(amount: Double, currency: String): String {
        val symbol = getSymbol(currency)
        return "$symbol ${String.format("%,.2f", amount)}"
    }

    /**
     * Format an amount in short form — thousands as K, millions as M
     * @param amount the numeric value
     * @param currency ISO currency code
     * @return abbreviated formatted string
     */
    fun formatShort(amount: Double, currency: String): String {
        val symbol = getSymbol(currency)
        return when {
            amount >= 1_000_000 -> "$symbol ${String.format("%.1fM", amount / 1_000_000)}"
            amount >= 1_000 -> "$symbol ${String.format("%.1fK", amount / 1_000)}"
            else -> "$symbol ${String.format("%.2f", amount)}"
        }
    }

    /**
     * Returns symbol for a given currency code
     */
    fun getSymbol(currency: String): String {
        return when (currency.uppercase()) {
            "LKR" -> "Rs."
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> currency
        }
    }

    /**
     * Parse a formatted currency string back to Double (removes symbol/commas)
     */
    fun parse(formatted: String): Double {
        // The "Rs." symbol leaves a stray leading dot once the letters are stripped; drop it
        val cleaned = formatted.replace(Regex("[^0-9.]"), "").trimStart('.')
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}
