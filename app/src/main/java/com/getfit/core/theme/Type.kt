@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.getfit.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.getfit.R

private fun sg(weight: Int) =
    Font(R.font.space_grotesk, weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

private fun mr(weight: Int) =
    Font(R.font.manrope, weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Space Grotesk — headings, numbers, display. */
val SpaceGrotesk = FontFamily(sg(400), sg(500), sg(600), sg(700))

/** Manrope — body. */
val Manrope = FontFamily(mr(400), mr(500), mr(600), mr(700), mr(800))

private val body = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W500)
private val display = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700)

val GfType = Typography(
    displayLarge = display.copy(fontSize = 30.sp, letterSpacing = (-0.5).sp),
    headlineMedium = display.copy(fontSize = 26.sp, letterSpacing = (-0.5).sp),
    headlineSmall = display.copy(fontSize = 24.sp, letterSpacing = (-0.4).sp),
    titleLarge = display.copy(fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.W600, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.W600, fontSize = 15.sp),
    bodyLarge = body.copy(fontSize = 15.sp),
    bodyMedium = body.copy(fontSize = 13.sp),
    bodySmall = body.copy(fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 10.5.sp),
)
