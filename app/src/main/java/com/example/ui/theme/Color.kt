package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Spotify-inspired BeatWave Dark Theme Palette
val BackgroundDark = Color(0xFF0F0F14) // Requested exact #0F0F14
val SurfaceDark = Color(0xFF1A1A22)    // Requested exact #1A1A22
val SurfaceVariantDark = Color(0xFF242430)
val SurfaceHighlightDark = Color(0xFF2E2E3E)

// AMOLED Pitch Black Palette
val BackgroundAmoled = Color(0xFF000000)
val SurfaceAmoled = Color(0xFF0D0D12)
val SurfaceVariantAmoled = Color(0xFF161620)
val SurfaceHighlightAmoled = Color(0xFF22222E)

// Modern Light Palette
val BackgroundLight = Color(0xFFF6F6F9)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEDEDF2)
val SurfaceHighlightLight = Color(0xFFE2E2EA)
val TextPrimaryLight = Color(0xFF111116)
val TextSecondaryLight = Color(0xFF6B7280)
val TextMutedLight = Color(0xFF9CA3AF)

// Electric Violet & Accents
val PrimaryViolet = Color(0xFF8B5CF6) // Requested exact #8B5CF6 (Electric Violet)
val PrimaryNeon = PrimaryViolet       // Alias for backward compatibility
val PrimaryVioletLight = Color(0xFFA78BFA)
val PrimaryLight = PrimaryVioletLight
val PrimaryVioletDark = Color(0xFF7C3AED)
val SecondaryAccent = Color(0xFF06B6D4) // Vivid Cyan complementary accent
val SecondaryCyan = SecondaryAccent
val AccentPink = Color(0xFFEC4899)

// Typography Colors
val TextPrimary = Color(0xFFFFFFFF)  // Requested exact white primary text
val TextSecondary = Color(0xFF9CA3AF) // Requested exact grey secondary text (#9CA3AF)
val TextMuted = Color(0xFF6B7280)

val CardBorder = Color(0x2E8B5CF6)
val GlowColor = Color(0x4D8B5CF6)

enum class AppThemeMode {
    DARK,
    LIGHT,
    AMOLED
}
