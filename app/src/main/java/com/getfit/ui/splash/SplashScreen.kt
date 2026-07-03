package com.getfit.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.anim.GfSpring
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.SpaceGrotesk

@Composable
fun SplashScreen() {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, GfSpring.overshoot()) }
    Box(
        Modifier.fillMaxSize().background(GfColor.Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(96.dp).scale(scale.value).clip(RoundedCornerShape(28.dp)).background(GfColor.Lime),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = GfColor.OnAccent, modifier = Modifier.size(54.dp))
            }
            Text(
                "GetFit", color = GfColor.Text, fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.W700, fontSize = 30.sp,
                modifier = Modifier.padding(top = 20.dp), textAlign = TextAlign.Center,
            )
            Text(
                "Train smarter", color = GfColor.TextDim, fontFamily = Manrope,
                fontWeight = FontWeight.W600, fontSize = 13.5.sp,
            )
        }
    }
}
