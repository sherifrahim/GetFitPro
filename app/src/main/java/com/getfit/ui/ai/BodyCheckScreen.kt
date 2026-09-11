package com.getfit.ui.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.Pill
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.ShimmerBox
import com.getfit.core.ui.msIcon
import com.getfit.core.ui.pressScale
import com.getfit.data.ai.BodyGoal
import com.getfit.data.ai.PROVIDER_COMPAT
import com.getfit.ui.AppViewModel

/**
 * Body check: the user picks their own photos, states a goal, ticks a consent box, and gets a
 * physique assessment with concrete training changes. See data/ai/BodyAnalysis.kt for the privacy
 * stance — the short version is that nothing here is stored and nothing is sent until Analyze.
 */
@Composable
fun BodyCheckScreen(vm: AppViewModel) {
    val state by vm.bodyCheck.collectAsState()
    val hasKey by vm.aiReady.collectAsState()
    val settings by vm.settings.collectAsState()
    // Named in the consent text so the user knows exactly where the photos go.
    val providerName = if (settings.aiProvider == PROVIDER_COMPAT) {
        settings.compatBaseUrl.removePrefix("https://").removePrefix("http://").substringBefore("/").ifBlank { "your AI provider" }
    } else "Anthropic's API"

    // The system Photo Picker: no storage permission, and the app only ever sees the URIs the user
    // hands over — not the whole gallery.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        vm.bodyCheckAddPhotos(uris)
    }

    Column(Modifier.fillMaxSize().background(GfColor.Background).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                msIcon("arrow_back"), null, tint = GfColor.Text,
                modifier = Modifier.size(26.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.closeBodyCheck() },
            )
            Text("Body check", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 20.sp)
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 30.dp)) {
            if (!hasKey) {
                NoKey(onOpenSettings = { vm.closeBodyCheck(); vm.openSettings() })
                return@Column
            }

            Text(
                "Add photos of yourself — front, back and sides work best, plus a close-up of anything " +
                    "specific. You'll get a read on where you are and what to change in your training.",
                color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.5.sp, lineHeight = 20.sp,
            )

            // --- photos ---
            SectionTitle("Photos")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.photos.forEachIndexed { i, p ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(96.dp, 128.dp).clip(RoundedCornerShape(14.dp)).background(GfColor.Surface)) {
                            AsyncImage(model = p.uri, contentDescription = p.view.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xB3000000))
                                    .clickable(remember { MutableInteractionSource() }, indication = null) { vm.bodyCheckRemovePhoto(i) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(msIcon("close"), null, tint = GfColor.Text, modifier = Modifier.size(14.dp)) }
                        }
                        // Tap the label to cycle the view if the auto-assignment was wrong.
                        Box(
                            Modifier.padding(top = 6.dp).clip(Pill).background(GfColor.AccentFill12)
                                .clickable(remember { MutableInteractionSource() }, indication = null) { vm.bodyCheckCycleView(i) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(p.view.label, color = GfColor.Accent, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.5.sp)
                        }
                    }
                }
                if (state.photos.size < 5) {
                    Column(
                        Modifier.size(96.dp, 128.dp).clip(RoundedCornerShape(14.dp)).background(GfColor.Surface)
                            .border(1.dp, GfColor.Hairline10, RoundedCornerShape(14.dp))
                            .clickable(remember { MutableInteractionSource() }, indication = null, enabled = !state.loading) {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(msIcon("add_circle"), null, tint = GfColor.Accent, modifier = Modifier.size(28.dp))
                        Text(
                            if (state.photos.isEmpty()) "Add photos" else "Add more",
                            color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            // --- goal ---
            SectionTitle("Goal")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyGoal.values().forEach { g ->
                    val sel = state.goal == g
                    Box(
                        Modifier.clip(Pill).background(if (sel) GfColor.Accent else GfColor.Surface)
                            .border(1.dp, if (sel) Color.Transparent else GfColor.Hairline08, Pill)
                            .clickable(remember { MutableInteractionSource() }, indication = null) { vm.bodyCheckSetGoal(g) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text(g.label, color = if (sel) GfColor.OnAccent else GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp)
                    }
                }
            }

            // --- focus ---
            SectionTitle("Anything specific? (optional)")
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GfColor.Surface)
                    .border(1.dp, GfColor.Hairline08, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (state.focusArea.isEmpty()) {
                    Text("e.g. lagging chest, posture, left vs right arm", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 14.sp)
                }
                BasicTextField(
                    value = state.focusArea, onValueChange = vm::bodyCheckSetFocus, singleLine = true,
                    textStyle = TextStyle(color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 14.sp),
                    cursorBrush = SolidColor(GfColor.Accent), modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- consent: explicit, per-open, and it says exactly where the photos go ---
            Row(
                Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (state.consented) GfColor.AccentFill12 else GfColor.Surface)
                    .border(1.dp, if (state.consented) Color(0x660B7BF7) else GfColor.Hairline08, RoundedCornerShape(16.dp))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { vm.bodyCheckSetConsent(!state.consented) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (state.consented) GfColor.Accent else Color.Transparent)
                        .border(1.5.dp, if (state.consented) GfColor.Accent else GfColor.TextFaint, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (state.consented) Icon(msIcon("check"), null, tint = GfColor.OnAccent, modifier = Modifier.size(15.dp)) }
                Text(
                    "Send these photos to $providerName using my key. Forge doesn't store them — they're " +
                        "resized, stripped of location and device data, sent once, and discarded.",
                    color = GfColor.TextCue, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.5.sp, lineHeight = 18.sp,
                )
            }

            val canRun = state.photos.isNotEmpty() && state.consented && !state.loading
            Row(
                Modifier.padding(top = 16.dp).fillMaxWidth().height(52.dp).pressScale(remember { MutableInteractionSource() }, 0.97f)
                    .clip(RoundedCornerShape(16.dp)).background(if (canRun) GfColor.Accent else GfColor.Surface)
                    .clickable(remember { MutableInteractionSource() }, indication = null, enabled = canRun) { vm.runBodyCheck() },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    if (state.loading) "Analysing…" else "Analyse",
                    color = if (canRun) GfColor.OnAccent else GfColor.TextFaint,
                    fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp,
                )
            }

            when {
                state.loading -> Loading()
                state.error != null -> ErrorCard(state.error!!)
                state.text != null -> Result(state.text!!)
            }
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(
        t, color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.5.sp,
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
    )
}

@Composable
private fun NoKey(onOpenSettings: () -> Unit) {
    Column(Modifier.padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(msIcon("lock"), null, tint = GfColor.TextFaint, modifier = Modifier.size(40.dp))
        Text("Set up an AI provider to use the body check", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 17.sp, modifier = Modifier.padding(top = 16.dp))
        Text(
            "Your photos go to the AI provider you choose, with your key — nothing is sent until you set that up.",
            color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier.padding(top = 22.dp).height(50.dp).clip(RoundedCornerShape(16.dp)).background(GfColor.Accent)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onOpenSettings).padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { Text("Open Settings", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 14.5.sp) }
    }
}

@Composable
private fun Loading() {
    Column(Modifier.padding(top = 22.dp)) {
        Text("Looking at your photos and your training log…", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.5.sp, modifier = Modifier.padding(bottom = 14.dp))
        repeat(5) { i ->
            ShimmerBox(Modifier.fillMaxWidth(if (i == 4) 0.5f else 1f).height(16.dp).clip(RoundedCornerShape(6.dp)).padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Column(Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline08, RoundedCornerShape(18.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(msIcon("warning"), null, tint = GfColor.Coral, modifier = Modifier.size(20.dp))
            Text("Couldn't run the check", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp)
        }
        Text(message, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

/** Renders the model's ALL-CAPS headings as headings; everything else as paragraphs. */
@Composable
private fun Result(text: String) {
    Column(
        Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.Surface)
            .border(1.dp, GfColor.Hairline06, RoundedCornerShape(20.dp)).padding(18.dp),
    ) {
        text.split("\n").map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
            val isHeading = line.length <= 40 && line == line.uppercase() && line.any { it.isLetter() }
            if (isHeading) {
                Text(line, color = GfColor.Accent, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.5.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            } else {
                Text(line, color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(bottom = 12.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "A read from photos, not a measurement. Use it to steer training, not as a diagnosis.",
            color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, lineHeight = 17.sp,
        )
    }
    Spacer(Modifier.width(1.dp))
}
