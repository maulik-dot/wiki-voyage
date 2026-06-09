package com.example.wikipedia_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TealCyan,
    secondary = RustyRed,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF2B2930),
    onPrimary = CreamOffWhite,
    onSecondary = CreamOffWhite,
    onBackground = Color(0xFFECE6F0),
    onSurface = Color(0xFFECE6F0),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

private val LightColorScheme = lightColorScheme(
    primary = TealCyan,
    secondary = RustyRed,
    background = BackgroundBeige,
    surface = CreamOffWhite,
    onPrimary = CreamOffWhite,
    onSecondary = CreamOffWhite,
    onBackground = DarkBrown,
    onSurface = DarkBrown,
    onSurfaceVariant = DarkBrown
)

@Composable
fun WikipediaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}