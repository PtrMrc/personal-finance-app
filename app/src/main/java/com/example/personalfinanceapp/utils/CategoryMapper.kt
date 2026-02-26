package com.example.personalfinanceapp.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Single source of truth for category display logic.
 *
 * The app stores categories in Hungarian (e.g. "Étel"), but the ML model
 * predicts in English (e.g. "Food"). Both forms are handled here so callers
 * don't need to care which language a category string came from.
 */
object CategoryMapper {

    /**
     * Returns the accent color used for a category's icon and chip.
     * Handles both Hungarian and English category names case-insensitively.
     */
    fun getColor(category: String): Color = when (category.lowercase()) {
        "étel", "food"                   -> Color(0xFFEF4444)
        "közlekedés", "transport"         -> Color(0xFF3B82F6)
        "szórakozás", "entertainment"     -> Color(0xFF8B5CF6)
        "lakhatás", "housing"             -> Color(0xFFF59E0B)
        "egészség", "health"              -> Color(0xFF10B981)
        "oktatás", "education"            -> Color(0xFF6366F1)
        "ruházat", "clothing"             -> Color(0xFFEC4899)
        "bevétel", "income"              -> Color(0xFF10B981)
        else                             -> Color(0xFF64748B)
    }

    /**
     * Returns the icon used for a category throughout the app.
     * Handles both Hungarian and English category names case-insensitively.
     */
    fun getIcon(category: String): ImageVector = when (category.lowercase()) {
        "étel", "food"                   -> Icons.Default.Restaurant
        "közlekedés", "transport"         -> Icons.Default.DirectionsCar
        "szórakozás", "entertainment"     -> Icons.Default.MovieCreation
        "lakhatás", "housing"             -> Icons.Default.Home
        "egészség", "health"             -> Icons.Default.Favorite
        "oktatás", "education"            -> Icons.Default.School
        "ruházat", "clothing"             -> Icons.Default.Checkroom
        "bevétel", "income"              -> Icons.AutoMirrored.Filled.TrendingUp
        else                             -> Icons.Default.Category
    }

    /**
     * Translates an English category name (as returned by the ML model)
     * into its Hungarian display equivalent.
     */
    fun toHungarian(englishCategory: String): String = when (englishCategory) {
        "Food"          -> "Étel"
        "Transport"     -> "Közlekedés"
        "Entertainment" -> "Szórakozás"
        "Housing"       -> "Lakhatás"
        "Bills"         -> "Számlák"
        "Health"        -> "Egészség"
        "Education"     -> "Oktatás"
        "Clothing"      -> "Ruházat"
        "Income"        -> "Bevétel"
        else            -> "Egyéb"
    }
}