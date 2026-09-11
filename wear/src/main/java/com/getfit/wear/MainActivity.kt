package com.getfit.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.getfit.wear.data.ActionKind
import com.getfit.wear.data.HeartRateBatch
import com.getfit.wear.data.HeartRateMonitor
import com.getfit.wear.data.HeartRateSample
import com.getfit.wear.data.SessionSnapshot
import com.getfit.wear.data.SnapshotBus
import com.getfit.wear.data.WatchAction
import com.getfit.wear.data.WatchMessenger
import com.getfit.wear.data.WearPhase
import com.getfit.wear.theme.ForgeWearColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    companion object {
        /** Read by ForgeListenerService to decide whether a snapshot needs to launch us. */
        @Volatile var inForeground: Boolean = false
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
        // Ask the phone for the current session, so opening the app mid-workout shows it immediately
        // instead of waiting for the phone's next state change.
        messenger.sendAction(WatchAction(ActionKind.REQUEST_STATE))
    }

    override fun onPause() {
        inForeground = false
        super.onPause()
    }

    private val messenger by lazy { WatchMessenger(this) }
    private val heartRateMonitor by lazy { HeartRateMonitor(this) }

    // Must be registered unconditionally before the activity leaves CREATED — doing it here as a
    // property initializer (not inside onCreate's body) is the safe place for that.
    private val requestSensors = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Both are needed for heart rate on current Wear OS (see the manifest comment); asking for
        // the pair in one prompt avoids a second dialog on the next launch.
        val wanted = listOf(Manifest.permission.BODY_SENSORS, "android.permission.health.READ_HEART_RATE")
            .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (wanted.isNotEmpty()) requestSensors.launch(wanted.toTypedArray())
        setContent {
            MaterialTheme(colors = ForgeWearColors) {
                ForgeWearApp(messenger, heartRateMonitor)
            }
        }
    }
}

/**
 * Top-level state holder + screen router. No ViewModel here on purpose — this activity's state is
 * simple enough (one nullable snapshot, one nullable BPM reading) that adding a new dependency
 * (lifecycle-viewmodel-compose, not already on this module's classpath) for it isn't worth the risk
 * given zero build verification elsewhere in this module; plain remember/DisposableEffect/
 * LaunchedEffect covers it.
 */
@Composable
private fun ForgeWearApp(messenger: WatchMessenger, heartRateMonitor: HeartRateMonitor) {
    var snapshot by remember { mutableStateOf<SessionSnapshot?>(null) }
    var bpm by remember { mutableStateOf<Double?>(null) }
    val pendingHrSamples = remember { mutableListOf<HeartRateSample>() }
    val context = LocalContext.current

    // Snapshots arrive via ForgeListenerService (app open or not) and land on SnapshotBus; this is
    // the only consumer. A new snapshot landing while the watch was showing Rest and now shows Work
    // means rest just ended — the phone's own ticker drives this (see SessionController.kt), so the
    // watch doesn't run a duplicate timer, it just reacts to what the phone already decided.
    LaunchedEffect(Unit) {
        SnapshotBus.snapshot.collect { newSnapshot ->
            if (newSnapshot != null && snapshot?.phase == WearPhase.REST && newSnapshot.phase == WearPhase.WORK) {
                vibrateRestEnd(context)
            }
            snapshot = newSnapshot
        }
    }

    // Heart rate only runs while a workout is actually in progress (not Idle, not the brief DONE
    // summary) — starting/stopping ExerciseClient outside that window would just burn battery for
    // no reason. Samples are buffered locally and flushed as one batch every 8s (see
    // docs/wear-companion-design.md section 3): continuous per-sample streaming over the radio is
    // the real battery cost in most watch apps, not the sensor sampling itself.
    LaunchedEffect(snapshot?.active, snapshot?.phase) {
        val trackingActive = snapshot?.active == true && snapshot?.phase != WearPhase.DONE
        if (!trackingActive) {
            heartRateMonitor.stop()
            bpm = null
            return@LaunchedEffect
        }
        heartRateMonitor.start { newBpm ->
            bpm = newBpm
            pendingHrSamples += HeartRateSample(newBpm, System.currentTimeMillis())
        }
        try {
            while (isActive) {
                delay(8_000)
                if (pendingHrSamples.isNotEmpty()) {
                    messenger.sendHeartRateBatch(HeartRateBatch(pendingHrSamples.toList()))
                    pendingHrSamples.clear()
                }
            }
        } finally {
            heartRateMonitor.stop()
        }
    }

    // Ticks purely to trigger recomposition — the actual elapsed/rest-left values are always
    // recomputed from the snapshot's wall-clock anchors (elapsedSecFrom/restLeftSecFrom below), the
    // same pattern SessionEngine.kt uses on the phone, so a delayed or missed tick self-corrects
    // instead of drifting.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }

    val s = snapshot
    when {
        s == null || !s.active -> IdleScreen(
            snapshot = s,
            onStart = { messenger.sendAction(WatchAction(ActionKind.START_ROUTINE, routineId = it)) },
            onSelect = { messenger.sendAction(WatchAction(ActionKind.SELECT_ROUTINE, routineId = it)) },
        )
        s.phase == WearPhase.DONE -> SessionEndScreen(s)
        s.phase == WearPhase.REST -> RestScreen(
            snapshot = s,
            bpm = bpm,
            nowMs = nowMs,
            onSkip = { messenger.sendAction(WatchAction(ActionKind.SKIP_REST)) },
            onAddRest = { messenger.sendAction(WatchAction(ActionKind.ADJUST_REST, restDeltaSec = 15)) },
        )
        else -> ActiveExerciseScreen(
            snapshot = s,
            bpm = bpm,
            onDone = { messenger.sendAction(WatchAction(ActionKind.DONE_SET)) },
        )
    }
}

