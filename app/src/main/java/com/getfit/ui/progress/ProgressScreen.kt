package com.getfit.ui.progress

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.ProgressRing
import com.getfit.core.ui.msIcon
import com.getfit.domain.DAY_MS
import com.getfit.domain.SessionRecord
import com.getfit.domain.Units
import com.getfit.domain.floorDayLocal
import com.getfit.domain.fmtVol
import com.getfit.domain.isBW
import com.getfit.domain.streakCount
import com.getfit.domain.targetDaysLeft
import com.getfit.domain.targetPct
import com.getfit.domain.weekAgg
import com.getfit.domain.weekStartLocal
import com.getfit.ui.AppViewModel
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(vm: AppViewModel) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()
    val nav by vm.nav.collectAsState()
    val units = settings.units
    val now = System.currentTimeMillis()

    val week = weekAgg(
        data.sessions.map { SessionRecord(it.id, it.dateMs, it.name, it.durationSec, it.totalSets, it.volume, it.prs) },
        weekStartLocal(now),
    )
    val weekGoal = 5
    val weekDone = week.workouts
    val goalMsg = if (weekDone >= weekGoal) "You hit your weekly target — great work."
    else "${weekGoal - weekDone} more ${if (weekGoal - weekDone == 1) "session" else "sessions"} to hit your goal."

    val streak = streakCount(data.sessions.map { it.dateMs }, now, ::floorDayLocal)
    val totalVol = data.sessions.sumOf { it.volume }
    val monthStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val prMonth = data.sessions.filter { it.dateMs >= monthStart }.sumOf { it.prs }

    val records = run {
        data.bestMap.mapNotNull { (id, b) ->
            val ex = data.exercise(id) ?: return@mapNotNull null
            Triple(ex, b, if (b.bodyweight) b.reps else b.e1rm + 1000)
        }.sortedByDescending { it.third }.take(6)
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 108.dp),
    ) {
        Text("Progress", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 26.sp, modifier = Modifier.padding(vertical = 6.dp).padding(bottom = 12.dp))

        // weekly goal ring
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(GfColor.SurfaceElevated, GfColor.SurfaceElevatedAlt))).border(1.dp, GfColor.Hairline06, RoundedCornerShape(24.dp)).padding(22.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ProgressRing(progress = if (nav.charted) weekDone / weekGoal.toFloat() else 0f, size = 104.dp, strokeWidth = 10.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$weekDone", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 24.sp)
                    Text("of $weekGoal", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 10.sp)
                }
            }
            Column {
                Text("Weekly goal", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 18.sp)
                Text(goalMsg, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // volume chart
        Column(
            Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Volume this week", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp)
                Text("${Units.volDisplay(week.totalVolume, units)} $units", color = GfColor.Lime, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.5.sp)
            }
            val todayIdx = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
            val maxV = (week.dayVolume.maxOrNull() ?: 0).coerceAtLeast(1)
            val labels = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                labels.forEachIndexed { i, d ->
                    val frac = week.dayVolume[i].toFloat() / maxV
                    val h by animateFloatAsState(if (nav.charted) frac else 0f, tween(1000), label = "bar$i")
                    val isToday = i == todayIdx
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                Modifier.fillMaxWidth().fillMaxHeight(h.coerceIn(if (week.dayVolume[i] == 0) 0f else 0.05f, 1f)).clip(RoundedCornerShape(7.dp))
                                    .background(if (isToday) GfColor.Lime else if (week.dayVolume[i] == 0) GfColor.Hairline06 else GfColor.Lime.copy(alpha = 0.35f)),
                            )
                        }
                        Text(d, color = if (isToday) GfColor.Lime else GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 10.5.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }

        // stat grid
        Column(Modifier.padding(top = 14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgStat("local_fire_department", GfColor.Amber, "Streak", "$streak ${if (streak == 1) "day" else "days"}", Modifier.weight(1f))
                ProgStat("exercise", GfColor.Lime, "Total workouts", data.sessions.size.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgStat("monitoring", GfColor.Lime, "Total volume", "${fmtVol(Units.volDisplay(totalVol, units))} $units", Modifier.weight(1f))
                ProgStat("emoji_events", GfColor.Amber, "PRs this month", prMonth.toString(), Modifier.weight(1f))
            }
        }

        // AI review entry point
        Row(
            Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.AccentGradient)
                .clickable(remember { MutableInteractionSource() }, indication = null) { vm.openAiReview() }.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x24171210)), contentAlignment = Alignment.Center) {
                Icon(msIcon("smart_display"), null, tint = GfColor.OnAccent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("AI review", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp)
                Text("Get a coach's read on your recent training", color = GfColor.OnAccentSub, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(msIcon("chevron_right"), null, tint = GfColor.OnAccentSub, modifier = Modifier.size(20.dp))
        }

        // personal records
        Row(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Personal records", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp)
            Icon(msIcon("emoji_events"), null, tint = GfColor.Amber, modifier = Modifier.size(20.dp))
        }
        records.forEach { (ex, b, _) ->
            val recent = (now - b.dateMs) < 14 * DAY_MS
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { vm.openDetail(ex.id) }.padding(13.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x21F3B24A)), contentAlignment = Alignment.Center) {
                    Icon(msIcon("military_tech"), null, tint = GfColor.Amber, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(ex.name, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W600, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (b.bodyweight) "${ex.muscle} · best set" else "Est. 1RM ${Units.toDisplay(b.e1rm.toDouble(), units).roundToInt()} $units", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (b.bodyweight) "${b.reps} reps" else "${Units.fmtDisplay(b.weight, units)} $units × ${b.reps}", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 14.sp)
                    if (recent) Text("NEW PR", color = GfColor.Amber, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 9.5.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        // goals & targets
        Row(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Goals & targets", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { vm.openNewGoal() }) {
                Icon(msIcon("add"), null, tint = GfColor.Lime, modifier = Modifier.size(18.dp))
                Text("New goal", color = GfColor.Lime, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.5.sp)
            }
        }
        val targets = data.targets.filter { data.exercise(it.exId) != null }
        if (targets.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).border(1.5.dp, Color(0x66CBF25C), RoundedCornerShape(16.dp)).clickable(remember { MutableInteractionSource() }, indication = null) { vm.openNewGoal() },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(msIcon("flag"), null, tint = GfColor.Lime, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Set your first target", color = GfColor.Lime, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp)
            }
        } else {
            targets.forEach { t ->
                val ex = data.exercise(t.exId)!!
                val bw = isBW(ex.equipment, ex.reps)
                val b = data.bestMap[t.exId]
                val cur = b?.let { if (bw) it.reps.toDouble() else it.weight } ?: t.start
                val pct = targetPct(cur, t.start, t.target)
                val days = targetDaysLeft(t.startDMs, t.weeks, now)
                val remain = (t.target - cur).coerceAtLeast(0.0).let { Math.round(it * 10) / 10.0 }
                val done = pct >= 1.0
                Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 15.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(ex.name, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W600, fontSize = 14.5.sp)
                        Icon(msIcon("close"), null, tint = GfColor.TextFaint, modifier = Modifier.size(18.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.deleteTarget(t.id) })
                    }
                    Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(if (bw) "${cur.roundToInt()} reps" else "${Units.fmtDisplay(cur, units)} $units", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 21.sp)
                        Icon(msIcon("arrow_forward"), null, tint = GfColor.TextFaint, modifier = Modifier.size(16.dp).padding(bottom = 3.dp))
                        Text(if (bw) "${t.target.roundToInt()} reps" else "${Units.fmtDisplay(t.target, units)} $units", color = GfColor.Lime, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp)
                    }
                    Box(Modifier.padding(top = 11.dp).fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)).background(GfColor.Hairline10)) {
                        val w by animateFloatAsState(if (nav.charted) pct.toFloat() else 0f, tween(1000), label = "tgt")
                        Box(Modifier.fillMaxWidth(w).height(7.dp).clip(RoundedCornerShape(999.dp)).background(GfColor.Lime))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (done) "Reached — nice!" else "+${if (bw) remain.roundToInt().toString() else Units.fmtDisplay(remain, units)} ${if (bw) "reps" else units} to go", color = if (done) GfColor.Lime else GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.5.sp)
                        Text(if (days <= 0) "Due now" else "${days}d left", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.5.sp)
                    }
                }
            }
        }

        // recent sessions
        Text("Recent sessions", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp, modifier = Modifier.padding(top = 26.dp, bottom = 12.dp))
        if (data.sessions.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline10, RoundedCornerShape(20.dp)).padding(vertical = 32.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(msIcon("history"), null, tint = GfColor.TextFaint, modifier = Modifier.size(34.dp))
                Text("No workouts yet", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp)
                Text("Finish a session and it'll show up here with your time, sets and volume.", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.5.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
            }
        } else {
            data.sessions.take(5).forEach { h ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp)).padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(GfColor.LimeFill12), contentAlignment = Alignment.Center) {
                        Icon(msIcon("check_circle"), null, tint = GfColor.Lime, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(h.name, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W600, fontSize = 14.5.sp)
                        Text("${(h.durationSec / 60).coerceAtLeast(1)} min · ${h.totalSets} sets · ${if (h.volume > 0) "${Units.volDisplay(h.volume, units)} $units" else "bodyweight"}", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(relDay(h.dateMs, now), color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProgStat(icon: String, tint: Color, label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(msIcon(icon), null, tint = tint, modifier = Modifier.size(19.dp))
            Text(label, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        }
        Text(value, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 24.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

private fun relDay(ts: Long, now: Long): String {
    val floorNow = dayFloor(now)
    val diff = ((floorNow - dayFloor(ts)) / DAY_MS).toInt()
    return when {
        diff <= 0 -> "Today"
        diff == 1 -> "Yesterday"
        diff < 7 -> arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")[Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.DAY_OF_WEEK) - 1]
        else -> "${diff}d ago"
    }
}

private fun dayFloor(ts: Long): Long = Calendar.getInstance().apply {
    timeInMillis = ts; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
