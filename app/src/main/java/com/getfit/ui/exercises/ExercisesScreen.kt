package com.getfit.ui.exercises

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.MuscleIcon
import com.getfit.core.ui.Pill as PillAtom
import com.getfit.core.ui.msIcon
import com.getfit.core.ui.pressScale
import com.getfit.data.db.Curated
import com.getfit.data.db.ExerciseEntity
import com.getfit.domain.Best
import com.getfit.domain.Units
import com.getfit.domain.isBW
import com.getfit.ui.AppViewModel

@Composable
fun ExercisesScreen(vm: AppViewModel) {
    val data by vm.data.collectAsState()
    val nav by vm.nav.collectAsState()
    val settings by vm.settings.collectAsState()

    val q = nav.query.trim().lowercase()
    val filtered = remember(data.exercises, nav.query, nav.filter) {
        data.exercises.filter { e ->
            (nav.filter == "All" || e.muscle == nav.filter) &&
                (q.isBlank() || e.name.lowercase().contains(q) ||
                    e.muscle.lowercase().contains(q) || e.equipment.lowercase().contains(q) ||
                    e.target.lowercase().contains(q) || e.secondaryMuscles.lowercase().contains(q))
        }
    }
    val libLine = if (q.isNotBlank() || nav.filter != "All") "${filtered.size} results"
    else "${data.exercises.size} exercises in the library"

    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 108.dp),
    ) {
        item {
            Text("Exercises", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 26.sp, modifier = Modifier.padding(vertical = 6.dp))
            Text(libLine, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
            SearchField(nav.query, hasQuery = nav.query.isNotEmpty(), onChange = vm::setQuery, onClear = vm::clearQuery)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Curated.MUSCLES.forEach { m ->
                    Box(Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { vm.setFilter(m) }) {
                        PillAtom(m, selected = nav.filter == m)
                    }
                }
            }
        }
        items(filtered, key = { it.id }) { ex ->
            ExerciseRow(ex, best = data.bestMap[ex.id], units = settings.units,
                onOpen = { vm.openDetail(ex.id) }, onAdd = { vm.addToPlan(ex.id) })
            Spacer(Modifier.height(10.dp))
        }
        if (filtered.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(msIcon("search_off"), null, tint = GfColor.TextFaint, modifier = Modifier.size(40.dp))
                    Text("No exercises found", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, hasQuery: Boolean, onChange: (String) -> Unit, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(GfColor.Surface)
            .border(1.dp, GfColor.Hairline08, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(msIcon("search"), null, tint = GfColor.TextDim, modifier = Modifier.size(22.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Search exercises", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 15.sp)
            }
            BasicTextField(
                value = query, onValueChange = onChange, singleLine = true,
                textStyle = LocalTextStyle.current.merge(TextStyle(color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 15.sp)),
                cursorBrush = SolidColor(GfColor.Lime), modifier = Modifier.fillMaxWidth(),
            )
        }
        if (hasQuery) {
            Icon(msIcon("close"), null, tint = GfColor.TextDim, modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClear))
        }
    }
}

@Composable
private fun ExerciseRow(ex: ExerciseEntity, best: Best?, units: String, onOpen: () -> Unit, onAdd: () -> Unit) {
    val row = remember { MutableInteractionSource() }
    val bw = isBW(ex.equipment, ex.reps)
    Row(
        Modifier.fillMaxWidth().pressScale(row, 0.985f).clip(RoundedCornerShape(18.dp)).background(GfColor.Surface)
            .border(1.dp, GfColor.Hairline06, RoundedCornerShape(18.dp))
            .clickable(row, indication = null, onClick = onOpen).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(GfColor.LimeFill12), contentAlignment = Alignment.Center) {
            Icon(MuscleIcon.of(ex.muscle), null, tint = GfColor.Lime, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(ex.name, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W600, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${ex.muscle} · ${ex.equipment}", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.5.sp, modifier = Modifier.padding(top = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (best != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(if (bw) "${best.reps} reps" else "${Units.fmtDisplay(best.weight, units)} $units", color = GfColor.Lime, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 12.5.sp)
                Text("BEST", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 9.5.sp)
            }
        }
        val add = remember { MutableInteractionSource() }
        Box(
            Modifier.size(44.dp).clickable(add, indication = null, onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(34.dp).pressScale(add, 0.88f).clip(RoundedCornerShape(11.dp)).background(GfColor.SurfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                Icon(msIcon("add"), null, tint = GfColor.Lime, modifier = Modifier.size(20.dp))
            }
        }
    }
}
