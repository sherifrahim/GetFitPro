package com.getfit.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.msIcon
import com.getfit.data.db.Curated

@Composable
fun OnboardingScreen(vm: com.getfit.ui.AppViewModel) {
    val nav by vm.nav.collectAsState()
    val slides = Curated.ONBOARD
    val cta = if (nav.obSlide < 2) "Continue" else "Get started"

    Column(Modifier.fillMaxSize().background(GfColor.Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.End) {
            Text("Skip", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp, modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { vm.obSkip() })
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val w = maxWidth
            val shift by animateDpAsState(-w * nav.obSlide, tween(500), label = "obShift")
            Row(Modifier.offset(x = shift).fillMaxHeight()) {
                slides.forEach { s -> Slide(s, w) }
            }
        }
        Column(Modifier.padding(horizontal = 30.dp).padding(bottom = 40.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 28.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { i ->
                    val wDot by animateDpAsState(if (i == nav.obSlide) 26.dp else 8.dp, label = "dot")
                    Box(Modifier.padding(horizontal = 4.dp).size(width = wDot, height = 8.dp).clip(RoundedCornerShape(999.dp)).background(if (i == nav.obSlide) GfColor.Accent else GfColor.Text.copy(alpha = 0.18f)))
                }
            }
            Box(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(GfColor.Accent)
                    .clickable(remember { MutableInteractionSource() }, indication = null) { vm.obNext() },
                contentAlignment = Alignment.Center,
            ) {
                Text(cta, color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.5.sp)
            }
        }
    }
}

@Composable
private fun Slide(s: Curated.OnboardSlide, width: androidx.compose.ui.unit.Dp) {
    val infinite = rememberInfiniteTransition(label = "float")
    val y by infinite.animateFloat(-9f, 9f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "floaty")
    Column(
        Modifier.width(width).fillMaxHeight().padding(horizontal = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.offset(y = y.dp).size(150.dp).clip(RoundedCornerShape(40.dp)).background(Color(s.glowArgb)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(msIcon(s.icon), null, tint = Color(s.iconArgb), modifier = Modifier.size(76.dp))
        }
        Text(s.title, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 27.sp, lineHeight = 32.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 40.dp))
        Text(s.body, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 15.sp, lineHeight = 24.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
    }
}
