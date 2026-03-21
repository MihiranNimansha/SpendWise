package com.spendwise.app.utils

/**
 * App-wide constants for SpendWise
 */
object Constants {

    // App Info
    const val DB_NAME = "spendwise_database"
    const val PREFS_NAME = "spendwise_preferences"

    // Budget
    const val PANIC_THRESHOLD = 0.90

    // Expense Categories
    val CATEGORIES = listOf(
        "Food",
        "Transport",
        "Fun",
        "Health",
        "Bills",
        "Shopping",
        "Other"
    )

    // Supported Currencies
    val CURRENCIES = listOf(
        "LKR",
        "USD",
        "EUR",
        "GBP"
    )

    // SharedPreferences Keys
    const val KEY_USER_NAME = "pref_user_name"
    const val KEY_USER_EMAIL = "pref_user_email"
    const val KEY_CURRENCY = "pref_currency"
    const val KEY_DARK_MODE = "pref_dark_mode"
    const val KEY_MONTHLY_BUDGET = "pref_monthly_budget"
    const val KEY_ONBOARDING_SHOWN = "pref_onboarding_shown"
    const val KEY_NOTIFICATIONS_ENABLED = "pref_notifications_enabled"
    const val KEY_IS_LOGGED_IN = "pref_is_logged_in"
    const val KEY_LAST_LOGIN = "pref_last_login"

    // Default Values
    const val DEFAULT_CURRENCY = "LKR"
    const val DEFAULT_MONTHLY_BUDGET = 50000.0
}
