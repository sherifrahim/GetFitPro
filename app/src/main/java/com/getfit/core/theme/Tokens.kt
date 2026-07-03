package com.getfit.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design tokens lifted verbatim from GetFit.dc.html. When a value is unclear, the prototype wins.
 */
object GfColor {
    // Surfaces (dark, warm)
    val Background = Color(0xFF17120D)
    val Page = Color(0xFF0C0906)
    val Surface = Color(0xFF211A12)
    val SurfaceElevated = Color(0xFF2B2118)
    val SurfaceElevatedAlt = Color(0xFF1C150E)

    // Hairlines (rgba(246,240,230, a))
    val Hairline06 = Color(0x0FF6F0E6) // ~.06
    val Hairline08 = Color(0x14F6F0E6) // ~.08
    val Hairline10 = Color(0x1AF6F0E6) // ~.10
    val Hairline12 = Color(0x1FF6F0E6) // ~.12

    // Text
    val Text = Color(0xFFF6F0E6)
    val TextDim = Color(0xFFA99C8A)
    val TextFaint = Color(0xFF6E6353)
    val TextCue = Color(0xFFC9BDAB)

    // Accent lime + gradient
    val Lime = Color(0xFFCBF25C)
    val LimeDeep = Color(0xFFA9D63E)
    val OnAccent = Color(0xFF17120D)
    val OnAccentDim = Color(0xFF3F5610)
    val OnAccentDim2 = Color(0xFF41550F)
    val OnAccentSub = Color(0xFF33430F)

    // Semantic
    val Amber = Color(0xFFF3B24A)
    val Coral = Color(0xFFF0774E)
    val Mint = Color(0xFF86D6B4)

    // Difficulty
    val Beginner = Color(0xFF8FD694)
    val Intermediate = Color(0xFFF3B24A)
    val Advanced = Color(0xFFF0774E)

    // Faint accent fills
    val LimeFill12 = Color(0x1FCBF25C) // rgba(203,242,92,.12)
    val LimeFill15 = Color(0x26CBF25C)
    val AmberFill13 = Color(0x21F3B24A)

    val AccentGradient = Brush.linearGradient(listOf(Lime, LimeDeep))
}

object GfDifficulty {
    fun color(level: String): Color = when (level) {
        "Beginner" -> GfColor.Beginner
        "Advanced" -> GfColor.Advanced
        else -> GfColor.Intermediate
    }
    fun bg(level: String): Color = when (level) {
        "Beginner" -> Color(0x248FD694)
        "Advanced" -> Color(0x24F0774E)
        else -> Color(0x24F3B24A)
    }
}
