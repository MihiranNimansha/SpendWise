package com.spendwise.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.spendwise.app.utils.Constants

/**
 * SharedPreferences wrapper exposing all user settings as readable/writable properties.
 * @param context Application context
 */
class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    /** User's display name */
    var userName: String
        get() = prefs.getString(Constants.KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_USER_NAME, value).apply()

    /** User's email address */
    var userEmail: String
        get() = prefs.getString(Constants.KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_USER_EMAIL, value).apply()

    /** Preferred currency code (default: LKR) */
    var preferredCurrency: String
        get() = prefs.getString(Constants.KEY_CURRENCY, Constants.DEFAULT_CURRENCY) ?: Constants.DEFAULT_CURRENCY
        set(value) = prefs.edit().putString(Constants.KEY_CURRENCY, value).apply()

    /** Whether dark mode is enabled */
    var isDarkMode: Boolean
        get() = prefs.getBoolean(Constants.KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_DARK_MODE, value).apply()

    /** Monthly budget limit (default: 50000.0) */
    var monthlyBudget: Double
        get() = prefs.getFloat(Constants.KEY_MONTHLY_BUDGET, Constants.DEFAULT_MONTHLY_BUDGET.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(Constants.KEY_MONTHLY_BUDGET, value.toFloat()).apply()

    /** Whether the onboarding flow has already been shown */
    var isOnboardingShown: Boolean
        get() = prefs.getBoolean(Constants.KEY_ONBOARDING_SHOWN, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_ONBOARDING_SHOWN, value).apply()

    /** Whether push notifications are enabled */
    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, value).apply()

    /** Whether the user is currently logged in */
    var isLoggedIn: Boolean
        get() = prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_IS_LOGGED_IN, value).apply()

    /** Timestamp of the last successful login */
    var lastLoginTime: Long
        get() = prefs.getLong(Constants.KEY_LAST_LOGIN, 0L)
        set(value) = prefs.edit().putLong(Constants.KEY_LAST_LOGIN, value).apply()

    /**
     * Clear all preferences — call on sign-out
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
