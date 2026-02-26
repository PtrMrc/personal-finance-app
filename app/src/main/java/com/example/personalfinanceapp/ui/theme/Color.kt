package com.example.personalfinanceapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary brand ─────────────────────────────────────────────────────────────
// Indigo — used for primary actions, balance highlight, and key accents
val BrandIndigo       = Color(0xFF6366F1)
val BrandIndigoLight  = Color(0xFF818CF8)   // Dark-theme primary
val BrandIndigoDark   = Color(0xFF3730A3)   // Dark-theme primaryContainer

// ── Success / income ──────────────────────────────────────────────────────────
// Emerald — used for income amounts, positive budget state, and secondary actions
val BrandEmerald      = Color(0xFF10B981)
val BrandEmeraldLight = Color(0xFF34D399)   // Dark-theme secondary

// ── Warning ───────────────────────────────────────────────────────────────────
// Amber — used for budget warning state and tertiary accents
val BrandAmber        = Color(0xFFF59E0B)
val BrandAmberLight   = Color(0xFFFBBF24)   // Dark-theme tertiary

// ── Danger / error ────────────────────────────────────────────────────────────
val BrandRed          = Color(0xFFEF4444)
val BrandRedLight     = Color(0xFFFCA5A5)   // Dark-theme error (softer on dark bg)

// ── Neutral surfaces (light) ──────────────────────────────────────────────────
val SurfaceLight      = Color(0xFFFFFFFF)
val BackgroundLight   = Color(0xFFF8F9FA)
val SurfaceVarLight   = Color(0xFFF1F5F9)

// ── Neutral surfaces (dark) ───────────────────────────────────────────────────
val SurfaceDark       = Color(0xFF1E293B)
val BackgroundDark    = Color(0xFF0F172A)
val SurfaceVarDark    = Color(0xFF334155)

// ── Content / text ────────────────────────────────────────────────────────────
val ContentPrimary    = Color(0xFF1E293B)   // Main text on light backgrounds
val ContentSecondary  = Color(0xFF64748B)   // Secondary/hint text on light
val ContentOnDark     = Color(0xFFF1F5F9)   // Main text on dark backgrounds
val ContentOnDarkSub  = Color(0xFF94A3B8)   // Secondary text on dark backgrounds