package com.example.wikipedia_app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.wikipedia_app.R

/**
 * Editorial type scale.
 *
 * Wikipedia pairs a serif for titles/headings (web uses Linux Libertine) with a
 * clean sans for running text. We approximate that with the platform serif for
 * display + headline styles and bundled Roboto for titles, body and labels.
 */

val RobotoFont = FontFamily(
    Font(R.font.roboto_regular),
    Font(R.font.roboto_bold, FontWeight.Bold),
    Font(R.font.roboto_italic, FontWeight.Normal, FontStyle.Italic)
)

// Serif used for article titles and section headings — the "editorial" voice.
val SerifFont = FontFamily.Serif

val Typography = Typography(
    // Big editorial titles (article header, "won!" screen) — serif
    displayLarge = TextStyle(
        fontFamily = SerifFont, fontWeight = FontWeight.Normal,
        fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SerifFont, fontWeight = FontWeight.Normal,
        fontSize = 32.sp, lineHeight = 40.sp
    ),
    displaySmall = TextStyle(
        fontFamily = SerifFont, fontWeight = FontWeight.Normal,
        fontSize = 28.sp, lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SerifFont, fontWeight = FontWeight.Normal,
        fontSize = 26.sp, lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SerifFont, fontWeight = FontWeight.Normal,
        fontSize = 23.sp, lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SerifFont, fontWeight = FontWeight.Normal,
        fontSize = 20.sp, lineHeight = 28.sp
    ),
    // Card titles, list rows, section labels — sans
    titleLarge = TextStyle(
        fontFamily = RobotoFont, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RobotoFont, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RobotoFont, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    // Running text
    bodyLarge = TextStyle(
        fontFamily = RobotoFont, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 25.sp, letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RobotoFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RobotoFont, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 17.sp
    ),
    // Overline labels ("FEATURED ARTICLE"), buttons, chips
    labelLarge = TextStyle(
        fontFamily = RobotoFont, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoFont, fontWeight = FontWeight.Bold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoFont, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    )
)
