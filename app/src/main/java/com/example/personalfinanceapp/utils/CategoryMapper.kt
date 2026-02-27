package com.example.personalfinanceapp.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryMapper {
    fun getColor(category: String): Color {
        return when (category.trim().lowercase()) {
            "élelmiszer", "étel", "food" -> Color(0xFFEF4444)
            "utazás", "közlekedés", "transport" -> Color(0xFF3B82F6)
            "szórakozás", "entertainment" -> Color(0xFF8B5CF6)
            "számlák", "lakhatás", "housing" -> Color(0xFFF59E0B)
            "egészség", "health" -> Color(0xFF10B981)
            "oktatás", "education" -> Color(0xFF6366F1)
            "ruházat", "clothing" -> Color(0xFFEC4899)
            "bevétel", "income" -> Color(0xFF10B981)
            else -> Color(0xFF64748B)
        }
    }

    fun getIcon(category: String): ImageVector {
        return when (category.trim().lowercase()) {
            "élelmiszer", "étel", "food" -> Icons.Default.Restaurant
            "utazás", "közlekedés", "transport" -> Icons.Default.DirectionsCar
            "szórakozás", "entertainment" -> Icons.Default.MovieCreation
            "számlák", "lakhatás", "housing" -> Icons.Default.Receipt
            "egészség", "health" -> Icons.Default.Favorite
            "oktatás", "education" -> Icons.Default.School
            "ruházat", "clothing" -> Icons.Default.Checkroom
            "bevétel", "income" -> Icons.AutoMirrored.Filled.TrendingUp
            else -> Icons.Default.Category
        }
    }
}