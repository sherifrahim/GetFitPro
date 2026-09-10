package com.getfit.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val hasAiKey by vm.hasAiKey.collectAsState()

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
                Box(Modifier.size(58.dp).clip(Pill).background(GfColor.Accent), contentAlignment = Alignment.Center) {
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
                    Icon(msIcon("straighten"), null, tint = GfColor.Accent, modifier = Modifier.size(22.dp))
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
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(if (sel) GfColor.Accent else GfColor.Surface).border(1.dp, if (sel) GfColor.Accent else GfColor.Hairline08, RoundedCornerShape(14.dp))
                            .clickable(remember { MutableInteractionSource() }, indication = null) { vm.setRest(v) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${v}s", color = if (sel) GfColor.OnAccent else GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 14.sp)
                    }
                }
            }

            SectionLabel("AI coach")
            AiKeySection(hasAiKey = hasAiKey, model = settings.aiModel, vm = vm)

            SectionLabel("Backup")
            BackupSection(vm)

            SectionLabel("Import & export")
            ImportExportSection(vm)

            SectionLabel("Cloud sync")
            SyncSection(vm)

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
                Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x120B7BF7)).border(1.dp, Color(0x240B7BF7), RoundedCornerShape(18.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(msIcon("lock"), null, tint = GfColor.Accent, modifier = Modifier.size(20.dp))
                Text("Private by design. Everything — your logs, PRs and targets — stays on your device, with no account required. AI review and sync are opt-in and only reach the network when you turn them on.", color = GfColor.TextCue, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun UnitPill(text: String, selected: Boolean) {
    Box(Modifier.clip(Pill).background(if (selected) GfColor.Accent else Color.Transparent).padding(horizontal = 14.dp, vertical = 5.dp)) {
        Text(text, color = if (selected) GfColor.OnAccent else GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp)
    }
}

@Composable
private fun ToggleRow(icon: String, label: String, on: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) { onToggle(!on) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(msIcon(icon), null, tint = GfColor.Accent, modifier = Modifier.size(22.dp))
        Text(label, color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.5.sp, modifier = Modifier.weight(1f))
        GfSwitch(on)
    }
}

@Composable
private fun GfSwitch(on: Boolean) {
    val knobX by animateDpAsState(if (on) 19.dp else 0.dp, label = "knob")
    Box(
        Modifier.size(width = 46.dp, height = 27.dp).clip(Pill).background(if (on) GfColor.Accent else GfColor.Hairline12).padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset(x = knobX).size(21.dp).clip(Pill).background(Color.White))
    }
}

@Composable
private fun AiKeySection(hasAiKey: Boolean, model: String, vm: AppViewModel) {
    var keyInput by remember { mutableStateOf("") }
    var modelInput by remember(model) { mutableStateOf(model) }
    var reveal by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.Surface)
            .border(1.dp, GfColor.Hairline06, RoundedCornerShape(20.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(msIcon(if (hasAiKey) "check_circle" else "info"), null, tint = if (hasAiKey) GfColor.Accent else GfColor.TextFaint, modifier = Modifier.size(16.dp))
            Text(
                if (hasAiKey) "API key saved" else "No API key set", color = GfColor.TextDim, fontFamily = Manrope,
                fontWeight = FontWeight.W600, fontSize = 12.5.sp,
            )
        }
        Text(
            "Bring your own Anthropic API key — it's encrypted on-device and only ever sent to " +
                "Anthropic's API when you ask for a review. Get one at console.anthropic.com.",
            color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp,
            lineHeight = 17.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )

        GfTextField(
            value = keyInput, onValueChange = { keyInput = it },
            placeholder = if (hasAiKey) "New key (leave blank to keep current)" else "sk-ant-...",
            masked = !reveal,
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (reveal) "Hide" else "Show", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp,
                modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { reveal = !reveal },
            )
            Row {
                if (hasAiKey) {
                    Text(
                        "Remove key", color = GfColor.Coral, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.5.sp,
                        modifier = Modifier.padding(end = 18.dp).clickable(remember { MutableInteractionSource() }, indication = null) { vm.clearAiApiKey() },
                    )
                }
                Text(
                    "Save", color = GfColor.Accent, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.5.sp,
                    modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
                        if (keyInput.isNotBlank()) { vm.setAiApiKey(keyInput); keyInput = "" }
                    },
                )
            }
        }

        Text("MODEL", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.5.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        GfTextField(
            value = modelInput, onValueChange = { modelInput = it }, placeholder = "claude-sonnet-4-5", masked = false,
        )
        LaunchedEffect(modelInput) {
            // Debounce-free: this is a rarely-typed field, save on every change is fine.
            if (modelInput.isNotBlank() && modelInput != model) vm.setAiModel(modelInput)
        }
    }
}

