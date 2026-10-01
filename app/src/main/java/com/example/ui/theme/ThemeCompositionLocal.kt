package com.example.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal to expose the current theme mode (true = Dark, false = Light)
 * across all composable screens and components.
 */
val LocalIsDarkMode = compositionLocalOf { true }
