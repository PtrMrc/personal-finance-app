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

// Logic: Translates AI (English) -> UI (Hungarian)
fun mapToHungarian(englishCategory: String): String {
    return when(englishCategory) {
        "Food" -> "Élelmiszer"
        "Transport" -> "Utazás"
        "Entertainment" -> "Szórakozás"
        "Bills" -> "Számlák"
        "Health" -> "Egészség"
        "Income" -> "Bevétel"
        else -> "Egyéb"
    }
}

// Returns color based on category names
fun getColorForCategory(category: String): Color {
    return when(category) {
        "Food", "Élelmiszer" -> Color(0xFFFF9800)      // Orange
        "Transport", "Utazás" -> Color(0xFF2196F3)     // Blue
        "Entertainment", "Szórakozás" -> Color(0xFF9C27B0) // Purple
        "Bills", "Számlák" -> Color(0xFFF44336)        // Red
        "Health", "Egészség" -> Color(0xFF4CAF50)      // Green
        "Income", "Bevétel" -> Color(0xFF009688)       // Teal
        else -> Color.Gray
    }
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