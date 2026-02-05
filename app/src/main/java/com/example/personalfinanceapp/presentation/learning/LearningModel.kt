package com.example.personalfinanceapp.presentation.learning

enum class LessonLevel { BEGINNER, INTERMEDIATE, ADVANCED }

data class FinanceLesson(
    val id: Int,
    val title: String,
    val duration: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val level: LessonLevel
)