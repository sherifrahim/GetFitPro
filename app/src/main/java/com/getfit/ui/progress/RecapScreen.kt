package com.getfit.ui.progress

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.Segmented
import com.getfit.core.ui.msIcon
import com.getfit.domain.LoggedSet
import com.getfit.domain.RecapSession
import com.getfit.domain.Units
import com.getfit.domain.floorDayLocal
import com.getfit.domain.fmtDur
import com.getfit.domain.fmtVol
import com.getfit.domain.isBW
import com.getfit.domain.recap
import com.getfit.ui.AppViewModel
import java.util.Calendar

/** Strava-style recap: totals, records, streaks and favourites for a period, from existing data. */
@Composable
fun RecapScreen(vm: AppViewModel) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()
    val units = settings.units
    var period by remember { mutableStateOf("Month") }
    val now = System.currentTimeMillis()
    val from = remember(period, now / 60_000) {
        val c = Calendar.getInstance()
        when (period) {
            "Month" -> { c.set(Calendar.DAY_OF_MONTH, 1); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); c.timeInMillis }
            "Year" -> { c.set(Calendar.DAY_OF_YEAR, 1); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); c.timeInMillis }
            else -> 0L
        }
    }
    val stats = remember(data.sessions, data.sessionSets, data.logs, from) {
        recap(
            sessions = data.sessions.map { RecapSession(it.id, it.dateMs, it.durationSec, it.volume, it.calories) },
            sets = data.sessionSets.map { it.sessionId to it.exerciseId },
            logs = data.logs.map { LoggedSet(it.exerciseId, it.weight, it.reps, it.dateMs) },
            fromMs = from, toMs = now,
            muscleOf = { id -> data.exercise(id)?.muscle },
            nameOf = { id -> data.exercise(id)?.name ?: data.sessionSets.firstOrNull { it.exerciseId == id }?.name },
            isBodyweight = { id -> data.exercise(id)?.let { isBW(it.equipment, it.reps) } ?: false },
            floorDay = ::floorDayLocal,
        )
    }
    val title = when (period) { "Month" -> "This month"; "Year" -> "This year"; else -> "All time" }

    Column(Modifier.fillMaxSize().background(GfColor.Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(msIcon("arrow_back"), null, tint = GfColor.Text, modifier = Modifier.size(26.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.closeRecap() })
            Text("Recap", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 20.sp)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 40.dp)) {
            Segmented(options = listOf("Month", "Year", "All"), selected = period, onSelect = { period = it }, label = { it })

            // hero
            Column(
                Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(GfColor.AccentGradient).padding(22.dp),
            ) {
                Text(title.uppercase(), color = GfColor.OnAccentSub, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, letterSpacing = 1.sp)
                Text("${stats.workouts} ${if (stats.workouts == 1) "workout" else "workouts"}", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 34.sp, modifier = Modifier.padding(top = 6.dp))
                Text(
                    if (stats.workouts == 0) "Nothing logged in this period yet."
                    else "${fmtDur(stats.durationSec)} training · ${fmtVol(Units.volDisplay(stats.volumeKg.toInt(), units))} $units moved · ${String.format("%.1f", stats.avgPerWeek)} a week",
                    color = GfColor.OnAccentDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.5.sp, modifier = Modifier.padding(top = 4.dp),
                )
            }

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("military_tech", "Records set", stats.prs.toString(), Modifier.weight(1f))
                Stat("local_fire_department", "Best streak", "${stats.bestStreak} ${if (stats.bestStreak == 1) "day" else "days"}", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("exercise", "Sets logged", stats.sets.toString(), Modifier.weight(1f))
                Stat("calendar_month", "Active days", stats.activeDays.toString(), Modifier.weight(1f))
            }
            if (stats.calories > 0) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat("favorite", "Calories (est.)", "~${stats.calories}", Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }
            }

            Text("Favourites", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp))) {
                FavRow("Most-trained muscle", stats.topMuscle ?: "—", if (stats.topMuscleSets > 0) "${stats.topMuscleSets} sets" else "")
                Box(Modifier.fillMaxWidth().height(1.dp).background(GfColor.Hairline06.copy(alpha = 0.5f)))
                FavRow("Most-done exercise", stats.topExercise ?: "—", if (stats.topExerciseSets > 0) "${stats.topExerciseSets} sets" else "")
            }

            Text(
                "Totals come straight from your log; calories are the same estimate shown on each workout.",
                color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun Stat(icon: String, label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(msIcon(icon), null, tint = if (icon == "favorite") GfColor.Coral else if (icon == "military_tech" || icon == "local_fire_department") GfColor.Amber else GfColor.Accent, modifier = Modifier.size(18.dp))
            Text(label, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        }
        Text(value, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 22.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun FavRow(label: String, value: String, sub: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(value, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
            if (sub.isNotBlank()) Text(sub, color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.5.sp)
        }
    }
}
