package com.getfit.ui.builder

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.Segmented
import com.getfit.core.ui.msIcon
import com.getfit.core.ui.pressScale
import com.getfit.data.db.Curated
import com.getfit.data.prefs.PlanItemData
import com.getfit.domain.fmtW
import com.getfit.domain.isBW
import com.getfit.ui.AppViewModel

@Composable
fun BuilderScreen(vm: AppViewModel) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()

    val exCount = data.plan.size
    val setsTotal = data.plan.sumOf { it.sets }
    val est = Math.round(data.plan.sumOf { it.sets * (45 + settings.restDefault) / 60.0 }).toInt()
    val intensity = Curated.INTENSITY[settings.intensity]
    val rpe = intensity?.rpe ?: "RPE 7–8"
    val intensityColor = intensity?.let { Color(it.colorArgb) } ?: GfColor.Amber

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 96.dp),
        ) {
            Text("Push Day", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 26.sp, modifier = Modifier.padding(vertical = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$exCount exercises", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp)
                Text("·", color = GfColor.TextDim, fontSize = 13.sp)
                Text("$setsTotal sets", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp)
                Text("·", color = GfColor.TextDim, fontSize = 13.sp)
                Text("~$est min", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp)
            }

            // intensity
            Column(
                Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GfColor.Surface)
                    .border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Intensity", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.5.sp)
                    Text("$rpe · $est min", color = intensityColor, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                }
                Segmented(
                    options = listOf("Light", "Moderate", "Hard"),
                    selected = settings.intensity,
                    onSelect = vm::setIntensity,
                    label = { it },
                )
            }

            Spacer(Modifier.height(16.dp))
            data.plan.forEachIndexed { i, item ->
                PlanRow(
                    item = item,
                    exName = data.exercise(item.id)?.name ?: item.id,
                    sub = subFor(vm, item.id, settings.units),
                    isFirst = i == 0, isLast = i == data.plan.lastIndex,
                    onUp = { vm.movePlan(i, -1) }, onDown = { vm.movePlan(i, 1) },
                    onLess = { vm.setSets(item.id, -1) }, onMore = { vm.setSets(item.id, 1) },
                    onRemove = { vm.removeFromPlan(item.id) },
                )
                Spacer(Modifier.height(10.dp))
            }

            // add exercise
            val add = remember { MutableInteractionSource() }
            Row(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0x66CBF25C), RoundedCornerShape(16.dp))
                    .clickable(add, indication = null) { vm.goExercises() },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(msIcon("add"), null, tint = GfColor.Lime, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add exercise", color = GfColor.Lime, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
            }
        }

        // sticky Start
        val start = remember { MutableInteractionSource() }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                .height(56.dp).pressScale(start, 0.98f).clip(RoundedCornerShape(18.dp)).background(GfColor.Lime)
                .clickable(start, indication = null) { vm.startSession(data.plan) },
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(msIcon("play_arrow"), null, tint = GfColor.OnAccent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(9.dp))
            Text("Start workout", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.5.sp)
        }
    }
}

private fun subFor(vm: AppViewModel, id: String, units: String): String {
    val data = vm.data.value
    val ex = data.exercise(id) ?: return ""
    val best = data.bestMap[id] ?: return ex.muscle
    return if (isBW(ex.equipment, ex.reps)) "Best ${best.reps} reps" else "Best ${fmtW(best.weight)} $units"
}

@Composable
private fun PlanRow(
    item: PlanItemData, exName: String, sub: String,
    isFirst: Boolean, isLast: Boolean,
    onUp: () -> Unit, onDown: () -> Unit, onLess: () -> Unit, onMore: () -> Unit, onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GfColor.Surface)
            .border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Icon(msIcon("keyboard_arrow_up"), null, tint = if (isFirst) Color(0xFF3A342B) else GfColor.TextDim, modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onUp))
            Icon(msIcon("keyboard_arrow_down"), null, tint = if (isLast) Color(0xFF3A342B) else GfColor.TextDim, modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDown))
        }
        Column(Modifier.weight(1f)) {
            Text(exName, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W600, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).background(GfColor.Background).padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(msIcon("remove"), null, tint = GfColor.Lime, modifier = Modifier.size(18.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onLess))
            Text("${item.sets} × ${item.reps}", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.width(48.dp), maxLines = 1)
            Icon(msIcon("add"), null, tint = GfColor.Lime, modifier = Modifier.size(18.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onMore))
        }
        Icon(msIcon("delete"), null, tint = GfColor.TextFaint, modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onRemove))
    }
}
