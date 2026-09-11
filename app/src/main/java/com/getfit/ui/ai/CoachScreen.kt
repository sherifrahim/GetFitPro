package com.getfit.ui.ai

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.Pill
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.msIcon
import com.getfit.core.ui.pressScale
import com.getfit.data.ai.CoachMode
import com.getfit.domain.Units
import com.getfit.domain.isBW
import com.getfit.ui.AppViewModel

/** The AI coach: pick an ask, read the answer, turn proposed targets into real ones with a tap. */
@Composable
fun CoachScreen(vm: AppViewModel) {
    val coach by vm.coach.collectAsState()
    val ready by vm.aiReady.collectAsState()
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()

    // Auto-run on open when configured and nothing has been asked for this mode yet.
    LaunchedEffect(coach.mode, coach.sessionId, ready) {
        if (ready && coach.text == null && coach.error == null && !coach.loading) vm.runCoach()
    }

    Column(Modifier.fillMaxSize().background(GfColor.Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(msIcon("arrow_back"), null, tint = GfColor.Text, modifier = Modifier.size(26.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.closeCoach() })
            Text("AI coach", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 20.sp)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoachMode.values().forEach { m ->
                val sel = m == coach.mode
                Box(
                    Modifier.clip(Pill).background(if (sel) GfColor.Accent else GfColor.Surface)
                        .border(1.dp, if (sel) GfColor.Accent else GfColor.Hairline08, Pill)
                        .clickable(remember { MutableInteractionSource() }, indication = null) { vm.setCoachMode(m) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(m.label, color = if (sel) GfColor.OnAccent else GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp)
                }
            }
        }
        Text(coach.mode.blurb, color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 30.dp)) {
            when {
                !ready -> Centered("lock", "Set up an AI provider in Settings to use the coach", "It sends your workout summary (and, for a critique, that session's sets) to the provider you chose, with your own key.") {
                    Button("Open Settings") { vm.closeCoach(); vm.openSettings() }
                }
                coach.loading -> Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GfColor.Accent, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                    Text("Coach is looking at your log…", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.5.sp, modifier = Modifier.padding(top = 16.dp))
                }
                coach.error != null -> Centered("warning", "Couldn't reach the coach", coach.error!!) { Button("Try again") { vm.runCoach() } }
                coach.text != null -> {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(20.dp)).padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(msIcon("psychology"), null, tint = GfColor.Accent, modifier = Modifier.size(20.dp))
                            Text("Coach's take", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp)
                        }
                        Text(coach.text!!, color = GfColor.TextCue, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                    if (coach.suggestions.isNotEmpty()) {
                        Text("Proposed targets", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                        coach.suggestions.forEach { s ->
                            val ex = data.exercise(s.exerciseId) ?: return@forEach
                            val bw = isBW(ex.equipment, ex.reps)
                            val done = s.exerciseId in coach.accepted
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(16.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(ex.name, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W600, fontSize = 14.5.sp)
                                    Text(
                                        (if (bw) "${s.value.toInt()} reps" else "${Units.fmtDisplay(s.value, settings.units)} ${settings.units}") + " · ${s.weeks} weeks",
                                        color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp,
                                    )
                                }
                                val press = remember { MutableInteractionSource() }
                                Box(
                                    Modifier.pressScale(press, 0.96f).clip(Pill).background(if (done) GfColor.Surface else GfColor.Accent)
                                        .border(1.dp, if (done) GfColor.Hairline10 else GfColor.Accent, Pill)
                                        .clickable(press, indication = null, enabled = !done) { vm.acceptSuggestedTarget(s) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text(if (done) "Set ✓" else "Set as target", color = if (done) GfColor.TextDim else GfColor.OnAccent, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Text(
                        "Ask again", color = GfColor.Accent, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) { vm.runCoach() }.padding(vertical = 14.dp), textAlign = TextAlign.Center,
                    )
                }
                else -> Centered("psychology", "Ready when you are", coach.mode.blurb) { Button("Ask the coach") { vm.runCoach() } }
            }
        }
    }
}

@Composable
private fun Centered(icon: String, title: String, body: String, action: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(msIcon(icon), null, tint = if (icon == "warning") GfColor.Coral else GfColor.Accent, modifier = Modifier.size(40.dp))
        Text(title, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 17.sp, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
        Text(body, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.5.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(22.dp))
        action()
    }
}

@Composable
private fun Button(label: String, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    Row(
        Modifier.height(50.dp).pressScale(press, 0.97f).clip(RoundedCornerShape(16.dp)).background(GfColor.Accent)
            .clickable(press, indication = null, onClick = onClick).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
        Spacer(Modifier.width(0.dp))
    }
}
