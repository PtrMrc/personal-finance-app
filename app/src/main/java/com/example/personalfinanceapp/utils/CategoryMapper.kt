package com.example.personalfinanceapp.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryMapper {
    fun getColor(category: String): Color {
        return when (category.trim().lowercase()) {
            "élelmiszer" -> Color(0xFFEF4444)
            "utazás" -> Color(0xFF3B82F6)
            "szórakozás" -> Color(0xFF8B5CF6)
            "számlák" -> Color(0xFFF59E0B)
            "egészség" -> Color(0xFF10B981)
            "bevétel" -> Color(0xFF10B981)
            else -> Color(0xFF64748B) // Egyéb
        }
    }

    fun getIcon(category: String): ImageVector {
        return when (category.trim().lowercase()) {
            "élelmiszer" -> Icons.Default.Restaurant
            "utazás" -> Icons.Default.DirectionsCar
            "szórakozás" -> Icons.Default.MovieCreation
            "számlák" -> Icons.Default.Receipt
            "egészség" -> Icons.Default.Favorite
            "bevétel" -> Icons.AutoMirrored.Filled.TrendingUp
            else -> Icons.Default.Category // Egyéb
        }
    }
}