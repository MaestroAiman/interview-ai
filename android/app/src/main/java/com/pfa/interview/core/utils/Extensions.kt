package com.pfa.interview.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun String.toDisplayDate(): String {
    return try {
        val inputSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputSdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputSdf.parse(this)
        if (date != null) outputSdf.format(date) else this
    } catch (e: Exception) {
        this
    }
}

fun Float.toScoreLabel(): String = when {
    this >= 8.5f -> "Excellent!"
    this >= 7f -> "Good job!"
    this >= 5f -> "Keep going!"
    else -> "Keep practicing"
}

fun Float.toScoreColor(
    excellent: androidx.compose.ui.graphics.Color,
    good: androidx.compose.ui.graphics.Color,
    average: androidx.compose.ui.graphics.Color,
    poor: androidx.compose.ui.graphics.Color
): androidx.compose.ui.graphics.Color = when {
    this >= 7f -> excellent
    this >= 5f -> good
    this >= 3f -> average
    else -> poor
}
