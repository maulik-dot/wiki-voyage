package com.example.wikipedia_app.ui.theme

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

/**
 * Wikipedia-authentic colour schemes. Clean paper surfaces, link-blue accent,
 * neutral chrome. When [dynamicColor] is on (Android 12+) Material You wallpaper
 * tones drive the accents instead, keeping the editorial neutrals.
 */

private val LightColors = lightColorScheme(
    primary = WikiBlue,
    onPrimary = Color.White,
    primaryContainer = WikiBlueContainer,
    onPrimaryContainer = Color(0xFF001A41),
    secondary = WikiGreen,
    onSecondary = Color.White,
    tertiary = WikiAmber,
    onTertiary = Color.White,
    error = WikiRed,
    onError = Color.White,
    background = WikiCanvas,
    onBackground = WikiBaseText,
    surface = WikiPaper,
    onSurface = WikiBaseText,
    surfaceVariant = WikiCanvas,
    onSurfaceVariant = WikiSubtleText,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFCFD),
    surfaceContainer = Color(0xFFF5F6F8),
    surfaceContainerHigh = Color(0xFFEEF0F3),
    surfaceContainerHighest = Color(0xFFE7E9ED),
    outline = WikiBorder,
    outlineVariant = WikiBorderSubtle
)

private val DarkColors = darkColorScheme(
    primary = WikiBlueDark,
    onPrimary = Color(0xFF002C71),
    primaryContainer = WikiNightContainer,
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFF4FD1B0),
    onSecondary = Color(0xFF00382C),
    tertiary = Color(0xFFE6B25C),
    onTertiary = Color(0xFF3F2D00),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF4B0000),
    background = WikiNightCanvas,
    onBackground = WikiNightText,
    surface = WikiNightPaper,
    onSurface = WikiNightText,
    surfaceVariant = WikiNightSurfaceHigh,
    onSurfaceVariant = WikiNightSubtleText,
    surfaceContainerLowest = Color(0xFF0B0E11),
    surfaceContainerLow = WikiNightPaper,
    surfaceContainer = Color(0xFF1E2228),
    surfaceContainerHigh = WikiNightSurfaceHigh,
    surfaceContainerHighest = Color(0xFF32353A),
    outline = WikiNightBorder,
    outlineVariant = Color(0xFF3A3E44)
)

@Composable
fun WikipediaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    // Edge-to-edge friendly status bar that matches the surface.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
