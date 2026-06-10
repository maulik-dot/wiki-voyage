package com.example.wikipedia_app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Wikipedia-authentic palette.
 *
 * Mirrors the colour tokens Wikipedia uses on web/app: a near-black base text,
 * a clean paper-white surface, the signature link blue (#3366CC) and a muted
 * neutral grey for chrome. The legacy brand colours (teal / rust / cream) are
 * kept only as fallbacks for any code that still references them.
 */

// ---- Wikipedia base (light) ----
val WikiBaseText = Color(0xFF202122)       // body text
val WikiSubtleText = Color(0xFF54595D)     // captions, metadata
val WikiPaper = Color(0xFFFFFFFF)          // article surface
val WikiCanvas = Color(0xFFF8F9FA)         // page background ("base90")
val WikiBorder = Color(0xFFA2A9B1)         // hairline dividers
val WikiBorderSubtle = Color(0xFFEAECF0)   // faint dividers

// ---- Wikipedia accent ----
val WikiBlue = Color(0xFF3366CC)           // link / primary accent
val WikiBlueContainer = Color(0xFFEAF1FF)  // tonal blue surface
val WikiBlueDark = Color(0xFF6699FF)       // link on dark surfaces
val WikiRed = Color(0xFFB32424)            // destructive / red-link
val WikiGreen = Color(0xFF14866D)          // success / "good article"
val WikiAmber = Color(0xFFAC6600)          // featured-article star

// ---- Wikipedia dark mode (Vector 2022 night theme) ----
val WikiNightCanvas = Color(0xFF101418)
val WikiNightPaper = Color(0xFF1B1F23)
val WikiNightSurfaceHigh = Color(0xFF27292D)
val WikiNightText = Color(0xFFEAECF0)
val WikiNightSubtleText = Color(0xFFA2A9B1)
val WikiNightBorder = Color(0xFF54595D)
val WikiNightContainer = Color(0xFF1F2A3D)

// ---- Legacy brand (kept for backward-compatible references) ----
val BackgroundBeige = Color(0xFFF0E0BB)
val TealCyan = Color(0xFF407880)
val CreamOffWhite = Color(0xFFF7EED6)
val RustyRed = Color(0xFFC9582D)
val DarkBrown = Color(0xFF3A2C1E)
