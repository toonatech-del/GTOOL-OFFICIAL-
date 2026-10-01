package com.example.model

import androidx.compose.ui.graphics.Color

enum class PdfTemplate(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val description: String,
    val primaryColorHex: String,
    val accentColorHex: String,
    val secondaryColorHex: String,
    val previewGradient: List<Color>,
    val headerStyle: String,
    val showAccentBorder: Boolean,
    val fontStyle: String
) {
    PROFESSIONAL(
        id = "professional",
        displayName = "Professional",
        subtitle = "Formal & Clean Business",
        description = "Deep navy top header with gold accent badge, formal serif-accent headings, structured dividers and formal footer.",
        primaryColorHex = "#0F2942",
        accentColorHex = "#D4AF37",
        secondaryColorHex = "#4A5568",
        previewGradient = listOf(Color(0xFF0F2942), Color(0xFF1E3A5F), Color(0xFFD4AF37)),
        headerStyle = "BANNER_TOP",
        showAccentBorder = true,
        fontStyle = "SERIF_BOLD"
    ),
    MODERN(
        id = "modern",
        displayName = "Modern Vibrant",
        subtitle = "Gradient Accent & Tech",
        description = "Dynamic cyan & orange modern gradients, sleek sans-serif typography, rounded item chips, and contemporary layout.",
        primaryColorHex = "#00838F",
        accentColorHex = "#FF6D00",
        secondaryColorHex = "#37474F",
        previewGradient = listOf(Color(0xFF00E5FF), Color(0xFFFF2D55), Color(0xFFFF9500)),
        headerStyle = "GRADIENT_ACCENT",
        showAccentBorder = true,
        fontStyle = "SANS_CLEAN"
    ),
    SIMPLE(
        id = "simple",
        displayName = "Simple Minimal",
        subtitle = "Clean & Ink-Friendly",
        description = "Monochrome minimalist layout with lightweight borders, maximum reading contrast, high print speed and low ink usage.",
        primaryColorHex = "#1A1A1A",
        accentColorHex = "#555555",
        secondaryColorHex = "#666666",
        previewGradient = listOf(Color(0xFF2B2B2B), Color(0xFF4A4A4A), Color(0xFF888888)),
        headerStyle = "MINIMAL_LINE",
        showAccentBorder = false,
        fontStyle = "MINIMAL_LIGHT"
    ),
    CREATIVE(
        id = "creative",
        displayName = "Creative Studio",
        subtitle = "Portfolio & Dark Aura",
        description = "Stylized magenta-purple sidebar header, decorative bullet points, custom quote blocks and magazine-style photo cards.",
        primaryColorHex = "#4A148C",
        accentColorHex = "#E040FB",
        secondaryColorHex = "#455A64",
        previewGradient = listOf(Color(0xFF7B1FA2), Color(0xFFE040FB), Color(0xFF00E5FF)),
        headerStyle = "STUDIO_SIDEBAR",
        showAccentBorder = true,
        fontStyle = "CURSIVE_HERO"
    )
}
