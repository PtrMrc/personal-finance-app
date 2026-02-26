package com.example.personalfinanceapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand / Primary ───────────────────────────────────────────────────────────
// Electric indigo — pops on both black and white backgrounds
val BrandIndigo        = Color(0xFF00C896)   // Light mode primary — Electric Blue
val BrandIndigoVibrant = Color(0xFF00E5A8)   // Dark mode primary (lighter on black)
val BrandIndigoDim    = Color(0xFF00896A)   // Dark mode primaryContainer

// ── Success / Income ──────────────────────────────────────────────────────────
val BrandEmerald      = Color(0xFF10B981)
val BrandEmeraldLight = Color(0xFF34D399)

// ── Warning ───────────────────────────────────────────────────────────────────
val BrandAmber        = Color(0xFFF59E0B)
val BrandAmberLight   = Color(0xFFFBBF24)

// ── Danger ────────────────────────────────────────────────────────────────────
val BrandRed          = Color(0xFFEF4444)
val BrandRedLight     = Color(0xFFFCA5A5)

// ── Light mode surfaces ───────────────────────────────────────────────────────
val SurfaceLight      = Color(0xFFFFFFFF)
val BackgroundLight   = Color(0xFFF8F9FA)
val SurfaceVarLight   = Color(0xFFF1F5F9)

// ── Dark mode surfaces — true black (OLED / Revolut style) ───────────────────
// True black background so OLED pixels turn fully off
val BackgroundDark    = Color(0xFF000000)
// Cards sit just barely above the background — visible but not grey
val SurfaceDark       = Color(0xFF111111)
// Secondary containers, chips, input backgrounds
val SurfaceVarDark    = Color(0xFF1C1C1E)
// Dividers, subtle borders
val OutlineDark       = Color(0xFF2C2C2E)

// ── Text tokens ───────────────────────────────────────────────────────────────
val ContentPrimary    = Color(0xFF1E293B)
val ContentSecondary  = Color(0xFF64748B)
val ContentOnDark     = Color(0xFFFFFFFF)
val ContentOnDarkSub  = Color(0xFF8E8E93)   // iOS-style secondary label on black
