package com.getfit.ui.home

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.Pill
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.StatCard
import com.getfit.core.ui.msIcon
import com.getfit.core.ui.pressScale
import com.getfit.data.db.Curated
import com.getfit.domain.SessionRecord
import com.getfit.domain.Units
import com.getfit.domain.floorDayLocal
import com.getfit.domain.fmtDur
import com.getfit.domain.fmtVol
import com.getfit.domain.streakCount
import com.getfit.domain.weekAgg
import com.getfit.domain.weekStartLocal
import com.getfit.ui.AppViewModel
import java.util.Calendar

@Composable
fun HomeScreen(vm: AppViewModel) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()
    val scroll = rememberScrollState()

    val now = System.currentTimeMillis()
    val cal = remember(now) { Calendar.getInstance() }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"; hour < 18 -> "Good afternoon"; else -> "Good evening"
    }
    val todayIdx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

    val week = weekAgg(
        data.sessions.map { SessionRecord(it.id, it.dateMs, it.name, it.durationSec, it.totalSets, it.volume, it.prs) },
        weekStartLocal(now),
    )
    val streak = streakCount(data.sessions.map { it.dateMs }, now, ::floorDayLocal)

    val exCount = data.plan.size
    val setsTotal = data.plan.sumOf { it.sets }
    val est = Math.round(data.plan.sumOf { it.sets * (45 + settings.restDefault) / 60.0 }).toInt()
    val rpe = Curated.INTENSITY[settings.intensity]?.rpe ?: "RPE 7–8"

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 108.dp),
    ) {
        // header
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(greeting, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp)
                Text("Alex", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 24.sp)
            }
            val gear = remember { MutableInteractionSource() }
            Box(
                Modifier.size(46.dp).pressScale(gear)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.linearGradient(listOf(GfColor.SurfaceElevated, Color(0xFF20180F))))
                    .border(1.dp, GfColor.Hairline10, RoundedCornerShape(999.dp))
                    .clickable(gear, indication = null) { vm.openSettings() },
                contentAlignment = Alignment.Center,
            ) {
                Text("AR", color = GfColor.Lime, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp)
            }
        }

        HeroCard(
            intensity = settings.intensity, rpe = rpe, exCount = exCount, sets = setsTotal, est = est,
            onStart = { vm.startSession(data.plan) },
        )

        Spacer(Modifier.height(16.dp))
        StreakStrip(streak = streak, dayHit = week.dayHit, todayIdx = todayIdx)

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(msIcon("exercise"), week.workouts.toString(), "Workouts", Modifier.weight(1f))
            StatCard(msIcon("timer"), fmtDur(week.durationSec), "Time", Modifier.weight(1f))
            StatCard(msIcon("trending_up"), fmtVol(Units.volDisplay(week.totalVolume, settings.units)), "Volume ${settings.units}", Modifier.weight(1f))
        }

        // categories
        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Browse by muscle", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp)
            Text(
                "See all", color = GfColor.Lime, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.5.sp,
                modifier = Modifier.clickable { vm.goExercises() },
            )
        }
        val counts = remember(data.exercises) { data.exercises.groupingBy { it.muscle }.eachCount() }
        val cats = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Cardio", "Glutes")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            cats.forEach { m -> CategoryCard(m, counts[m] ?: 0) { vm.goCategory(m) } }
        }
    }
}

@Composable
private fun HeroCard(intensity: String, rpe: String, exCount: Int, sets: Int, est: Int, onStart: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(GfColor.AccentGradient).padding(22.dp),
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(msIcon("bolt"), null, tint = GfColor.OnAccentSub, modifier = Modifier.size(16.dp))
                    Text("TODAY · READY", color = GfColor.OnAccentSub, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                }
                Box(
                    Modifier.clip(Pill).background(Color(0x24171210)).padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("$intensity · $rpe", color = GfColor.OnAccent, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp)
                }
            }
            Text("Push Day", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 29.sp, modifier = Modifier.padding(top = 8.dp))
            Text("Chest · Shoulders · Triceps", color = GfColor.OnAccentSub, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.5.sp, modifier = Modifier.padding(top = 3.dp))
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                HeroStat(exCount.toString(), "exercises")
                HeroStat(sets.toString(), "sets")
                HeroStat(est.toString(), "minutes")
            }
            val start = remember { MutableInteractionSource() }
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp).height(52.dp).pressScale(start, 0.97f)
                    .clip(RoundedCornerShape(16.dp)).background(GfColor.OnAccent)
                    .clickable(start, indication = null, onClick = onStart),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(msIcon("play_arrow"), null, tint = GfColor.Lime, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start workout", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column {
        Text(value, color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 19.sp)
        Text(label, color = GfColor.OnAccentDim2, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.sp)
    }
}

@Composable
private fun StreakStrip(streak: Int, dayHit: BooleanArray, todayIdx: Int) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(GfColor.Surface)
            .border(1.dp, GfColor.Hairline06, RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(msIcon("local_fire_department"), null, tint = GfColor.Amber, modifier = Modifier.size(20.dp))
                Text("$streak-day streak", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
            }
            Text("This week", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, d ->
                val done = dayHit.getOrElse(i) { false }
                val today = i == todayIdx
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(999.dp))
                            .background(if (done) GfColor.Lime else if (today) GfColor.LimeFill12 else GfColor.Surface)
                            .border(1.5.dp, if (today || done) GfColor.Lime else GfColor.Hairline08, RoundedCornerShape(999.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(msIcon("check"), null, tint = GfColor.OnAccent, modifier = Modifier.size(16.dp))
                    }
                    Text(d, color = if (today) GfColor.Lime else GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 10.5.sp)
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(name: String, count: Int, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier.width(120.dp).height(96.dp).pressScale(interaction).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(GfColor.SurfaceElevated, GfColor.SurfaceElevatedAlt)))
            .border(1.dp, GfColor.Hairline06, RoundedCornerShape(20.dp))
            .clickable(interaction, indication = null, onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(com.getfit.core.ui.MuscleIcon.of(name), null, tint = GfColor.Lime, modifier = Modifier.size(24.dp))
        Column {
            Text(name, color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp)
            Text("$count moves", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.sp)
        }
    }
}
