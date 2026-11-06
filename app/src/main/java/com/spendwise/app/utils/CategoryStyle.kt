package com.spendwise.app.utils

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * How a spending category looks everywhere in the app: its emoji, the soft background colour
 * used behind it in lists, and the colour used for its slice in the analytics chart.
 *
 * This used to be copy-pasted (with slightly different colours) into the dashboard, history and
 * goals screens, and the chart handed out colours by position instead of by category.
 */
data class CategoryStyle(
    val emoji: String,
    val pastelHex: String,
    val chartHex: String
) {
    @get:ColorInt
    val pastelColor: Int get() = Color.parseColor(pastelHex)

    @get:ColorInt
    val chartColor: Int get() = Color.parseColor(chartHex)
}

object CategoryStyles {

    private val styles = mapOf(
        "Food" to CategoryStyle("🍔", "#FFF3E0", "#4CAF50"),
        "Transport" to CategoryStyle("🚗", "#E3F2FD", "#2196F3"),
        "Fun" to CategoryStyle("🎉", "#F3E5F5", "#FF9800"),
        "Health" to CategoryStyle("💊", "#E8F5E9", "#F44336"),
        "Bills" to CategoryStyle("⚡", "#FFF8E1", "#9C27B0"),
        "Shopping" to CategoryStyle("🛍️", "#FCE4EC", "#E91E63"),
        "Other" to CategoryStyle("💰", "#EFEBE9", "#607D8B")
    )

    private val fallback = styles.getValue("Other")

    /** Style for [category]; unknown categories get the "Other" look. */
    fun of(category: String?): CategoryStyle = styles[category] ?: fallback
}
