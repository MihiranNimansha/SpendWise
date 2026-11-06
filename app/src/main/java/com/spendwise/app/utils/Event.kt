package com.spendwise.app.utils

/**
 * Wraps a value that should be handled only once (a navigation trigger, a one-off message...).
 *
 * A plain LiveData replays its last value to every new observer. The auth result is shared by the
 * Register and Login screens through the activity ViewModel, so a successful registration used to
 * be replayed into the freshly created Login screen, which then "signed in" without a password.
 */
open class Event<out T>(private val content: T) {

    private var handled = false

    /** Returns the content the first time it is asked for, and null afterwards. */
    fun getContentIfNotHandled(): T? =
        if (handled) null else {
            handled = true
            content
        }

    /** Returns the content even if it was already handled. */
    fun peekContent(): T = content
}
