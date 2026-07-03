package com.getfit.ui.settings

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.remember
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
import com.getfit.core.ui.msIcon
import com.getfit.ui.AppViewModel

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsState()
    val nav by vm.nav.collectAsState()

    Column(Modifier.fillMaxSize().background(GfColor.Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(msIcon("arrow_back"), null, tint = GfColor.Text, modifier = Modifier.size(26.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.closeSettings() })
            Text("Settings", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 20.sp)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 30.dp)) {
            // profile
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(GfColor.SurfaceElevated, GfColor.SurfaceElevatedAlt))).border(1.dp, GfColor.Hairline06, RoundedCornerShape(22.dp)).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(58.dp).clip(Pill).background(GfColor.Lime), contentAlignment = Alignment.Center) {
                    Text("AR", color = GfColor.OnAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 22.sp)
                }
                Column {
                    Text("Alex Rivera", color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 18.sp)
                    Text("Member since 2024", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp)
                }
            }

            SectionLabel("Preferences")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.Surface).border(1.dp, GfColor.Hairline06, RoundedCornerShape(20.dp))) {
                // units
                Row(
                    Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) { vm.toggleUnits() }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(msIcon("straighten"), null, tint = GfColor.Lime, modifier = Modifier.size(22.dp))
                    Text("Units", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.5.sp, modifier = Modifier.weight(1f))
                    Row(Modifier.clip(Pill).background(GfColor.Background).padding(3.dp)) {
                        UnitPill("kg", settings.units == "kg")
                        UnitPill("lb", settings.units == "lb")
                    }
                }
                Divider()
                ToggleRow("volume_up", "Sound cues", settings.sound) { vm.setSound(it) }
                Divider()
                ToggleRow("vibration", "Haptics", settings.haptics) { vm.setHaptics(it) }
                Divider()
                ToggleRow("timer", "Auto-start rest", settings.autorest) { vm.setAutorest(it) }
            }

            SectionLabel("Rest timer default")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 45, 60, 90).forEach { v ->
                    val sel = settings.restDefault == v
                    Box(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(if (sel) GfColor.Lime else GfColor.Surface).border(1.dp, if (sel) GfColor.Lime else GfColor.Hairline08, RoundedCornerShape(14.dp))
                            .clickable(remember { MutableInteractionSource() }, indication = null) { vm.setRest(v) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${v}s", color = if (sel) GfColor.OnAccent else GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 14.sp)
                    }
                }
            }

            // clear all
            val confirm = nav.confirmClear
            Row(
                Modifier.padding(top = 22.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (confirm) Color(0x24F0774E) else GfColor.Surface).border(1.dp, if (confirm) Color(0x66F0774E) else GfColor.Hairline06, RoundedCornerShape(18.dp))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { vm.clearAll() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(msIcon(if (confirm) "warning" else "delete_forever"), null, tint = GfColor.Coral, modifier = Modifier.size(20.dp))
                Text(if (confirm) "Tap again to clear everything" else "Clear all data", color = if (confirm) GfColor.Coral else GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.5.sp, modifier = Modifier.weight(1f))
                Icon(msIcon("chevron_right"), null, tint = GfColor.TextFaint, modifier = Modifier.size(20.dp))
            }

            // privacy note
            Row(
                Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x12CBF25C)).border(1.dp, Color(0x24CBF25C), RoundedCornerShape(18.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(msIcon("lock"), null, tint = GfColor.Lime, modifier = Modifier.size(20.dp))
                Text("Private by design. Everything — your logs, PRs and targets — stays on your device. No account, no cloud.", color = GfColor.TextCue, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun UnitPill(text: String, selected: Boolean) {
    Box(Modifier.clip(Pill).background(if (selected) GfColor.Lime else Color.Transparent).padding(horizontal = 14.dp, vertical = 5.dp)) {
        Text(text, color = if (selected) GfColor.OnAccent else GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp)
    }
}

@Composable
private fun ToggleRow(icon: String, label: String, on: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) { onToggle(!on) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(msIcon(icon), null, tint = GfColor.Lime, modifier = Modifier.size(22.dp))
        Text(label, color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.5.sp, modifier = Modifier.weight(1f))
        GfSwitch(on)
    }
}

@Composable
private fun GfSwitch(on: Boolean) {
    val knobX by animateDpAsState(if (on) 19.dp else 0.dp, label = "knob")
    Box(
        Modifier.size(width = 46.dp, height = 27.dp).clip(Pill).background(if (on) GfColor.Lime else GfColor.Hairline12).padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset(x = knobX).size(21.dp).clip(Pill).background(Color.White))
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(GfColor.Hairline06.copy(alpha = 0.5f)))
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
}
