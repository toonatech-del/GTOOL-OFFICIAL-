package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Universal Dark Glassmorphism Palette (iOS 18 Standards)
val DarkBackground = Color(0xFF0A0E17)
val DarkSurface = Color(0xFF131824)
val DarkSurfaceVariant = Color(0xFF1E293B)
val TextPrimaryColor = Color(0xFFFFFFFF)
val TextSecondaryColor = Color(0xFF94A3B8)
val AccentAmber = Color(0xFFF59E0B)

private val UniversalDarkColorScheme = darkColorScheme(
    primary = AccentAmber,
    onPrimary = Color(0xFF0A0E17),
    primaryContainer = Color(0xFF2E2008),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = AccentAmber,
    onSecondary = Color(0xFF0A0E17),
    secondaryContainer = Color(0x33F59E0B),
    onSecondaryContainer = TextPrimaryColor,
    tertiary = SyncGreen,
    onTertiary = Color(0xFF0A0E17),
    background = DarkBackground,
    onBackground = TextPrimaryColor,
    surface = DarkSurface,
    onSurface = TextPrimaryColor,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryColor,
    outline = GlassStroke
)

/**
 * Universal Dark Theme for GTOOL X.
 * Strictly enforces dark glassmorphism architecture, completely ignoring Android system
 * light/dark mode settings, and permanently locking status bar and navigation bar icons
 * to bright white/light appearance.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = UniversalDarkColorScheme,
        typography = Typography,
        content = content
    )
}
