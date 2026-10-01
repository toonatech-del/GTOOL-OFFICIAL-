package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Earthy Warm Olive-Charcoal Base Palette (Darkened for Deep Black Aesthetic)
val OliveBgTop = Color(0xFF0D0D0F)
val OliveBgBottom = Color(0xFF050506)
val OliveCardBg = Color(0xCC1A1B1F) // rgba(26, 27, 31, 0.8)
val OliveCardBgSolid = Color(0xFF141519)
val OliveStroke = Color(0x1FFFFFFF) // rgba(255, 255, 255, 0.12)
val OliveStrokeLight = Color(0x35FFFFFF)

// High Contrast Accents
val WhiteAccentBadge = Color(0xFFFFFFFF)
val DarkIconTint = Color(0xFF1D1C16)
val AmberWarm = Color(0xFFFFB87E)
val AmberGlow = Color(0xFFE8833A)
val WarmCream = Color(0xFFFFE8D1)
val WarmGold = Color(0xFFF5D6A0)

// Text Hierarchy
val TextPrimary = Color(0xFFF5F5F3) // Dark Mode Default
val SectionHeaderColor = Color(0xFFE6E5DF)
val TextSecondary = Color(0xFFC7C5BC) // Dark Mode Default
val TextMuted = Color(0xFF9E9B90) // Dark Mode Default

// Light Mode Text Hierarchy (High Contrast)
val TextPrimaryLight = Color(0xFF1A1A1A)
val TextSecondaryLight = Color(0xFF4A4A4A)
val TextMutedLight = Color(0xFF757575)

// Legacy compatibility aliases
val DeepCharcoal = OliveBgBottom
val DeepCharcoalSurface = OliveBgTop
val DeepCharcoalElevated = OliveCardBgSolid
val WarmEarthDark = Color(0xFF25231C)
val EarthBronze = Color(0xFF383226)

// Glassmorphism Constants
val GlassCardBackground = Color(0x28FFFFFF)
val GlassCardBackgroundLight = Color(0x3AFFFFFF)
val GlassCardBackgroundDark = Color(0x12FFFFFF)
val GlassStroke = Color(0x1FFFFFFF)
val GlassStrokeLight = Color(0x3BFFFFFF)
val GlassStrokeSubtle = Color(0x14FFFFFF)

// Status & Badges
val SyncGreen = Color(0xFF22C55E)
val SyncGreenContainer = Color(0x2622C55E)
val SyncGreenGlow = Color(0x6622C55E)
val PdfRed = Color(0xFFFF5252)
val NoteYellow = Color(0xFFFFD166)
val ImagePurple = Color(0xFFC084FC)
val VoiceCyan = Color(0xFF38BDF8)

// Gradients
val GlassGradientBrush = Brush.linearGradient(
    listOf(
        Color(0x33FFFFFF),
        Color(0x10FFFFFF)
    )
)

val GlassStrokeGradient = Brush.linearGradient(
    listOf(
        Color(0x3BFFFFFF),
        Color(0x14FFFFFF),
        Color(0x26FFFFFF)
    )
)

val OliveAmbientGradient = Brush.verticalGradient(
    listOf(
        OliveBgTop,
        Color(0xFF25231C),
        OliveBgBottom
    )
)

val WarmAmbientGradient = OliveAmbientGradient

