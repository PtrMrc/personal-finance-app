package com.example.personalfinanceapp.ui.theme

import android.app.Activity
import android.os.Build
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
import com.example.personalfinanceapp.data.AppTheme

private val LightColorScheme = lightColorScheme(
    primary              = BrandBlue,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFDBEAFE),
    onPrimaryContainer   = Color(0xFF1E3A8A),
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
    outline              = Color(0xFFCCE8E2),
    error                = BrandRed,
    onError              = Color.White
)

// Simple-style dark scheme: dark teal-slate background, teal primary, gold accent.
// Inspired by the Simple Pay (OTP) app color language.
private val SimpleColorScheme = darkColorScheme(
    primary              = BrandBlue,
    onPrimary            = Color(0xFF1E3A8A),
    primaryContainer     = BrandBlueDim,
    onPrimaryContainer   = Color(0xFFDBEAFE),
    secondary            = BrandEmeraldLight,
    onSecondary          = Color(0xFF064E3B),
    secondaryContainer   = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary             = BrandGold,
    onTertiary           = Color(0xFF3D2800),
    tertiaryContainer    = BrandGoldDim,
    onTertiaryContainer  = BrandGold,
    background           = BackgroundSimple,
    onBackground         = ContentOnDark,
    surface              = SurfaceSimple,
    onSurface            = ContentOnDark,
    surfaceVariant       = SurfaceVarSimple,
    onSurfaceVariant     = ContentOnDarkSub,
    outline              = OutlineSimple,
    error                = BrandRedLight,
    onError              = Color(0xFF7F1D1D)
)

// True-black OLED scheme: same teal accent, max battery saving on OLED screens.
private val OledColorScheme = darkColorScheme(
    primary              = BrandBlue,
    onPrimary            = Color(0xFF1E3A8A),
    primaryContainer     = BrandBlueDim,
    onPrimaryContainer   = Color(0xFFDBEAFE),
    secondary            = BrandEmeraldLight,
    onSecondary          = Color(0xFF064E3B),
    secondaryContainer   = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary             = BrandGold,
    onTertiary           = Color(0xFF3D2800),
    tertiaryContainer    = BrandGoldDim,
    onTertiaryContainer  = BrandGold,
    background           = BackgroundOled,
    onBackground         = ContentOnDark,
    surface              = SurfaceOled,
    onSurface            = ContentOnDark,
    surfaceVariant       = SurfaceVarOled,
    onSurfaceVariant     = ContentOnDarkSub,
    outline              = OutlineOled,
    error                = BrandRedLight,
    onError              = Color(0xFF7F1D1D)
)

@Composable
fun PersonalFinanceAppTheme(
    appTheme: AppTheme = AppTheme.SIMPLE,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = appTheme != AppTheme.LIGHT

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        appTheme == AppTheme.OLED   -> OledColorScheme
        appTheme == AppTheme.SIMPLE -> SimpleColorScheme
        else                        -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}