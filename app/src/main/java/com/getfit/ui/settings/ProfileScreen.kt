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
import androidx.compose.runtime.LaunchedEffect
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
import com.getfit.core.theme.Pill
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.ui.GfInput
import com.getfit.core.ui.Segmented
import com.getfit.core.ui.msIcon
import com.getfit.domain.Units
import com.getfit.ui.AppViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Hevy's Edit Profile, cut down to what Forge can actually use: the name feeds the greeting and
 * avatar initials; sex, birth year and body weight feed the calorie estimate on each workout.
 * Every field writes through on a short debounce — there is no Save button to forget.
 */
@Composable
fun ProfileScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsState()
    val data by vm.data.collectAsState()
    val units = settings.units

    var name by remember { mutableStateOf(settings.name) }
    var year by remember { mutableStateOf(if (settings.birthYear > 0) settings.birthYear.toString() else "") }
    var height by remember { mutableStateOf(if (settings.heightCm > 0) settings.heightCm.toString() else "") }
    var weight by remember {
        mutableStateOf(if (settings.bodyWeightKg > 0) Units.fmtDisplay(Units.toDisplay(settings.bodyWeightKg, units), units) else "")
    }

    LaunchedEffect(name) { if (name != settings.name) { delay(500); vm.setProfileName(name) } }
    LaunchedEffect(year) {
        val y = year.toIntOrNull() ?: 0
        if (y != settings.birthYear && (y == 0 || y in 1900..Calendar.getInstance().get(Calendar.YEAR))) { delay(600); vm.setProfileBirthYear(y) }
    }
    LaunchedEffect(height) {
        val h = height.toIntOrNull() ?: 0
        if (h != settings.heightCm) { delay(600); vm.setProfileHeightCm(h) }
    }
    LaunchedEffect(weight) {
        val w = weight.toDoubleOrNull() ?: 0.0
        val curDisplay = if (settings.bodyWeightKg > 0) Units.toDisplay(settings.bodyWeightKg, units) else 0.0
        if (Math.abs(w - curDisplay) > 0.05) { delay(600); vm.setProfileBodyWeight(w) }
    }

    Column(Modifier.fillMaxSize().background(GfColor.Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(msIcon("arrow_back"), null, tint = GfColor.Text, modifier = Modifier.size(26.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.closeProfile() })
            Text("Profile", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 20.sp)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 40.dp)) {
            // avatar + stats
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(72.dp).clip(Pill).background(GfColor.Accent), contentAlignment = Alignment.Center) {
                    if (settings.initials.isNotBlank()) Text(settings.initials, color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 26.sp)
                    else Icon(msIcon("person"), null, tint = GfColor.OnAccent, modifier = Modifier.size(34.dp))
                }
                Column {
                    Text(settings.name.ifBlank { "Your name" }, color = if (settings.name.isBlank()) GfColor.TextFaint else GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 20.sp)
                    Text(profileSubline(data.sessions.size, data.sessions.minOfOrNull { it.dateMs }), color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp)
                }
            }

            Label("Name")
            GfInput(value = name, onValueChange = { name = it.take(40) }, placeholder = "How should Forge greet you?")

            Label("Sex")
            Segmented(
                options = listOf("male", "female", ""),
                selected = settings.sex,
                onSelect = vm::setProfileSex,
                label = { when (it) { "male" -> "Male"; "female" -> "Female"; else -> "Not set" } },
            )
            Hint("Only used to pick the calorie formula. Leave it unset and Forge averages both.")

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Label("Birth year")
                    GfInput(value = year, onValueChange = { year = it.filter { c -> c.isDigit() }.take(4) }, placeholder = "e.g. 1998", numeric = true)
                }
                Column(Modifier.weight(1f)) {
                    Label("Height (cm)")
                    GfInput(value = height, onValueChange = { height = it.filter { c -> c.isDigit() }.take(3) }, placeholder = "e.g. 178", numeric = true)
                }
            }

            Label("Body weight ($units)")
            GfInput(value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' }.take(6) }, placeholder = "Used for calorie estimates", numeric = true)
            Hint("Logging a measurement (Settings › Body measurements) updates this too.")

            Label("Weekly goal")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${settings.weeklyGoal} workouts a week", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.5.sp)
                    Text("Drives the streak strip and the weekly target on Progress", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp)
                }
                Row(Modifier.clip(RoundedCornerShape(12.dp)).background(GfColor.Background).padding(horizontal = 6.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(msIcon("remove"), null, tint = GfColor.Accent, modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.setWeeklyGoal(settings.weeklyGoal - 1) })
                    Text("${settings.weeklyGoal}", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 15.sp, modifier = Modifier.width(20.dp))
                    Icon(msIcon("add"), null, tint = GfColor.Accent, modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.setWeeklyGoal(settings.weeklyGoal + 1) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** "12 workouts · since Aug 2026", or an invitation when there's no history yet. */
internal fun profileSubline(workouts: Int, firstMs: Long?): String {
    if (workouts == 0 || firstMs == null) return "No workouts yet — start one from Home"
    val since = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(firstMs))
    return "$workouts ${if (workouts == 1) "workout" else "workouts"} · since $since"
}

@Composable
private fun Label(t: String) {
    Text(t.uppercase(), color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun Hint(t: String) {
    Text(t, color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 6.dp))
}
