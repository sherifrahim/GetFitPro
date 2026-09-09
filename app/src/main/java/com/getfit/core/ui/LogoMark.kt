package com.getfit.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * The Forge mark: a bold, chamfered geometric "F" built from three angled slabs
 * (a chamfered stem plus two beveled arms), evoking a forged/struck edge rather
 * than a plain typographic letter or a stock icon glyph.
 *
 * Drawn from the same 108x108 coordinate scheme as
 * res/drawable/ic_launcher_foreground.xml, so the in-app mark (splash, etc.)
 * always matches the launcher icon exactly. Keep both in sync if this changes.
 */
@Composable
fun LogoMark(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 108f
        val path = Path().apply {
            // stem, chamfered bottom-left
            moveTo(34f * s, 28f * s)
            lineTo(48f * s, 28f * s)
            lineTo(48f * s, 80f * s)
            lineTo(40f * s, 80f * s)
            lineTo(34f * s, 74f * s)
            close()
            // top arm, beveled right end
            moveTo(34f * s, 28f * s)
            lineTo(78f * s, 28f * s)
            lineTo(70f * s, 40f * s)
            lineTo(34f * s, 40f * s)
            close()
            // middle arm, beveled right end
            moveTo(34f * s, 48f * s)
            lineTo(66f * s, 48f * s)
            lineTo(58f * s, 58f * s)
            lineTo(34f * s, 58f * s)
            close()
        }
        drawPath(path, color = color)
    }
}
