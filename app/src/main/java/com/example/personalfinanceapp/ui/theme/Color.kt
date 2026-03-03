package com.example.personalfinanceapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand / Primary — Blue accent on dark teal-slate background ───────────────
// Blue keeps clear separation from the green used for income/positive numbers
val BrandBlue          = Color(0xFF3B82F6)   // Primary blue — vibrant on dark teal bg
val BrandBlueDim       = Color(0xFF1D4ED8)   // Darker blue — primaryContainer in dark
val BrandBlueLight     = Color(0xFF60A5FA)   // Lighter blue — on light backgrounds

// ── Highlight / CTA — Lime-to-teal gradient (the featured card green) ─────────
val BrandLime          = Color(0xFF7ED957)   // Bright lime — gradient start
val BrandLimeDeep      = Color(0xFF3CC8A0)   // Teal-green — gradient end

// ── Gold — from app icon (chart bars gradient: #F5C842 → #E8A020) ─────────────
val BrandGold          = Color(0xFFF5C842)   // Bright icon gold
val BrandGoldDeep      = Color(0xFFE8A020)   // Darker icon gold
val BrandGoldDim       = Color(0xFF3D2800)   // Very dark gold — tertiaryContainer

// ── Success / Income ──────────────────────────────────────────────────────────
val BrandEmerald       = Color(0xFF10B981)
val BrandEmeraldLight  = Color(0xFF34D399)

// ── Warning ───────────────────────────────────────────────────────────────────
val BrandAmber         = Color(0xFFF59E0B)
val BrandAmberLight    = Color(0xFFFBBF24)

// ── Danger ────────────────────────────────────────────────────────────────────
val BrandRed           = Color(0xFFEF4444)
val BrandRedLight      = Color(0xFFFCA5A5)

// ── Light mode surfaces ───────────────────────────────────────────────────────
val SurfaceLight       = Color(0xFFFFFFFF)
val BackgroundLight    = Color(0xFFF0F4FF)   // Very subtle blue tint
val SurfaceVarLight    = Color(0xFFE8EEFF)   // Blue-tinted surface variant

// ── Simple-style dark surfaces — dark teal-slate ─────────────────────────────
// Inspired by Simple Pay: very dark teal, not black, not navy
val BackgroundSimple   = Color(0xFF0D1C25)   // Base layer — dark teal-slate
val SurfaceSimple      = Color(0xFF152535)   // Cards — slightly lifted
val SurfaceVarSimple   = Color(0xFF1C3040)   // Chips, inputs, secondary containers
val OutlineSimple      = Color(0xFF1E4050)   // Dividers — teal-tinted

// Arc line color for the hero balance card background decoration
val ArcLineColor       = Color(0xFF60A5FA)   // Blue arc lines — contrast against teal background

// ── OLED dark mode surfaces — true black ──────────────────────────────────────
val BackgroundOled     = Color(0xFF000000)   // True black — OLED pixels fully off
val SurfaceOled        = Color(0xFF0A1520)   // Cards — barely lifted, teal tint
val SurfaceVarOled     = Color(0xFF102030)   // Secondary containers
val OutlineOled        = Color(0xFF1A3545)   // Teal-tinted dividers

// ── Text tokens ───────────────────────────────────────────────────────────────
val ContentPrimary     = Color(0xFF0F2420)   // Near-black with teal undertone
val ContentSecondary   = Color(0xFF64748B)
val ContentOnDark      = Color(0xFFFFFFFF)
val ContentOnDarkSub   = Color(0xFF8BA8B5)   // Muted blue-grey secondary text