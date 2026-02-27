package com.example.personalfinanceapp.presentation.learning

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ─── Data models ─────────────────────────────────────────────────────────────

enum class LessonLevel(val label: String, val color: Color) {
    BEGINNER("Kezdő", Color(0xFF10B981)),
    INTERMEDIATE("Haladó", Color(0xFFF59E0B)),
    ADVANCED("Szakértő", Color(0xFF8B5CF6))
}

data class FinanceLesson(
    val id: Int,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val level: LessonLevel,
    val summary: String,
    val keyPoints: List<String>,
    val source: String
)