/**
 * No session running. With routines on hand this is the watch's own start screen: the one up next
 * on top as a Start chip, the others below — tapping one of those makes IT the next workout (the
 * phone's Home card follows), then Start begins it. Without routines (phone app never opened since
 * the update) it falls back to pointing at the phone.
 */
@Composable
private fun IdleScreen(snapshot: SessionSnapshot?, onStart: (String) -> Unit, onSelect: (String) -> Unit) {
    val routines = snapshot?.routines.orEmpty()
    val current = routines.firstOrNull { it.id == snapshot?.currentRoutineId } ?: routines.firstOrNull()
    if (current == null) {
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Forge", color = ForgeWearColors.primary, style = MaterialTheme.typography.title2)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Open Forge on your phone to start a workout",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption2,
                    color = ForgeWearColors.onBackground,
                )
            }
        }
        return
    }
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text("Up next", style = MaterialTheme.typography.caption2, color = ForgeWearColors.onSurfaceVariant)
        }
        item {
            Chip(
                onClick = { onStart(current.id) },
                label = { Text(current.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                secondaryLabel = { Text("Start · ${current.exercises} exercises") },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val others = routines.filter { it.id != current.id }
        if (others.isNotEmpty()) {
            item {
                Text("Tap to do next", style = MaterialTheme.typography.caption2, color = ForgeWearColors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            items(others.size) { i ->
                val r = others[i]
                Chip(
                    onClick = { onSelect(r.id) },
                    label = { Text(r.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    secondaryLabel = { Text("${r.exercises} exercises · ${r.sets} sets") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ActiveExerciseScreen(snapshot: SessionSnapshot, bpm: Double?, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                snapshot.exerciseName,
                style = MaterialTheme.typography.caption1,
                color = ForgeWearColors.primary,
                maxLines = 1,
            )
            Text(
                "Set ${snapshot.setNum}/${snapshot.totalSetsForExercise}",
                style = MaterialTheme.typography.caption2,
                color = ForgeWearColors.onBackground,
            )
            Text(
                if (snapshot.bodyweight) "${snapshot.curReps} reps"
                else "${fmtWeight(snapshot.curWeight)} ${snapshot.units} × ${snapshot.curReps}",
                style = MaterialTheme.typography.display3,
                color = ForgeWearColors.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Chip(onClick = onDone, label = { Text("Done") })
            bpm?.let {
                Spacer(Modifier.height(4.dp))
                Text("${it.toInt()} bpm", style = MaterialTheme.typography.caption2, color = ForgeWearColors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RestScreen(
    snapshot: SessionSnapshot,
    bpm: Double?,
    nowMs: Long,
    onSkip: () -> Unit,
    onAddRest: () -> Unit,
) {
    val restLeft = restLeftSecFrom(snapshot, nowMs)
    val progress = if (snapshot.restTotal > 0) (restLeft.toFloat() / snapshot.restTotal.toFloat()).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            indicatorColor = ForgeWearColors.primary,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Rest", style = MaterialTheme.typography.caption1, color = ForgeWearColors.onBackground)
            Text("${restLeft}s", style = MaterialTheme.typography.display2, color = ForgeWearColors.primary)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(onClick = onAddRest, label = { Text("+15") })
                Chip(onClick = onSkip, label = { Text("Skip") })
            }
            bpm?.let {
                Spacer(Modifier.height(4.dp))
                Text("${it.toInt()} bpm", style = MaterialTheme.typography.caption2, color = ForgeWearColors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SessionEndScreen(snapshot: SessionSnapshot) {
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Workout complete",
                style = MaterialTheme.typography.title3,
                color = ForgeWearColors.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${snapshot.completedSets}/${snapshot.totalSets} sets",
                style = MaterialTheme.typography.caption1,
                color = ForgeWearColors.onBackground,
            )
        }
    }
}

private fun restLeftSecFrom(s: SessionSnapshot, nowMs: Long): Int =
    ((s.restEndAtMs - nowMs) / 1000).toInt().coerceAtLeast(0)

private fun fmtWeight(w: Double): String =
    if (w == w.toLong().toDouble()) w.toLong().toString() else String.format("%.1f", w)

/** Same cue as the phone's SessionFeedback.restEnded (core/util/SessionFeedback.kt) — duplicated
 *  rather than shared, matching this module's established pattern (see WearProtocol.kt) of small
 *  standalone pieces over a shared module. No sound cue on the watch (a watch is worn, not held up
 *  to listen to — vibration is the expected cue there); the phone still plays its own tone. */
private fun vibrateRestEnd(context: Context) {
    runCatching {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80), -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(effect)
        }
    }
}
