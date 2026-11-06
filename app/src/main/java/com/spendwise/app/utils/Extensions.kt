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
 * First millisecond (00:00:00.000 on the 1st) of the month that contains [timeMillis].
 */
fun monthStartOf(timeMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/**
 * Last millisecond (23:59:59.999 on the last day) of the month that contains [timeMillis].
 */
fun monthEndOf(timeMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}

/**
 * MaterialDatePicker works in UTC: it expects and returns "midnight UTC of the chosen day".
 * Turns a local timestamp into the value to pass to setSelection() so the picker highlights the
 * same calendar day the user sees (without this, UTC-minus zones show the previous day).
 */
fun localMillisToDatePickerSelection(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.clear()
    utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    return utc.timeInMillis
}

/**
 * Turns the value returned by MaterialDatePicker (midnight UTC of the chosen day) into a local
 * timestamp on that same calendar day, keeping the time of day of [keepTimeOfDayFrom] so several
 * expenses on one day still sort in the order they were entered.
 */
fun datePickerSelectionToLocalMillis(selection: Long, keepTimeOfDayFrom: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection }
    val time = Calendar.getInstance().apply { timeInMillis = keepTimeOfDayFrom }
    val result = Calendar.getInstance()
    result.clear()
    result.set(
        utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH),
        time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), time.get(Calendar.SECOND)
    )
    return result.timeInMillis
}

/**
 * Get the start of the current month as a Unix timestamp (midnight, 1st of month)
 */
fun getCurrentMonthStart(): Long = monthStartOf(System.currentTimeMillis())

/**
 * Get the end of the current month as a Unix timestamp (last millisecond of last day)
 */
fun getCurrentMonthEnd(): Long = monthEndOf(System.currentTimeMillis())

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
