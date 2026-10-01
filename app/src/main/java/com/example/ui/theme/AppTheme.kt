package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppTheme {
    val textPrimary: Color
        @Composable
        get() = if (LocalIsDarkMode.current) TextPrimary else TextPrimaryLight

    val textSecondary: Color
        @Composable
        get() = if (LocalIsDarkMode.current) TextSecondary else TextSecondaryLight

    val textMuted: Color
        @Composable
        get() = if (LocalIsDarkMode.current) TextMuted else TextMutedLight
}
