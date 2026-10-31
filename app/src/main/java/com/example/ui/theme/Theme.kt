package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryViolet,
    onPrimary = Color.White,
    primaryContainer = SurfaceHighlightDark,
    onPrimaryContainer = PrimaryVioletLight,
    secondary = SecondaryAccent,
    onSecondary = Color.Black,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = SecondaryAccent,
    tertiary = AccentPink,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

private val AmoledColorScheme = darkColorScheme(
    primary = PrimaryViolet,
    onPrimary = Color.White,
    primaryContainer = SurfaceHighlightAmoled,
    onPrimaryContainer = PrimaryVioletLight,
    secondary = SecondaryAccent,
    onSecondary = Color.Black,
    secondaryContainer = SurfaceVariantAmoled,
    onSecondaryContainer = SecondaryAccent,
    tertiary = AccentPink,
    onTertiary = Color.White,
    background = BackgroundAmoled,
    onBackground = TextPrimary,
    surface = SurfaceAmoled,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantAmoled,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryViolet,
    onPrimary = Color.White,
    primaryContainer = SurfaceHighlightLight,
    onPrimaryContainer = PrimaryVioletDark,
    secondary = SecondaryAccent,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = Color(0xFF0E7490),
    tertiary = AccentPink,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0x339CA3AF)
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.AMOLED -> AmoledColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val bg = colorScheme.background.toArgb()
                window.statusBarColor = bg
                window.navigationBarColor = bg
                val controller = WindowCompat.getInsetsController(window, view)
                val isLight = themeMode == AppThemeMode.LIGHT
                controller.isAppearanceLightStatusBars = isLight
                controller.isAppearanceLightNavigationBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
