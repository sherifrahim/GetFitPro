package com.getfit.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design tokens.
 *
 * NOTE: these no longer track GetFit.dc.html. The prototype's warm palette (brown #17120D surfaces,
 * lime #CBF25C accent) was replaced on request with a neutral black + blue scheme in the style of
 * Hevy. The prototype remains the source of truth for layout, copy, spacing, animation and all
 * business logic — colour is the one axis where it has been deliberately superseded, so don't
 * "correct" these values back to it.
 */
object GfColor {
    // Surfaces — neutral black, no warm tint.
    val Background = Color(0xFF000000)
    val Page = Color(0xFF000000)
    val Surface = Color(0xFF121212)
    val SurfaceElevated = Color(0xFF1C1C1C)
    val SurfaceElevatedAlt = Color(0xFF0A0A0A)

    // Hairlines (rgba(255,255,255, a)) — neutral, previously tinted #F6F0E6.
    val Hairline06 = Color(0x0FFFFFFF) // ~.06
    val Hairline08 = Color(0x14FFFFFF) // ~.08
    val Hairline10 = Color(0x1AFFFFFF) // ~.10
    val Hairline12 = Color(0x1FFFFFFF) // ~.12

    // Text — pure neutral ramp. Each step keeps >=4.5:1 on the black background; TextFaint is the
    // floor at ~5.9:1, which is what the smallest labels (tab labels, "BEST", day letters) use.
    val Text = Color(0xFFFFFFFF)
    val TextDim = Color(0xFFA0A0A0)
    val TextFaint = Color(0xFF8A8A8A)
    val TextCue = Color(0xFFC8C8C8)

    // Accent — blue, replacing lime. White on this is ~4.1:1, which clears AA for the large/bold
    // button labels it is used for, but is NOT enough for small text: keep body copy off the accent
    // fill, or use Text on a dark surface instead.
    val Accent = Color(0xFF0B7BF7)
    val AccentDeep = Color(0xFF0A63C9)
    val OnAccent = Color(0xFFFFFFFF)

    // Secondary text sitting ON the accent fill (the Today card's stat labels). Translucent white
    // now, where the lime scheme used dark olive shades.
    val OnAccentDim = Color(0xCCFFFFFF)
    val OnAccentDim2 = Color(0xB3FFFFFF)
    val OnAccentSub = Color(0x99FFFFFF)

    // Faint accent fills
    val AccentFill12 = Color(0x1F0B7BF7)
    val AccentFill15 = Color(0x260B7BF7)

    // Semantic — deliberately still coloured. These do not encode brand, they encode state, and
    // both survive the monochrome pass because losing them costs real information:
    //   Amber = the rest phase, which must stay instantly distinguishable from a working set.
    //   Coral = destructive actions (Clear all data).
    val Amber = Color(0xFFF3B24A)
    val Coral = Color(0xFFF0774E)
    val Mint = Color(0xFF86D6B4)
    val AmberFill13 = Color(0x21F3B24A)

    // Difficulty — monochrome. Ordered by brightness instead of hue (dim -> bright as difficulty
    // rises), so the ranking still reads at a glance without colour coding.
    val Beginner = Color(0xFF9E9E9E)
    val Intermediate = Color(0xFFD0D0D0)
    val Advanced = Color(0xFFFFFFFF)

    // Trend verdicts — monochrome, distinguished by icon + label and by brightness.
    val TrendUp = Color(0xFFFFFFFF)
    val TrendFlat = Color(0xFFB0B0B0)
    val TrendDown = Color(0xFF8A8A8A)
    val TrendUpFill = Color(0x1FFFFFFF)
    val TrendFlatFill = Color(0x14FFFFFF)
    val TrendDownFill = Color(0x14FFFFFF)

    val AccentGradient = Brush.linearGradient(listOf(Accent, AccentDeep))
}

object GfDifficulty {
    fun color(level: String): Color = when (level) {
        "Beginner" -> GfColor.Beginner
        "Advanced" -> GfColor.Advanced
        else -> GfColor.Intermediate
    }
    /** Uniform faint white fill; the pill's rank is carried by [color]'s brightness and its label. */
    fun bg(level: String): Color = when (level) {
        "Beginner" -> Color(0x0FFFFFFF)
        "Advanced" -> Color(0x24FFFFFF)
        else -> Color(0x1AFFFFFF)
    }
}
