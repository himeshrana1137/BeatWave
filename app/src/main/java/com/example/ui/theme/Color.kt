package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Refined, Premium Dark Theme Palette (Calm, Minimal, High-end)
val BackgroundDark = Color(0xFF0B0B10)        // Requested exact #0B0B10
val SurfaceDark = Color(0xFF14141B)           // Requested exact #14141B
val SurfaceVariantDark = Color(0xFF1C1C26)    // Elevated surface: #1C1C26
val SurfaceHighlightDark = Color(0xFF242432)  // Tonal highlight surface

// AMOLED Pitch Black Palette
val BackgroundAmoled = Color(0xFF000000)
val SurfaceAmoled = Color(0xFF0B0B10)
val SurfaceVariantAmoled = Color(0xFF14141B)
val SurfaceHighlightAmoled = Color(0xFF1C1C26)

// Modern Light Palette
val BackgroundLight = Color(0xFFF6F6F9)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEDEDF2)
val SurfaceHighlightLight = Color(0xFFE2E2EA)
val TextPrimaryLight = Color(0xFF111116)
val TextSecondaryLight = Color(0xFF6B7280)
val TextMutedLight = Color(0xFF9CA3AF)

// Restrained Violet Accent (#8B5CF6 - used sparingly: only play button, active tab, slider, current item)
val PrimaryViolet = Color(0xFF8B5CF6)
val PrimaryNeon = PrimaryViolet
val PrimaryVioletLight = Color(0xFFA78BFA)
val PrimaryLight = PrimaryVioletLight
val PrimaryVioletDark = Color(0xFF7C3AED)
val SecondaryAccent = Color(0xFF38BDF8)
val SecondaryCyan = SecondaryAccent
val AccentPink = Color(0xFFF43F5E)

// Typography Colors
val TextPrimary = Color(0xFFF2F2F7)           // Requested exact #F2F2F7
val TextSecondary = Color(0xFF9A9AA8)         // Requested exact #9A9AA8
val TextMuted = Color(0xFF6B6B7A)

// 1dp at 6% white opacity border (replaces purple borders)
val CardBorder = Color(0x0FFFFFFF)
val GlowColor = Color(0x1A8B5CF6)
val SubtleBorder = Color(0x0FFFFFFF)

enum class AppThemeMode {
    DARK,
    LIGHT,
    AMOLED
}
