package com.getfit.ui.progress

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.GfSheet
import com.getfit.core.ui.GfSheetRow
import com.getfit.core.ui.msIcon
import com.getfit.data.db.HeartRateSampleEntity
import com.getfit.data.db.SessionSetEntity
import com.getfit.domain.ExerciseDelta
import com.getfit.domain.LoggedSet
import com.getfit.domain.SessionSetRow
import com.getfit.domain.Units
import com.getfit.domain.exerciseDeltas
import com.getfit.domain.fmtDur
import com.getfit.domain.isBW
import com.getfit.domain.muscleSplit
import com.getfit.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hevy's Workout Detail: the stats strip, muscle split, the watch's heart-rate trace, then every
 * exercise's sets with how its top set moved against the previous time it was done.
 */
@Composable
fun WorkoutDetailScreen(vm: AppViewModel, id: String) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()
    val units = settings.units
    val session = data.sessions.firstOrNull { it.id == id } ?: return
    val sets = remember(data.sessionSets, id) { data.sessionSets.filter { it.sessionId == id } }
    var hr by remember(id) { mutableStateOf<List<HeartRateSampleEntity>>(emptyList()) }
    LaunchedEffect(id) { hr = vm.sessionHeartRate(id) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val rows = remember(sets) { sets.map { SessionSetRow(it.exerciseId, it.name, it.weight, it.reps) } }
    val bwOf: (String) -> Boolean = { exId -> data.exercise(exId)?.let { isBW(it.equipment, it.reps) } ?: (rows.filter { it.exerciseId == exId }.all { it.weightKg == 0.0 }) }
    val deltas = remember(rows, data.logs) {
        exerciseDeltas(session.dateMs, rows, data.logs.map { LoggedSet(it.exerciseId, it.weight, it.reps, it.dateMs) }, bwOf)
    }
    val split = remember(rows, data.exercises) { muscleSplit(rows) { exId -> data.exercise(exId)?.muscle } }
    val ordinal = remember(data.sessions, id) { data.sessions.count { it.dateMs <= session.dateMs } }
    val df = remember { SimpleDateFormat("EEEE, d MMM yyyy · h:mm a", Locale.getDefault()) }

    Box(Modifier.fillMaxSize().background(GfColor.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(msIcon("arrow_back"), null, tint = GfColor.Text, modifier = Modifier.size(26.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.closeSessionDetail() })
                Text("Workout", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Icon(msIcon("more_horiz"), null, tint = GfColor.Text, modifier = Modifier.size(26.dp).clickable(remember { MutableInteractionSource() }, indication = null) { menu = true })
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 40.dp)) {
                Text(session.name, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 26.sp)
                Text(df.format(Date(session.dateMs)) + " · workout #$ordinal", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))

                // stats grid
                val stats = buildList {
                    add(Triple("timer", "Time", if (session.durationSec > 0) fmtDur(session.durationSec) else "—"))
                    add(Triple("trending_up", "Volume", if (session.volume > 0) "${Units.volDisplay(session.volume, units)} $units" else "bodyweight"))
                    add(Triple("exercise", "Sets", session.totalSets.toString()))
                    add(Triple("emoji_events", "Records", session.prs.toString()))
                    if (session.avgBpm > 0) add(Triple("favorite", "Avg bpm", "${session.avgBpm}"))
                    if (session.calories > 0) add(Triple("local_fire_department", "Calories", "~${session.calories}"))
                }
                stats.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (icon, label, value) ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(16.dp)).padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(msIcon(icon), null, tint = if (icon == "favorite") GfColor.Coral else GfColor.Accent, modifier = Modifier.size(15.dp))
                                    Text(label, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.sp)
                                }
                                Text(value, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                // muscle split
                if (split.isNotEmpty()) {
                    SectionTitle("Muscle split")
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        split.take(5).forEach { (muscle, pct) ->
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(muscle, color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp)
                                    Text("$pct%", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                                }
                                Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(GfColor.Hairline08)) {
                                    Box(Modifier.fillMaxWidth(pct / 100f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(GfColor.Accent))
                                }
                            }
                        }
                    }
                }

                // heart rate trace
                if (hr.size >= 2) {
                    SectionTitle("Heart rate")
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp)).padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Avg ${session.avgBpm} · Max ${session.maxBpm} bpm", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                            Text("from your watch", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.5.sp)
                        }
                        HeartRateChart(hr, Modifier.fillMaxWidth().height(110.dp).padding(top = 12.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("0 min", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.sp)
                            Text(fmtDur(((hr.last().atMs - hr.first().atMs) / 1000).toInt()), color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.sp)
                        }
                    }
                }

                // exercises
                SectionTitle("Exercises")
                val grouped = remember(sets) { sets.groupBy { it.exerciseId } }
                grouped.forEach { (exId, exSets) ->
                    ExerciseBlock(
                        name = exSets.first().name, sets = exSets, bw = bwOf(exId), units = units, delta = deltas[exId],
                        onOpen = { if (data.exercise(exId) != null) vm.openDetail(exId) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        if (menu) {
            GfSheet(title = session.name, onDismiss = { menu = false; confirmDelete = false }) {
                GfSheetRow(msIcon("playlist_add_check"), "Save as routine") { vm.saveSessionAsRoutine(id); menu = false }
                GfSheetRow(msIcon(if (confirmDelete) "warning" else "delete"), if (confirmDelete) "Tap again to delete this workout" else "Delete workout", destructive = true) {
                    if (confirmDelete) { vm.deleteSession(id); menu = false } else confirmDelete = true
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
}

@Composable
private fun ExerciseBlock(name: String, sets: List<SessionSetEntity>, bw: Boolean, units: String, delta: ExerciseDelta?, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp)).padding(14.dp)) {
        Row(Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null, onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = GfColor.Accent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.5.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            delta?.let { DeltaPill(it, units) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)) {
            Text("SET", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.5.sp, letterSpacing = 1.sp, modifier = Modifier.width(44.dp))
            Text(if (bw) "REPS" else "WEIGHT & REPS", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.5.sp, letterSpacing = 1.sp)
        }
        sets.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 14.sp, modifier = Modifier.width(44.dp))
                Text(
                    if (bw) "${s.reps} reps" else "${Units.fmtDisplay(Units.toDisplay(s.weight, units), units)} $units × ${s.reps}",
                    color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.sp,
                )
            }
        }
    }
}

