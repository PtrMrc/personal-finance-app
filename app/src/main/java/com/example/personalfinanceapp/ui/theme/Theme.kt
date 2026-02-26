package com.example.personalfinanceapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary              = BrandIndigo,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFE0E7FF),
    onPrimaryContainer   = Color(0xFF1E1B4B),

    secondary            = BrandEmerald,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),

    tertiary             = BrandAmber,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFFEF3C7),
    onTertiaryContainer  = Color(0xFF78350F),

    background           = BackgroundLight,
    onBackground         = ContentPrimary,

    surface              = SurfaceLight,
    onSurface            = ContentPrimary,
    surfaceVariant       = SurfaceVarLight,
    onSurfaceVariant     = ContentSecondary,

    outline              = Color(0xFFE2E8F0),
    error                = BrandRed,
    onError              = Color.White
)

// True-black dark scheme — deep OLED look, Revolut-inspired
private val DarkColorScheme = darkColorScheme(
    primary              = BrandIndigoVibrant,
    onPrimary            = Color.White,
    primaryContainer     = BrandIndigoDim,
    onPrimaryContainer   = Color(0xFFE0E7FF),

    secondary            = BrandEmeraldLight,
    onSecondary          = Color(0xFF064E3B),
    secondaryContainer   = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),

    tertiary             = BrandAmberLight,
    onTertiary           = Color(0xFF78350F),
    tertiaryContainer    = Color(0xFF92400E),
    onTertiaryContainer  = Color(0xFFFEF3C7),

    // True black — OLED pixels fully off
    background           = BackgroundDark,
    onBackground         = ContentOnDark,

    // Cards are #111, barely lifted off the black background
    surface              = SurfaceDark,
    onSurface            = ContentOnDark,

    // Chips, input backgrounds, secondary containers
    surfaceVariant       = SurfaceVarDark,
    onSurfaceVariant     = ContentOnDarkSub,

    outline              = OutlineDark,
    error                = BrandRedLight,
    onError              = Color(0xFF7F1D1D)
)

@Composable
fun PersonalFinanceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar blends with our background (black in dark, light grey in light)
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
