package com.getfit.ui.progress

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.Pill
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.msIcon
import com.getfit.data.db.Curated
import com.getfit.domain.fmtW
import com.getfit.domain.isBW
import com.getfit.ui.AppViewModel
import kotlin.math.ceil

@Composable
fun GoalSheet(vm: AppViewModel) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()
    val nav by vm.nav.collectAsState()

    val ex = data.exercise(nav.goalEx)
    val bw = ex?.let { isBW(it.equipment, it.reps) } ?: false
    val unit = if (bw) "reps" else settings.units
    val cur = vm.goalCurDisplay(nav.goalEx)
    val diff = nav.goalTarget - cur
    val per = if (diff > 0 && nav.goalWeeks > 0) diff / nav.goalWeeks else 0.0
    val canSave = nav.goalTarget > cur

    Box(
        Modifier.fillMaxSize().background(Color(0x99060403)).clickable(remember { MutableInteractionSource() }, indication = null) { vm.closeGoal() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(GfColor.SurfaceElevatedAlt).border(1.dp, GfColor.Hairline08, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(remember { MutableInteractionSource() }, indication = null) { /* consume */ }
                .padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 26.dp).navigationBarsPadding(),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp).size(width = 38.dp, height = 4.dp).clip(Pill).background(GfColor.Text.copy(alpha = 0.2f)))
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("New target", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 20.sp)
                Icon(msIcon("close"), null, tint = GfColor.TextDim, modifier = Modifier.size(26.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.closeGoal() })
            }

            Label("Exercise")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Curated.GOAL_EX.forEach { id ->
                    val sel = id == nav.goalEx
                    Box(
                        Modifier.clip(Pill).background(if (sel) GfColor.Lime else GfColor.Surface).border(1.dp, if (sel) GfColor.Lime else GfColor.Hairline08, Pill)
                            .clickable(remember { MutableInteractionSource() }, indication = null) { vm.selectGoalEx(id) }.padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text(data.exercise(id)?.name ?: id, color = if (sel) GfColor.OnAccent else GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp)
                    }
                }
            }

            Label("Target $unit")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline08, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(msIcon("remove_circle"), null, tint = GfColor.Lime, modifier = Modifier.size(32.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.adjustGoal(-1) })
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(fmtW(nav.goalTarget), color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 28.sp)
                    Text(" $unit", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
                Icon(msIcon("add_circle"), null, tint = GfColor.Lime, modifier = Modifier.size(32.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.adjustGoal(1) })
            }

            Spacer(Modifier.height(18.dp))
            Label("Timeframe")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 6, 8, 12).forEach { w ->
                    val sel = w == nav.goalWeeks
                    Box(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp)).background(if (sel) GfColor.Lime else GfColor.Surface).border(1.dp, if (sel) GfColor.Lime else GfColor.Hairline08, RoundedCornerShape(14.dp))
                            .clickable(remember { MutableInteractionSource() }, indication = null) { vm.setGoalWeeks(w) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$w wk", color = if (sel) GfColor.OnAccent else GfColor.TextDim, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 14.sp)
                    }
                }
            }

            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(msIcon("trending_up"), null, tint = GfColor.Lime, modifier = Modifier.size(18.dp))
                Text(
                    if (canSave) "+${if (bw) ceil(per).toInt().toString() + " reps" else fmtW(Math.round(per * 10) / 10.0) + " " + settings.units} / week · from ${if (bw) cur.toInt() else fmtW(cur)} $unit now"
                    else "Pick a target above your best",
                    color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp,
                )
            }

            Box(
                Modifier.padding(top = 18.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).background(if (canSave) GfColor.Lime else GfColor.SurfaceElevated)
                    .clickable(remember { MutableInteractionSource() }, indication = null) { if (canSave) vm.saveGoal() },
                contentAlignment = Alignment.Center,
            ) {
                Text("Set target", color = if (canSave) GfColor.OnAccent else GfColor.TextFaint, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 10.dp))
}
