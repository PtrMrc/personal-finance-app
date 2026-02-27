package com.example.personalfinanceapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand / Primary — Electric Blue ──────────────────────────────────────────
// Vibrant blue that pops on both black OLED and white backgrounds
val BrandIndigo        = Color(0xFF1D6FFF)   // Light mode primary — Strong electric blue
val BrandIndigoVibrant = Color(0xFF4DA6FF)   // Dark mode primary — Neon electric blue on OLED
val BrandIndigoDim    = Color(0xFF0F3580)   // Dark mode primaryContainer — Deep navy

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
val BackgroundLight   = Color(0xFFF0F4FF)   // Very subtle blue tint on light bg
val SurfaceVarLight   = Color(0xFFE8EEFF)   // Blue-tinted surface variant

// ── Dark mode surfaces — true black (OLED / Revolut-style) ───────────────────
// True black background so OLED pixels turn fully off
val BackgroundDark    = Color(0xFF000000)
// Cards sit just barely above — a whisper of deep navy
val SurfaceDark       = Color(0xFF090E1A)
// Secondary containers, chips, input backgrounds
val SurfaceVarDark    = Color(0xFF111827)
// Dividers, subtle borders — blue-tinted so the palette stays cohesive
val OutlineDark       = Color(0xFF1E3060)

// ── Text tokens ───────────────────────────────────────────────────────────────
val ContentPrimary    = Color(0xFF0F172A)   // Near-black with blue undertone
val ContentSecondary  = Color(0xFF64748B)
val ContentOnDark     = Color(0xFFFFFFFF)
val ContentOnDarkSub  = Color(0xFF8EA3C8)   // Blue-tinted secondary label on black
