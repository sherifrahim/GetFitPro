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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.Pill
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.GfInput
import com.getfit.core.ui.GfSheet
import com.getfit.core.ui.GfSheetRow
import com.getfit.core.ui.Segmented
import com.getfit.core.ui.msIcon
import com.getfit.core.ui.pressScale
import com.getfit.data.db.Curated
import com.getfit.data.prefs.PlanItemData
import com.getfit.data.prefs.Routine
import com.getfit.domain.Units
import com.getfit.domain.isBW
import com.getfit.ui.AppData
import com.getfit.ui.AppViewModel
import kotlinx.coroutines.delay

/**
 * Build tab. Two states, like Hevy's Workout tab: the routines list (pick one to start, choose
 * which is up next, manage them) and, once a routine is opened, the editor the prototype's single
 * "Push Day" builder became.
 */
@Composable
fun BuilderScreen(vm: AppViewModel) {
    val nav by vm.nav.collectAsState()
    val data by vm.data.collectAsState()
    val editing = data.routine(nav.editRoutineId)
    if (editing != null) RoutineEditor(vm, editing) else RoutinesList(vm)
}

/** Rough duration: 45s of work per set plus the configured rest. Same maths Home uses. */
fun estMinutes(items: List<PlanItemData>, restDefault: Int): Int =
    Math.round(items.sumOf { it.sets * (45 + restDefault) / 60.0 }).toInt()

