package com.getfit.ui.detail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.getfit.core.Constants
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.GfDifficulty
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.Pill
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.MuscleIcon
import com.getfit.core.ui.ShimmerBox
import com.getfit.core.ui.msIcon
import com.getfit.core.ui.pressScale
import com.getfit.domain.DAY_MS
import com.getfit.domain.fmtW
import com.getfit.domain.isBW
import com.getfit.domain.targetDaysLeft
import com.getfit.domain.targetPct
import com.getfit.ui.AppViewModel
import kotlin.math.roundToInt

@Composable
fun DetailScreen(vm: AppViewModel, id: String) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()
    val ex = data.exercise(id) ?: return
    val bw = isBW(ex.equipment, ex.reps)
    val units = settings.units
    val best = data.bestMap[id]
    val now = System.currentTimeMillis()
    val cnt = data.logs.count { it.exerciseId == id }
    val target = data.targets.firstOrNull { it.exId == id }
    val inPlan = data.plan.any { it.id == id }
    val cues = remember(ex.cues) { ex.cues.split("||").filter { it.isNotBlank() } }

    Column(Modifier.fillMaxSize().background(GfColor.Background).statusBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            DemoStage(mediaId = ex.mediaId, gifUrl = Constants.gifUrl(ex.mediaId), muscle = ex.muscle, onClose = vm::closeDetail)

            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(ex.name, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 25.sp, lineHeight = 29.sp)

                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(msIcon("exercise"), ex.muscle)
                    InfoChip(msIcon("fitness_center"), ex.equipment)
                    Box(Modifier.clip(Pill).background(GfDifficulty.bg(ex.level)).padding(horizontal = 12.dp, vertical = 7.dp)) {
                        Text(ex.level, color = GfDifficulty.color(ex.level), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.5.sp)
                    }
                }

                // PR + last
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(
                        "Personal best",
                        if (best != null) (if (bw) "${best.reps} reps" else "${fmtW(best.weight)} $units × ${best.reps}") else "Not yet set",
                        if (best != null) (if (bw) "Best set logged" else "Est. 1RM ${best.e1rm} $units") else "Log a set to start tracking",
                        Modifier.weight(1.35f),
                    )
                    StatBox(
                        "Last trained",
                        if (best != null) ago(best.dateMs, now) else "—",
                        if (cnt > 0) "$cnt ${if (cnt == 1) "set logged" else "sets logged"}" else "No history",
                        Modifier.weight(1f),
                    )
                }

                if (target != null) {
                    val cur = best?.let { if (bw) it.reps.toDouble() else it.weight } ?: target.start
                    val pct = targetPct(cur, target.start, target.target)
                    val remain = (target.target - cur).coerceAtLeast(0.0).let { Math.round(it * 10) / 10.0 }
                    val days = targetDaysLeft(target.startDMs, target.weeks, now).coerceAtLeast(0)
                    Column(Modifier.padding(top = 10.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x12CBF25C)).border(1.dp, Color(0x29CBF25C), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(msIcon("flag"), null, tint = GfColor.Lime, modifier = Modifier.size(18.dp))
                                Text("Target ${if (bw) "${target.target.roundToInt()} reps" else "${fmtW(target.target)} $units"}", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.5.sp)
                            }
                            Text("${(pct * 100).roundToInt()}%", color = GfColor.Lime, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 13.sp)
                        }
                        ProgressBar(pct.toFloat(), Modifier.padding(top = 10.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (remain <= 0) "Target reached" else "+${if (bw) remain.roundToInt() else fmtW(remain)} ${if (bw) "reps" else units} to go", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.5.sp)
                            Text("$days days left", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.5.sp)
                        }
                    }
                } else {
                    val ts = remember { MutableInteractionSource() }
                    Row(
                        Modifier.padding(top = 10.dp).fillMaxWidth().height(48.dp).pressScale(ts, 0.98f)
                            .clip(RoundedCornerShape(14.dp)).border(1.5.dp, Color(0x66CBF25C), RoundedCornerShape(14.dp))
                            .clickable(ts, indication = null) { vm.openGoal(id) },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(msIcon("flag"), null, tint = GfColor.Lime, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Set a target", color = GfColor.Lime, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp)
                    }
                }

                Text("How to perform", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
                cues.forEachIndexed { i, c ->
                    Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        Box(Modifier.size(26.dp).clip(RoundedCornerShape(999.dp)).background(GfColor.LimeFill12), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = GfColor.Lime, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 13.sp)
                        }
                        Text(c, color = GfColor.TextCue, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
            }
        }

        // bottom actions
        Row(
            Modifier.fillMaxWidth().background(GfColor.Background).border(1.dp, GfColor.Hairline06).padding(horizontal = 20.dp, vertical = 14.dp).navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val addI = remember { MutableInteractionSource() }
            Row(
                Modifier.weight(1f).height(54.dp).pressScale(addI, 0.97f).clip(RoundedCornerShape(16.dp)).background(GfColor.SurfaceElevated).border(1.dp, GfColor.Hairline08, RoundedCornerShape(16.dp)).clickable(addI, indication = null) { vm.addToPlan(id) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(msIcon(if (inPlan) "check" else "add"), null, tint = GfColor.Lime, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (inPlan) "In workout" else "Add", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
            }
            val startI = remember { MutableInteractionSource() }
            Row(
                Modifier.weight(1f).height(54.dp).pressScale(startI, 0.97f).clip(RoundedCornerShape(16.dp)).background(GfColor.Lime).clickable(startI, indication = null) { vm.startSingle(id, ex.reps) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(msIcon("play_arrow"), null, tint = GfColor.OnAccent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
            }
        }
    }
}

@Composable
private fun DemoStage(mediaId: String?, gifUrl: String, muscle: String, onClose: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "demo")
    val angle by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart), label = "ring")
    val bob by infinite.animateFloat(-10f, 10f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "bob")
    val glow by infinite.animateFloat(0.05f, 0.22f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "glow")

    Box(
        Modifier.fillMaxWidth().height(300.dp)
            .background(Brush.radialGradient(listOf(GfColor.SurfaceElevated, GfColor.Background), center = Offset(0.5f, 0f), radius = 900f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(210.dp).rotate(angle)) {
            drawCircle(color = Color(0x29CBF25C), radius = size.minDimension / 2, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f))))
        }
        Box(Modifier.size(150.dp).background(Brush.radialGradient(listOf(GfColor.Lime.copy(alpha = glow), Color.Transparent)), RoundedCornerShape(999.dp)))

        if (gifUrl.isNotBlank()) {
            Box(Modifier.size(width = 248.dp, height = 232.dp).clip(RoundedCornerShape(22.dp))) {
                ShimmerBox(Modifier.fillMaxSize())
                AsyncImage(model = gifUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        } else {
            Box(
                Modifier.size(width = 248.dp, height = 232.dp).clip(RoundedCornerShape(22.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline08, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MuscleIcon.of(muscle), null, tint = GfColor.Lime, modifier = Modifier.offset(y = bob.dp).size(72.dp))
            }
        }

        // back button
        val back = remember { MutableInteractionSource() }
        Box(
            Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 14.dp).size(44.dp).pressScale(back, 0.9f).clip(RoundedCornerShape(999.dp)).background(Color(0x99171210)).border(1.dp, GfColor.Hairline10, RoundedCornerShape(999.dp)).clickable(back, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(msIcon("arrow_back"), null, tint = GfColor.Text, modifier = Modifier.size(24.dp))
        }
        // caption
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).clip(Pill).background(Color(0xB8171210)).border(1.dp, GfColor.Hairline10, Pill).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(msIcon("smart_display"), null, tint = GfColor.Lime, modifier = Modifier.size(14.dp))
            Text("media_id · ${mediaId ?: "not set"}", color = GfColor.TextDim, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W600, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        Modifier.clip(Pill).background(GfColor.Surface).border(1.dp, GfColor.Hairline08, Pill).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = GfColor.Lime, modifier = Modifier.size(16.dp))
        Text(text, color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatBox(label: String, main: String, sub: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text(label.uppercase(), color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.5.sp)
        Text(main, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 18.sp, modifier = Modifier.padding(top = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ProgressBar(pct: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(7.dp).clip(Pill).background(GfColor.Hairline10)) {
        Box(Modifier.fillMaxWidth(pct.coerceIn(0f, 1f)).height(7.dp).clip(Pill).background(GfColor.Lime))
    }
}

private fun ago(dateMs: Long, now: Long): String {
    val n = ((now - dateMs).toDouble() / DAY_MS).roundToInt()
    return when {
        n <= 0 -> "Today"; n == 1 -> "1d ago"; else -> "${n}d ago"
    }
}
