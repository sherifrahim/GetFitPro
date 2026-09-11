package com.getfit.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.ShimmerBox
import com.getfit.core.ui.msIcon
import com.getfit.core.ui.pressScale
import com.getfit.ui.AppViewModel

@Composable
fun AiReviewScreen(vm: AppViewModel) {
    val review by vm.aiReview.collectAsState()
    val hasKey by vm.aiReady.collectAsState()

    // First open with a key already set and nothing requested yet: kick off automatically.
    LaunchedEffect(Unit) {
        if (hasKey && review.text == null && review.error == null && !review.loading) vm.runAiReview()
    }

    Column(Modifier.fillMaxSize().background(GfColor.Background).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                msIcon("arrow_back"), null, tint = GfColor.Text,
                modifier = Modifier.size(26.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.closeAiReview() },
            )
            Text("AI review", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 20.sp)
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 30.dp)) {
            when {
                !hasKey -> NoKeyState(onOpenSettings = { vm.closeAiReview(); vm.openSettings() })
                review.loading -> LoadingState()
                review.error != null -> ErrorState(review.error!!, onRetry = vm::runAiReview)
                review.text != null -> ReviewResult(review.text!!, onRefresh = vm::runAiReview)
                else -> IdleState(onStart = vm::runAiReview)
            }
        }
    }
}

@Composable
private fun NoKeyState(onOpenSettings: () -> Unit) {
    Column(Modifier.padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(msIcon("lock"), null, tint = GfColor.TextFaint, modifier = Modifier.size(40.dp))
        Text(
            "Set up an AI provider to use AI review", color = GfColor.Text, fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.W700, fontSize = 17.sp, modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Forge sends a summary of your logged workouts to the AI provider you choose — Anthropic, " +
                "OpenAI, Groq, DeepSeek or your own — with your key. Nothing " +
                "leaves your device until you set this up.",
            color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.5.sp,
            lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier.padding(top = 22.dp).height(50.dp).pressScale(remember { MutableInteractionSource() }, 0.97f)
                .clip(RoundedCornerShape(16.dp)).background(GfColor.Accent)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onOpenSettings)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Open Settings", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
        }
    }
}

@Composable
private fun IdleState(onStart: () -> Unit) {
    Column(Modifier.padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(msIcon("smart_display"), null, tint = GfColor.Accent, modifier = Modifier.size(40.dp))
        Text(
            "Get a read on your training", color = GfColor.Text, fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.W700, fontSize = 17.sp, modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Sends your recent sessions, bests and targets to your AI provider and asks for an honest review.",
            color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.5.sp,
            lineHeight = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier.padding(top = 22.dp).height(50.dp).pressScale(remember { MutableInteractionSource() }, 0.97f)
                .clip(RoundedCornerShape(16.dp)).background(GfColor.Accent)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onStart)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Get AI review", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
        }
    }
}

@Composable
private fun LoadingState() {
    Column(Modifier.padding(top = 24.dp)) {
        Text(
            "Reading your training log…", color = GfColor.TextDim, fontFamily = Manrope,
            fontWeight = FontWeight.W600, fontSize = 13.5.sp, modifier = Modifier.padding(bottom = 14.dp),
        )
        repeat(4) { i ->
            ShimmerBox(
                Modifier.fillMaxWidth(if (i == 3) 0.6f else 1f).height(16.dp).clip(RoundedCornerShape(6.dp))
                    .padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.padding(top = 24.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline08, RoundedCornerShape(18.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(msIcon("warning"), null, tint = GfColor.Coral, modifier = Modifier.size(20.dp))
            Text("Couldn't get a review", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp)
        }
        Text(message, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
        Row(
            Modifier.padding(top = 16.dp).height(44.dp).clip(RoundedCornerShape(14.dp)).background(GfColor.Accent)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onRetry)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Try again", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 13.5.sp)
        }
    }
}

@Composable
private fun ReviewResult(text: String, onRefresh: () -> Unit) {
    Column {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.Surface)
                .border(1.dp, GfColor.Hairline06, RoundedCornerShape(20.dp)).padding(18.dp),
        ) {
            text.split("\n").filter { it.isNotBlank() }.forEach { para ->
                Text(
                    para.trim(), color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W500,
                    fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
        Row(
            Modifier.padding(top = 16.dp).height(46.dp).clip(RoundedCornerShape(14.dp)).background(GfColor.Surface)
                .border(1.dp, GfColor.Hairline08, RoundedCornerShape(14.dp))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onRefresh)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(msIcon("history"), null, tint = GfColor.Accent, modifier = Modifier.size(18.dp))
            Text("Get a fresh review", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.5.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}