@Composable
private fun RoutinesList(vm: AppViewModel) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()
    val nav by vm.nav.collectAsState()
    val current = data.currentRoutine

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 40.dp),
        ) {
            Text("Routines", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 26.sp, modifier = Modifier.padding(vertical = 6.dp))
            Text(
                "${data.routines.size} routines" + (current?.let { " · ${it.name} up next" } ?: ""),
                color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp,
            )

            // new routine / explore programs
            Row(Modifier.padding(top = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val add = remember { MutableInteractionSource() }
                Row(
                    Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0x660B7BF7), RoundedCornerShape(16.dp))
                        .clickable(add, indication = null) { vm.createRoutine("New routine") },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(msIcon("add"), null, tint = GfColor.Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New routine", color = GfColor.Accent, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
                }
                val explore = remember { MutableInteractionSource() }
                Row(
                    Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(16.dp)).background(GfColor.Surface)
                        .border(1.dp, GfColor.Hairline08, RoundedCornerShape(16.dp))
                        .clickable(explore, indication = null) { vm.openPrograms() },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(msIcon("search"), null, tint = GfColor.Text, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Programs", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            data.routines.forEachIndexed { i, r ->
                RoutineCard(
                    routine = r, data = data, isCurrent = r.id == current?.id,
                    est = estMinutes(r.items, settings.restDefault),
                    onOpen = { vm.openRoutineEditor(r.id) },
                    onStart = { vm.startRoutine(r.id) },
                    onMenu = { vm.openRoutineMenu(r.id) },
                )
                Spacer(Modifier.height(12.dp))
            }
        }


        if (nav.programsOpen) {
            GfSheet(title = "Programs", onDismiss = vm::closePrograms) {
                Text(
                    "Ready-made routine sets. Adding one appends its days to your routines and makes the first day up next.",
                    color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.5.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    if (data.sessions.isNotEmpty()) {
                        val press = remember { MutableInteractionSource() }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(GfColor.Background)
                                .border(1.dp, Color(0x660B7BF7), RoundedCornerShape(16.dp))
                                .clickable(press, indication = null) { vm.createRoutinesFromHistory() }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("From my history", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp)
                                Text("One routine per workout name you've logged (imports included), built from its latest session.", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp))
                            }
                            Box(Modifier.clip(Pill).background(GfColor.Accent).padding(horizontal = 12.dp, vertical = 7.dp)) {
                                Text("Create", color = GfColor.OnAccent, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                            }
                        }
                    }
                    Curated.PROGRAMS.forEach { p ->
                        val press = remember { MutableInteractionSource() }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(GfColor.Background)
                                .border(1.dp, GfColor.Hairline06, RoundedCornerShape(16.dp))
                                .clickable(press, indication = null) { vm.addProgram(p.id) }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp)
                                Text(p.blurb, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp))
                                Text(p.days.joinToString(" · ") { it.name }, color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.5.sp, modifier = Modifier.padding(top = 5.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Box(Modifier.clip(Pill).background(GfColor.Accent).padding(horizontal = 12.dp, vertical = 7.dp)) {
                                Text("Add ${p.days.size}", color = GfColor.OnAccent, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        data.routine(nav.routineMenuId)?.let { r ->
            val idx = data.routines.indexOfFirst { it.id == r.id }
            GfSheet(title = r.name, onDismiss = vm::closeRoutineMenu) {
                if (r.id != current?.id) GfSheetRow(msIcon("event_upcoming"), "Do this one next") { vm.setCurrentRoutine(r.id) }
                GfSheetRow(msIcon("edit"), "Edit routine") { vm.openRoutineEditor(r.id) }
                GfSheetRow(msIcon("content_copy"), "Duplicate routine") { vm.duplicateRoutine(r.id) }
                if (idx > 0) GfSheetRow(msIcon("keyboard_arrow_up"), "Move up") { vm.moveRoutine(idx, -1); vm.closeRoutineMenu() }
                if (idx in 0 until data.routines.lastIndex) GfSheetRow(msIcon("keyboard_arrow_down"), "Move down") { vm.moveRoutine(idx, 1); vm.closeRoutineMenu() }
                if (data.routines.size > 1) GfSheetRow(msIcon("delete"), "Delete routine", destructive = true) { vm.deleteRoutine(r.id) }
            }
        }
    }
}

@Composable
private fun RoutineCard(
    routine: Routine, data: AppData, isCurrent: Boolean, est: Int,
    onOpen: () -> Unit, onStart: () -> Unit, onMenu: () -> Unit,
) {
    val open = remember { MutableInteractionSource() }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.Surface)
            .border(1.dp, if (isCurrent) Color(0x660B7BF7) else GfColor.Hairline06, RoundedCornerShape(20.dp))
            .clickable(open, indication = null, onClick = onOpen).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(routine.name, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (isCurrent) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(Pill).background(GfColor.AccentFill15).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("UP NEXT", color = GfColor.Accent, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Icon(msIcon("more_horiz"), null, tint = GfColor.TextDim, modifier = Modifier.size(22.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onMenu))
        }
        val preview = routine.items.joinToString(", ") { data.exercise(it.id)?.name ?: it.id }
        Text(
            preview.ifBlank { "No exercises yet — tap to add some" },
            color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "${routine.items.size} exercises · ${routine.setsTotal} sets · ~$est min",
            color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.5.sp, modifier = Modifier.padding(top = 6.dp),
        )
        val start = remember { MutableInteractionSource() }
        val enabled = routine.items.isNotEmpty()
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp).height(46.dp).pressScale(start, 0.98f).clip(RoundedCornerShape(14.dp))
                .background(if (isCurrent && enabled) GfColor.Accent else GfColor.Background)
                .border(1.dp, if (isCurrent && enabled) Color.Transparent else GfColor.Hairline10, RoundedCornerShape(14.dp))
                .clickable(start, indication = null, enabled = enabled, onClick = onStart),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            val fg = if (isCurrent && enabled) GfColor.OnAccent else if (enabled) GfColor.Text else GfColor.TextFaint
            Icon(msIcon("play_arrow"), null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Start routine", color = fg, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp)
        }
    }
}

@Composable
private fun RoutineEditor(vm: AppViewModel, routine: Routine) {
    val data by vm.data.collectAsState()
    val settings by vm.settings.collectAsState()

    val exCount = routine.items.size
    val setsTotal = routine.setsTotal
    val est = estMinutes(routine.items, settings.restDefault)
    val intensity = Curated.INTENSITY[settings.intensity]
    val rpe = intensity?.rpe ?: "RPE 7–8"
    val intensityColor = intensity?.let { Color(it.colorArgb) } ?: GfColor.Amber

    // Name edits are debounced into the store so each keystroke doesn't rewrite DataStore + sync.
    var name by remember(routine.id) { mutableStateOf(routine.name) }
    LaunchedEffect(name) {
        if (name != routine.name) { delay(500); vm.renameRoutine(routine.id, name) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 96.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                val back = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(999.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline08, RoundedCornerShape(999.dp))
                        .clickable(back, indication = null) { vm.closeRoutineEditor() },
                    contentAlignment = Alignment.Center,
                ) { Icon(msIcon("arrow_back"), null, tint = GfColor.Text, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                GfInput(value = name, onValueChange = { name = it.take(40) }, placeholder = "Routine name", modifier = Modifier.weight(1f), textSize = 18.sp, fontFamily = SpaceGrotesk)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
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
            routine.items.forEachIndexed { i, item ->
                val linkedFromAbove = i > 0 && routine.items[i - 1].superset
                PlanRow(
                    item = item,
                    exName = data.exercise(item.id)?.name ?: item.id,
                    sub = subFor(data, item.id, settings.units),
                    isFirst = i == 0, isLast = i == routine.items.lastIndex,
                    linked = item.superset || linkedFromAbove,
                    onUp = { vm.movePlan(i, -1) }, onDown = { vm.movePlan(i, 1) },
                    onLess = { vm.setSets(item.id, -1) }, onMore = { vm.setSets(item.id, 1) },
                    onReps = { vm.setReps(item.id, it) },
                    onRemove = { vm.removeFromPlan(item.id) },
                )
                // Superset link between this row and the next: a tappable chain in the gap.
                if (i < routine.items.lastIndex) {
                    val link = remember { MutableInteractionSource() }
                    Row(
                        Modifier.fillMaxWidth().height(22.dp).clickable(link, indication = null) { vm.toggleSuperset(item.id) },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(2.dp).height(22.dp).background(if (item.superset) GfColor.Accent else GfColor.Hairline08))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (item.superset) "Superset · no rest between" else "Link as superset",
                            color = if (item.superset) GfColor.Accent else GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.sp,
                        )
                    }
                } else Spacer(Modifier.height(10.dp))
            }

            // add exercise
            val add = remember { MutableInteractionSource() }
            Row(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0x660B7BF7), RoundedCornerShape(16.dp))
                    .clickable(add, indication = null) { vm.goExercises() },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(msIcon("add"), null, tint = GfColor.Accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add exercise", color = GfColor.Accent, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
            }
        }

        // sticky Start
        val start = remember { MutableInteractionSource() }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                .height(56.dp).pressScale(start, 0.98f).clip(RoundedCornerShape(18.dp)).background(GfColor.Accent)
                .clickable(start, indication = null) { vm.startRoutine(routine.id) },
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(msIcon("play_arrow"), null, tint = GfColor.OnAccent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(9.dp))
            Text("Start workout", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 16.5.sp)
        }
    }
}