/**
 * On-device backup. Separate from Import & export below on purpose: that moves *sets* between apps
 * and is lossy, this puts the app back exactly as it was (targets, plan, settings and all).
 */
@Composable
private fun BackupSection(vm: AppViewModel) {
    val context = LocalContext.current
    val state by vm.backup.collectAsState()

    LaunchedEffect(Unit) { vm.refreshBackupInfo() }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.exportBackup(context.contentResolver, it) }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.restoreFromFile(context.contentResolver, it) }
    }

    val stored = state.stored
    val storedLabel = stored?.let {
        "Last backup ${relativeTime(it.createdAtMs)} · ${it.records} records"
    } ?: "No backup on this device yet"

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.Surface)
            .border(1.dp, GfColor.Hairline06, RoundedCornerShape(20.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = !state.busy) { vm.backupToDevice() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(msIcon("check_circle"), null, tint = GfColor.Accent, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text("Back up now", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.5.sp)
                Text(storedLabel, color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Divider()

        // Restore is gated behind a confirm tap — it replaces current history.
        val confirming = state.confirmingRestore
        Row(
            Modifier.fillMaxWidth()
                .background(if (confirming) Color(0x24F0774E) else Color.Transparent)
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = !state.busy && stored != null) {
                    if (confirming) vm.restoreFromDevice() else vm.askRestore()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                msIcon(if (confirming) "warning" else "keyboard_arrow_down"), null,
                tint = when {
                    confirming -> GfColor.Coral
                    stored == null -> GfColor.TextFaint
                    else -> GfColor.Accent
                },
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (confirming) "Tap again to replace your data" else "Restore from device",
                    color = if (confirming) GfColor.Coral else if (stored == null) GfColor.TextFaint else GfColor.Text,
                    fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.5.sp,
                )
                Text(
                    if (confirming) {
                        "Your current workouts, targets and plan are replaced by the backup."
                    } else {
                        "Puts back everything from the last backup on this device"
                    },
                    color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500,
                    fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (confirming) {
                Text(
                    "Cancel",
                    color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.5.sp,
                    modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { vm.cancelRestore() },
                )
            }
        }
        Divider()
        Row(
            Modifier.fillMaxWidth()
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = !state.busy) {
                    saveLauncher.launch("forge-backup-${fileStamp()}.json")
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(msIcon("keyboard_arrow_up"), null, tint = GfColor.Accent, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text("Save a copy off device", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.5.sp)
                Text(
                    "The on-device backup goes away if Forge is uninstalled — keep a copy elsewhere",
                    color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500,
                    fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(msIcon("chevron_right"), null, tint = GfColor.TextFaint, modifier = Modifier.size(20.dp))
        }
        Divider()
        Row(
            Modifier.fillMaxWidth()
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = !state.busy) { openLauncher.launch("*/*") }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(msIcon("exercise"), null, tint = GfColor.Accent, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text("Restore from a file", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.5.sp)
                Text("Pick a saved forge-backup .json", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(msIcon("chevron_right"), null, tint = GfColor.TextFaint, modifier = Modifier.size(20.dp))
        }

        if (state.busy || state.lastResult != null || state.lastError != null) {
            Divider()
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    state.busy -> {
                        Icon(msIcon("monitoring"), null, tint = GfColor.TextFaint, modifier = Modifier.size(16.dp))
                        Text("Working…", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp)
                    }
                    state.lastError != null -> {
                        Icon(msIcon("warning"), null, tint = GfColor.Coral, modifier = Modifier.size(16.dp))
                        Text(state.lastError.orEmpty(), color = GfColor.Coral, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp, lineHeight = 17.sp)
                    }
                    else -> {
                        Icon(msIcon("check_circle"), null, tint = GfColor.Accent, modifier = Modifier.size(16.dp))
                        Text(state.lastResult.orEmpty(), color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
    }
}

private fun fileStamp(): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

@Composable
private fun ImportExportSection(vm: AppViewModel) {
    val context = LocalContext.current
    val state by vm.importExport.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.importCsv(context.contentResolver, it) }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { vm.exportCsv(context.contentResolver, it) }
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.Surface)
            .border(1.dp, GfColor.Hairline06, RoundedCornerShape(20.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = !state.busy) { importLauncher.launch("*/*") }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(msIcon("keyboard_arrow_down"), null, tint = GfColor.Accent, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text("Import workouts", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.5.sp)
                Text("From Hevy, Strong, FitNotes or a Forge export (.csv)", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(msIcon("chevron_right"), null, tint = GfColor.TextFaint, modifier = Modifier.size(20.dp))
        }
        Divider()
        Row(
            Modifier.fillMaxWidth()
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = !state.busy) { exportLauncher.launch("forge_export.csv") }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(msIcon("keyboard_arrow_up"), null, tint = GfColor.Accent, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text("Export workouts", color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.5.sp)
                Text("Every logged set as a re-importable .csv", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(msIcon("chevron_right"), null, tint = GfColor.TextFaint, modifier = Modifier.size(20.dp))
        }
        if (state.busy || state.lastResult != null || state.lastError != null) {
            Divider()
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    state.busy -> {
                        Icon(msIcon("monitoring"), null, tint = GfColor.TextFaint, modifier = Modifier.size(16.dp))
                        Text("Working…", color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp)
                    }
                    state.lastError != null -> {
                        Icon(msIcon("warning"), null, tint = GfColor.Coral, modifier = Modifier.size(16.dp))
                        Text(state.lastError.orEmpty(), color = GfColor.Coral, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp, lineHeight = 17.sp)
                    }
                    else -> {
                        Icon(msIcon("check_circle"), null, tint = GfColor.Accent, modifier = Modifier.size(16.dp))
                        Text(state.lastResult.orEmpty(), color = GfColor.TextDim, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncSection(vm: AppViewModel) {
    val ui by vm.syncUi.collectAsState()
    var urlInput by remember(ui.state.serverUrl) { mutableStateOf(ui.state.serverUrl) }
    val connected = ui.state.serverUrl.isNotBlank()

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GfColor.Surface)
            .border(1.dp, GfColor.Hairline06, RoundedCornerShape(20.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(msIcon(if (connected) "check_circle" else "info"), null, tint = if (connected) GfColor.Accent else GfColor.TextFaint, modifier = Modifier.size(16.dp))
            Text(
                if (connected) "Server configured" else "No sync server set", color = GfColor.TextDim, fontFamily = Manrope,
                fontWeight = FontWeight.W600, fontSize = 12.5.sp,
            )
        }
        Text(
            "Point this at your own Oracle-hosted sync endpoint to back up and sync your logs across " +
                "devices. This is early groundwork — your data stays fully usable on-device whether or not it's set.",
            color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp,
            lineHeight = 17.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )

        GfTextField(value = urlInput, onValueChange = { urlInput = it }, placeholder = "https://your-server.example.com", masked = false)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            Text(
                "Save", color = GfColor.Accent, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.5.sp,
                modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { vm.setSyncServerUrl(urlInput) },
            )
        }

        Divider()

        val pending = ui.state.queue.size
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (pending == 0) "Up to date" else "$pending change${if (pending == 1) "" else "s"} queued",
                    color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.5.sp,
                )
                when {
                    ui.state.lastError != null -> Text(ui.state.lastError.orEmpty(), color = GfColor.Coral, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                    ui.state.lastSyncAtMs > 0 -> Text("Last synced ${relativeTime(ui.state.lastSyncAtMs)}", color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
            Box(
                Modifier.clip(RoundedCornerShape(12.dp)).background(if (ui.busy) GfColor.Hairline12 else GfColor.Accent)
                    .clickable(remember { MutableInteractionSource() }, indication = null, enabled = !ui.busy) { vm.syncNow() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(if (ui.busy) "Syncing…" else "Sync now", color = if (ui.busy) GfColor.TextDim else GfColor.OnAccent, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp)
            }
        }
    }
}

private fun relativeTime(ms: Long): String {
    val mins = (System.currentTimeMillis() - ms) / 60_000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins / 60}h ago"
        // Past a week "412d ago" stops being readable — and a backup's age is exactly the number a
        // user needs to judge at a glance, so fall back to a real date.
        mins < 10_080 -> "${mins / 1440}d ago"
        else -> java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.US).format(java.util.Date(ms))
    }
}

@Composable
private fun GfTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, masked: Boolean) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GfColor.Background)
            .border(1.dp, GfColor.Hairline08, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.5.sp)
        }
        BasicTextField(
            value = value, onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = GfColor.Text, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.5.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(GfColor.Accent),
            visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
        )
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