/** "▲ +2.5 kg" / "▼ −1 rep" / "= same" / "first time" against the previous session with this lift. */
@Composable
private fun DeltaPill(d: ExerciseDelta, units: String) {
    val delta = d.delta
    val (label, color) = when {
        delta == null -> "first time" to GfColor.TextFaint
        delta > 0.0001 -> "▲ +${fmtDelta(delta, d.bodyweight, units)}" to GfColor.Text
        delta < -0.0001 -> "▼ ${fmtDelta(delta, d.bodyweight, units)}" to GfColor.TextFaint
        else -> "= same" to GfColor.TextDim
    }
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(GfColor.Hairline08).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(label, color = color, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.sp)
    }
}

private fun fmtDelta(deltaKgOrReps: Double, bw: Boolean, units: String): String =
    if (bw) "${deltaKgOrReps.toInt()} ${if (Math.abs(deltaKgOrReps) == 1.0) "rep" else "reps"}"
    else "${Units.fmtDisplay(Units.toDisplay(deltaKgOrReps, units), units)} $units"

/** Area chart of the watch trace; y is scaled between the session's min and max bpm. */
@Composable
private fun HeartRateChart(samples: List<HeartRateSampleEntity>, modifier: Modifier) {
    val minB = samples.minOf { it.bpm }.toFloat()
    val maxB = samples.maxOf { it.bpm }.toFloat().coerceAtLeast(minB + 1f)
    val t0 = samples.first().atMs
    val span = (samples.last().atMs - t0).coerceAtLeast(1L).toFloat()
    Row(modifier) {
        Canvas(Modifier.weight(1f).fillMaxSize()) {
            val w = size.width; val h = size.height
            fun x(s: HeartRateSampleEntity) = (s.atMs - t0) / span * w
            fun y(s: HeartRateSampleEntity) = h - (s.bpm - minB) / (maxB - minB) * (h - 6f) - 3f
            val line = Path().apply {
                samples.forEachIndexed { i, s -> if (i == 0) moveTo(x(s), y(s)) else lineTo(x(s), y(s)) }
            }
            val area = Path().apply {
                addPath(line); lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(area, GfColor.Coral.copy(alpha = 0.18f))
            drawPath(line, GfColor.Coral, style = Stroke(width = 3f, cap = StrokeCap.Round))
            // faint mid gridline
            drawLine(Color(0x22FFFFFF), Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1f)
        }
        Column(Modifier.padding(start = 8.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("${maxB.toInt()}", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.sp)
            Text("${minB.toInt()}", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.sp)
        }
    }
}
