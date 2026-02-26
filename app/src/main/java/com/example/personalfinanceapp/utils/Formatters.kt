package com.example.personalfinanceapp.utils

import androidx.compose.ui.graphics.Color
import com.example.personalfinanceapp.data.Frequency
import java.text.NumberFormat
import java.util.Locale

// Extracts the first number found in a string (e.g. "Pizza 3000" -> 3000.0)
fun extractAmount(text: String): Double {
    val regex = Regex("[0-9]+")
    val match = regex.find(text)
    return match?.value?.toDouble() ?: 0.0
}

// Helper: Formats 12345.0 -> "12 345"
fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("hu-HU"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

// Helper: Translates Frequency enum to Hungarian
fun mapFrequency(frequency: Frequency): String {
    return when(frequency) {
        Frequency.MONTHLY -> "Havonta"
        Frequency.QUARTERLY -> "Negyedévente"
        Frequency.YEARLY -> "Évente"
    }
}

/**
 * Formats a Unix timestamp into a human-readable relative date string.
 * Returns "Ma", "Tegnap", "$n napja", or a short month-day string for older dates.
 */
fun formatDate(timestamp: Long): String {
    val days = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24)
    return when {
        days == 0L -> "Ma"
        days == 1L -> "Tegnap"
        days < 7   -> "$days napja"
        else       -> java.text.SimpleDateFormat("MMM dd", java.util.Locale("hu"))
            .format(java.util.Date(timestamp))
    }
}