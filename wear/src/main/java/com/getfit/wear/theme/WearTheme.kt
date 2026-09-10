package com.getfit.wear.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

/**
 * Forge's watch palette — same brand color as the phone app (Tokens.kt: primary #CBF25C on a near-
 * black background), reduced to the fields Wear Compose Material's Colors() actually needs; every
 * field not set here keeps that constructor's own sensible default rather than guessing at one.
 */
val ForgeWearColors = Colors(
    primary = Color(0xFFCBF25C),
    primaryVariant = Color(0xFFA9CC4B),
    onPrimary = Color(0xFF17120D),
    secondary = Color(0xFFCBF25C),
    onSecondary = Color(0xFF17120D),
    background = Color(0xFF0B0B0B),
    onBackground = Color(0xFFF5F5F0),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFF5F5F0),
    onSurfaceVariant = Color(0xFFB0B0AA),
    error = Color(0xFFCF6679),
    onError = Color(0xFF17120D),
)
