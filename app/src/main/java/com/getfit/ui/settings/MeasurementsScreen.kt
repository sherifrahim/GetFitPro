package com.getfit.ui.settings

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
import com.getfit.core.ui.GfInput
import com.getfit.core.ui.msIcon
import com.getfit.core.ui.pressScale
import com.getfit.domain.Units
import com.getfit.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Hevy's Measurements: body weight (and optional body-fat) over time, newest first. */
@Composable
fun MeasurementsScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsState()
    val data by vm.data.collectAsState()
    val units = settings.units
    var weight by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val df = remember { SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().background(GfColor.Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(msIcon("arrow_back"), null, tint = GfColor.Text, modifier = Modifier.size(26.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.closeMeasurements() })
            Text("Body measurements", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 20.sp)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 40.dp)) {
            // entry form
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(20.dp)).padding(16.dp),
            ) {
                Text("Log today", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GfInput(value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' }.take(6) }, placeholder = "Weight ($units)", numeric = true, modifier = Modifier.weight(1f))
                    GfInput(value = fat, onValueChange = { fat = it.filter { c -> c.isDigit() || c == '.' }.take(4) }, placeholder = "Body fat %", numeric = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                GfInput(value = note, onValueChange = { note = it.take(60) }, placeholder = "Note (optional)")
                val press = remember { MutableInteractionSource() }
                val ok = (weight.toDoubleOrNull() ?: 0.0) > 0.0
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp).pressScale(press, 0.98f).clip(RoundedCornerShape(14.dp))
                        .background(if (ok) GfColor.Accent else GfColor.Background).border(1.dp, if (ok) GfColor.Accent else GfColor.Hairline08, RoundedCornerShape(14.dp))
                        .clickable(press, indication = null, enabled = ok) {
                            vm.addMeasurement(weight.toDouble(), fat.toDoubleOrNull(), note)
                            weight = ""; fat = ""; note = ""
                        },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(msIcon("add"), null, tint = if (ok) GfColor.OnAccent else GfColor.TextFaint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add measurement", color = if (ok) GfColor.OnAccent else GfColor.TextFaint, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp)
                }
            }

            if (data.measurements.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(msIcon("straighten"), null, tint = GfColor.TextFaint, modifier = Modifier.size(40.dp))
                    Text("No measurements yet", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 17.sp, modifier = Modifier.padding(top = 12.dp))
                    Text("Track your weight over time. The latest entry also feeds the calorie estimate on each workout.", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                val first = data.measurements.last()
                val latest = data.measurements.first()
                val delta = latest.weightKg - first.weightKg
                Text(
                    "Latest ${Units.fmtDisplay(Units.toDisplay(latest.weightKg, units), units)} $units" +
                        if (data.measurements.size > 1) " · ${if (delta >= 0) "+" else ""}${Units.fmtDisplay(Units.toDisplay(delta, units), units)} $units since first entry" else "",
                    color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                )
                data.measurements.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(16.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(Units.fmtDisplay(Units.toDisplay(m.weightKg, units), units), color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 18.sp)
                                Text(units, color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp))
                                m.bodyFatPct?.let { Text("· ${Units.fmtDisplay(it, "kg")}% fat", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp)) }
                            }
                            Text(df.format(Date(m.dateMs)) + if (m.note.isNotBlank()) " · ${m.note}" else "", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp)
                        }
                        Icon(msIcon("delete"), null, tint = GfColor.TextFaint, modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.deleteMeasurement(m.id) })
                    }
                }
            }
        }
    }
}