private fun subFor(data: AppData, id: String, units: String): String {
    val ex = data.exercise(id) ?: return ""
    val best = data.bestMap[id] ?: return ex.muscle
    return if (isBW(ex.equipment, ex.reps)) "Best ${best.reps} reps" else "Best ${Units.fmtDisplay(best.weight, units)} $units"
}

@Composable
private fun PlanRow(
    item: PlanItemData, exName: String, sub: String,
    isFirst: Boolean, isLast: Boolean, linked: Boolean,
    onUp: () -> Unit, onDown: () -> Unit, onLess: () -> Unit, onMore: () -> Unit,
    onReps: (String) -> Unit, onRemove: () -> Unit,
) {
    // Tapping the "sets × reps" readout swaps it for a small reps field (Hevy's rep-range column).
    var editingReps by remember(item.id) { mutableStateOf(false) }
    var reps by remember(item.id, item.reps) { mutableStateOf(item.reps) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GfColor.Surface)
            .border(1.dp, if (linked) Color(0x660B7BF7) else GfColor.Hairline06, RoundedCornerShape(18.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Icon(msIcon("keyboard_arrow_up"), null, tint = if (isFirst) Color(0xFF3A3A3A) else GfColor.TextDim, modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onUp))
            Icon(msIcon("keyboard_arrow_down"), null, tint = if (isLast) Color(0xFF3A3A3A) else GfColor.TextDim, modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDown))
        }
        Column(Modifier.weight(1f)) {
            Text(exName, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W600, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).background(GfColor.Background).padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(msIcon("remove"), null, tint = GfColor.Accent, modifier = Modifier.size(18.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onLess))
            if (editingReps) {
                GfInput(
                    value = reps, onValueChange = { v -> reps = v.filter { it.isDigit() || it == 's' }.take(4) },
                    placeholder = item.reps, modifier = Modifier.width(56.dp), textSize = 12.sp,
                )
                Icon(
                    msIcon("check"), null, tint = GfColor.Accent,
                    modifier = Modifier.size(18.dp).clickable(remember { MutableInteractionSource() }, indication = null) {
                        editingReps = false; if (reps.isNotBlank() && reps != item.reps) onReps(reps)
                    },
                )
            } else {
                Text(
                    "${item.sets} × ${item.reps}", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 13.sp,
                    modifier = Modifier.width(48.dp).clickable(remember { MutableInteractionSource() }, indication = null) { editingReps = true }, maxLines = 1,
                )
                Icon(msIcon("add"), null, tint = GfColor.Accent, modifier = Modifier.size(18.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onMore))
            }
        }
        Icon(msIcon("delete"), null, tint = GfColor.TextFaint, modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onRemove))
    }
}
