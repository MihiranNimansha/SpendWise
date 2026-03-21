package com.spendwise.app.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

// ----- View Visibility Extensions -----

/**
 * Make a view visible
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * Make a view gone (no space)
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * Make a view invisible (preserves space)
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

// ----- Context Extensions -----

/**
 * Show a short Toast from Context
 */
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Show a long Toast from Context
 */
fun Context.toastLong(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// ----- View Snackbar Extensions -----

/**
 * Show a simple Snackbar on any View
 */
fun View.snackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

/**
 * Show a Snackbar with an action button
 */
fun View.snackbarWithAction(
    message: String,
    actionLabel: String,
    duration: Int = Snackbar.LENGTH_LONG,
    action: () -> Unit
) {
    Snackbar.make(this, message, duration)
        .setAction(actionLabel) { action() }
        .show()
}

// ----- Date Formatting Extensions (Long timestamps) -----

/**
 * Format a Unix timestamp (milliseconds) to a date string: "dd MMM yyyy"
 */
fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * Format a Unix timestamp (milliseconds) to date + time: "dd MMM yyyy, HH:mm"
 */
fun Long.toFormattedDateTime(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

// ----- Date Range Utilities -----

/**
 * Get the start of the current month as a Unix timestamp (midnight, 1st of month)
 */
fun getCurrentMonthStart(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/**
 * Get the end of the current month as a Unix timestamp (last millisecond of last day)
 */
fun getCurrentMonthEnd(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}

/**
 * Get the start of the previous month
 */
fun getLastMonthStart(): Long {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, -1)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/**
 * Get the end of the previous month
 */
fun getLastMonthEnd(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.add(Calendar.DAY_OF_MONTH, -1)
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}

/**
 * Format a Double to 2-decimal-place string without symbol
 */
fun Double.toFormattedAmount(): String = String.format("%,.2f", this)
