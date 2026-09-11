package com.getfit.ui.session

import android.view.SoundEffectConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.Pill
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.ProgressRing
import com.getfit.core.ui.msIcon
import com.getfit.core.ui.pressScale
import com.getfit.domain.Phase
import com.getfit.domain.Plates
import com.getfit.domain.Units
import com.getfit.domain.platesFor
import com.getfit.domain.warmupRamp
import com.getfit.domain.SessionState
import com.getfit.domain.fmtClock
import com.getfit.domain.fmtVol
import com.getfit.domain.fmtW
import com.getfit.domain.initReps
import com.getfit.domain.isTimeBased
import com.getfit.domain.nextPosition
import com.getfit.domain.prPace
import com.getfit.ui.AppViewModel

@Composable
fun SessionScreen(vm: AppViewModel) {
    val s by vm.session.collectAsState()
    val settings by vm.settings.collectAsState()
    val state = s ?: return

    Box(Modifier.fillMaxSize().background(if (state.phase == Phase.DONE) GfColor.Accent else GfColor.Background)) {
        if (state.phase == Phase.DONE) DoneView(state, settings.units, vm::endSession)
        else ActiveView(state, settings.units, vm)
    }
}

@Composable
private fun ActiveView(s: SessionState, units: String, vm: AppViewModel) {
    val it = s.current
    val isRest = s.phase == Phase.REST
    val ctrl = vm.sessionController
    val settings by vm.settings.collectAsState()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val tick = { if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    val click = { if (settings.sound) view.playSoundEffect(SoundEffectConstants.CLICK) }
    val ringProgress = if (isRest) s.restLeft / s.restTotal.coerceAtLeast(1).toFloat()
    else s.completedSets / s.totalSets.coerceAtLeast(1).toFloat()

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp)) {
        // top bar
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(msIcon("close"), null, tint = GfColor.Text, modifier = Modifier.size(28.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.endSession() })
            Text("Exercise ${s.idx + 1} of ${s.items.size}", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp)
            Icon(msIcon(if (s.paused) "play_arrow" else "pause"), null, tint = GfColor.Text, modifier = Modifier.size(28.dp).clickable(remember { MutableInteractionSource() }, indication = null) { ctrl.togglePause() })
        }
        // segment bar
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            s.items.forEachIndexed { i, _ ->
                val c = when { i < s.idx -> GfColor.Accent; i == s.idx -> GfColor.Accent.copy(alpha = 0.45f); else -> GfColor.Hairline10 }
                Box(Modifier.weight(1f).height(5.dp).clip(Pill).background(c))
            }
        }

        // ring
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            ProgressRing(
                progress = ringProgress, size = 210.dp, strokeWidth = 11.dp,
                color = if (isRest) GfColor.Amber else GfColor.Accent,
                trackColor = GfColor.Hairline08,
                animationSpec = if (isRest) tween(1000, easing = LinearEasing) else tween(600),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isRest) "REST" else "SET ${s.setNum} OF ${it.sets}",
                        color = if (isRest) GfColor.Amber else GfColor.Accent, fontFamily = Manrope,
                        fontWeight = FontWeight.W800, fontSize = 11.5.sp, letterSpacing = 1.5.sp,
                    )
                    Text(
                        if (isRest) fmtClock(s.restLeft.coerceAtLeast(0)) else initReps(it.reps).toString(),
                        color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700,
                        fontSize = if (isRest) 46.sp else 50.sp, modifier = Modifier.padding(top = 5.dp),
                    )
                    Text(
                        if (isRest) "until next set" else if (isTimeBased(it.reps)) "target seconds" else "target reps",
                        color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            Text(it.name, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 20.sp, modifier = Modifier.padding(top = 18.dp))
            // Live heart rate arrives from the watch in ~8 s batches (PhoneWearSync); null until the
            // first batch, or when no watch is paired, in which case the line reads as before.
            val bpm by vm.liveHeartRateBpm.collectAsState()
            val hr = bpm?.let { b -> " · ♥ ${b.toInt()} bpm" } ?: ""
            Text("${it.muscle} · ${fmtClock(s.elapsed)} elapsed$hr", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
            // Superset partner coming up with no rest: say so before the user reaches for the timer.
            if (!isRest) nextPosition(s)?.takeIf { p -> !p.restFirst }?.let { p ->
                Text("Superset · then ${s.items[p.idx].name}, no rest", color = GfColor.Accent, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        if (isRest) {
            val nextName = nextPosition(s)?.let { p -> s.items[p.idx].name } ?: "Finish"
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RestSideButton("−15") { tick(); ctrl.addRest(-15) }
                Row(
                    Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).background(GfColor.Accent)
                        .clickable(remember { MutableInteractionSource() }, indication = null) { tick(); click(); ctrl.skip() },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Skip rest", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(msIcon("skip_next"), null, tint = GfColor.OnAccent, modifier = Modifier.size(22.dp))
                }
                RestSideButton("+15") { tick(); ctrl.addRest(15) }
            }
            Text("Up next · $nextName", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp))
        } else {
            // Load tools — barbell lifts only: which plates make the current weight, and (first set
            // only) a warm-up ramp to it. Both are pure functions of the weight on screen.
            if (!it.bw && it.equipment == "Barbell") {
                val bar = Plates.barFor(units)
                val load = platesFor(s.curW, bar, Plates.setFor(units))
                val ramp = if (s.setNum == 1) warmupRamp(s.curW, bar, Units.step(units)) else emptyList()
                Column(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (load != null) {
                        val plates = if (load.perSide.isEmpty()) "empty bar" else load.perSide.joinToString(" · ") { fmtW(it) }
                        val tail = if (!load.exact) "  (+${fmtW(load.remainder)} short)" else ""
                        Text("Per side  $plates$tail", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.5.sp)
                    }
                    if (ramp.isNotEmpty()) {
                        Text(
                            "Warm-up  " + ramp.joinToString(" · ") { w -> "${fmtW(w.weight)}×${w.reps}" },
                            color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 11.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!it.bw) {
                    StepperCard(label = "Weight", value = fmtW(s.curW), unit = units, prBadge = prPace(s), modifier = Modifier.weight(1f), onDec = { tick(); ctrl.decW() }, onInc = { tick(); ctrl.incW() })
                }
                StepperCard(label = if (isTimeBased(it.reps)) "Seconds" else "Reps", value = s.curR.toString(), unit = null, prBadge = false, modifier = Modifier.weight(1f), onDec = { tick(); ctrl.decR() }, onInc = { tick(); ctrl.incR() })
            }
            Row(
                Modifier.fillMaxWidth().height(58.dp).pressScale(remember { MutableInteractionSource() }, 0.98f)
                    .clip(RoundedCornerShape(18.dp)).background(GfColor.Accent)
                    .clickable(remember { MutableInteractionSource() }, indication = null) { tick(); click(); ctrl.doneSet() },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(msIcon("check"), null, tint = GfColor.OnAccent, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(9.dp))
                Text("Log set & rest", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun RestSideButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(GfColor.SurfaceElevated).border(1.dp, GfColor.Hairline08, RoundedCornerShape(16.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 13.sp)
    }
}

@Composable
private fun StepperCard(label: String, value: String, unit: String?, prBadge: Boolean, modifier: Modifier, onDec: () -> Unit, onInc: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline08, RoundedCornerShape(18.dp)).padding(horizontal = 8.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label.uppercase(), color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.5.sp)
            if (prBadge) {
                Box(Modifier.clip(Pill).background(GfColor.Accent).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("PR", color = GfColor.OnAccent, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 9.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            StepHit("remove_circle", onDec)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 24.sp)
                if (unit != null) Text(unit, color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 10.sp)
            }
            StepHit("add_circle", onInc)
        }
    }
}

/** 30dp circular stepper icon with a 44dp touch target. */
@Composable
private fun StepHit(icon: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.size(44.dp).pressScale(interaction, 0.86f).clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(msIcon(icon), null, tint = GfColor.Accent, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun DoneView(s: SessionState, units: String, onFinish: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(999.dp)).background(GfColor.OnAccent), contentAlignment = Alignment.Center) {
            Icon(msIcon("check"), null, tint = GfColor.Accent, modifier = Modifier.size(54.dp))
        }
        Text("Workout complete", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 28.sp, modifier = Modifier.padding(top = 22.dp))
        Text("${s.name} · logged & saved", color = GfColor.OnAccentSub, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))

        Row(Modifier.padding(top = 26.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            DoneStat(fmtClock(s.elapsed), "time")
            DoneStat(s.completedSets.toString(), "sets")
            DoneStat(if (s.volume > 0) fmtVol(Math.round(s.volume).toInt()) else "—", "volume")
        }

        if (s.newPRs.isNotEmpty()) {
            Column(Modifier.padding(top = 24.dp).width(264.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NEW PERSONAL RECORDS", color = GfColor.OnAccentSub, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp, letterSpacing = 1.sp)
                s.newPRs.forEach { pr ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GfColor.OnAccent).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(msIcon("local_fire_department"), null, tint = GfColor.Amber, modifier = Modifier.size(20.dp))
                        Text(pr.name, color = GfColor.OnLight, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                        Text(pr.value, color = GfColor.Accent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 13.5.sp)
                    }
                }
            }
        }

        Box(
            Modifier.padding(top = 32.dp).height(54.dp).width(230.dp).pressScale(remember { MutableInteractionSource() }, 0.97f)
                .clip(RoundedCornerShape(16.dp)).background(GfColor.OnAccent)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onFinish),
            contentAlignment = Alignment.Center,
        ) {
            Text("Save & finish", color = GfColor.Accent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp)
        }
    }
}

@Composable
private fun DoneStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 24.sp)
        Text(label, color = GfColor.OnAccentDim2, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.sp)
    }
}
