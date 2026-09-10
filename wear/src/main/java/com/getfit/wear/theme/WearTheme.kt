package com.getfit.wear.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

/**
 * Forge's watch palette — mirrors the phone app's tokens (app/.../core/theme/Tokens.kt: blue
 * #0B7BF7 accent on true black), reduced to the fields Wear Compose Material's Colors() actually
 * needs; every field not set here keeps that constructor's own sensible default rather than
 * guessing at one. Keep this in step with Tokens.kt when the palette changes.
 *
 * Black is a real power saving on a watch OLED, not just a style choice, so the background stays
 * pure #000000 rather than the phone's slightly-lifted surface.
 */
val ForgeWearColors = Colors(
    primary = Color(0xFF0B7BF7),
    primaryVariant = Color(0xFF0A63C9),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0B7BF7),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
)
