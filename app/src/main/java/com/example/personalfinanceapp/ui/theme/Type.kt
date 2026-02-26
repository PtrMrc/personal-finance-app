package com.example.personalfinanceapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Using the system default font family. To use a custom font (e.g. Inter),
// replace FontFamily.Default below with a FontFamily built from downloaded font assets.
private val AppFont = FontFamily.Default

val Typography = Typography(
    // ── Display ───────────────────────────────────────────────────────────────
    // Used for the large balance figure on the home card
    displaySmall = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.Bold,
        fontSize     = 36.sp,
        lineHeight   = 44.sp,
        letterSpacing = (-0.5).sp
    ),

    // ── Headlines ─────────────────────────────────────────────────────────────
    // Screen titles (e.g. "Áttekintés", "Pénzügyi Áttekintés")
    headlineLarge = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.Bold,
        fontSize     = 30.sp,
        lineHeight   = 38.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.Bold,
        fontSize     = 26.sp,
        lineHeight   = 34.sp,
        letterSpacing = (-0.25).sp
    ),

    // ── Titles ────────────────────────────────────────────────────────────────
    // Section headers inside screens (e.g. "Legutóbbi tranzakciók")
    titleLarge = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 20.sp,
        lineHeight   = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.Medium,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ── Body ──────────────────────────────────────────────────────────────────
    // Transaction titles, dialog fields, general readable text
    bodyLarge = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.Normal,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // ── Labels ────────────────────────────────────────────────────────────────
    // Category pills, stat subtitles, date chips, badge text
    labelLarge = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.Medium,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.Medium,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily   = AppFont,
        fontWeight   = FontWeight.Medium,
        fontSize     = 10.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.5.sp
    )
)
