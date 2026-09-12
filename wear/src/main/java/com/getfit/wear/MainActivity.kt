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
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.getfit.domain.HrPoint
import com.getfit.data.wear.ActionKind
import com.getfit.data.wear.HeartRateBatch
import com.getfit.wear.data.HeartRateMonitor
import com.getfit.data.wear.HeartRateSample
import com.getfit.wear.data.LocalSession
import com.getfit.data.wear.SessionSnapshot
import com.getfit.wear.data.SnapshotBus
import com.getfit.data.wear.WatchAction
import com.getfit.wear.data.WatchMessenger
import com.getfit.wear.data.WatchStore
import com.getfit.data.wear.WearPhase
import com.getfit.wear.data.localSnapshot
import com.getfit.wear.theme.ForgeWearColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    companion object {
        /** Read by ForgeListenerService to decide whether a snapshot needs to launch us. */
        @Volatile var inForeground: Boolean = false
        /** Intent extra from the complication: routine id to start on arrival ("" = whichever is next). */
        const val EXTRA_START_ROUTINE = "com.getfit.wear.START_ROUTINE"
    }

    /** A start request from the complication, consumed by the composable once it is up. */
    private val startRequest = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        readStartRequest(intent)
    }

    private fun readStartRequest(intent: android.content.Intent?) {
        if (intent?.hasExtra(EXTRA_START_ROUTINE) != true) return
        startRequest.value = intent.getStringExtra(EXTRA_START_ROUTINE).orEmpty()
        intent.removeExtra(EXTRA_START_ROUTINE)
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
        // Ask the phone for the current session, so opening the app mid-workout shows it immediately
        // instead of waiting for the phone's next state change. Also the moment to retry uploads.
        messenger.sendAction(WatchAction(ActionKind.REQUEST_STATE))
        store.pendingUploads().forEach { messenger.sendUpload(it) }
    }

    override fun onPause() {
        inForeground = false
        super.onPause()
    }

    private val messenger by lazy { WatchMessenger(this) }
    private val heartRateMonitor by lazy { HeartRateMonitor(this) }
    private val store by lazy { WatchStore(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val localSession by lazy { LocalSession(store, appScope) }

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
        readStartRequest(intent)
        setContent {
            MaterialTheme(colors = ForgeWearColors) {
                ForgeWearApp(messenger, heartRateMonitor, store, localSession, startRequest)
            }
        }
    }
}

/**
 * Top-level state holder + screen router. No ViewModel here on purpose — this activity's state is
 * simple enough (one nullable snapshot, one nullable BPM reading, one local session) that plain
 * remember/LaunchedEffect covers it without a new dependency on this module.
 *
 * Two sources of truth, one screen set:
 *  - a snapshot from the phone (mirror mode — the phone runs the engine, we render and send taps);
 *  - a [LocalSession] (standalone mode — no phone in range, the watch runs the shared engine itself).
 * Mirror wins whenever the phone reports an active session; the standalone one only starts when the
 * phone didn't answer a Start within a few seconds.
 */
@Composable
private fun ForgeWearApp(
    messenger: WatchMessenger,
    heartRateMonitor: HeartRateMonitor,
    store: WatchStore,
    local: LocalSession,
    startRequest: kotlinx.coroutines.flow.MutableStateFlow<String?>,
) {
    var snapshot by remember { mutableStateOf<SessionSnapshot?>(null) }
    var bpm by remember { mutableStateOf<Double?>(null) }
    val pendingHrSamples = remember { mutableListOf<HeartRateSample>() }
    val context = LocalContext.current
    val localState by local.state.collectAsState()
    var pendingCount by remember { mutableIntStateOf(store.pendingUploads().size) }

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
            // A reachable phone means pending standalone workouts can go now.
            if (newSnapshot != null) store.pendingUploads().forEach { messenger.sendUpload(it) }
        }
    }
    LaunchedEffect(Unit) { SnapshotBus.acked.collect { pendingCount = store.pendingUploads().size } }

    // Local rest-end cue: same vibration the mirror gets from the phone's transition.
    var lastLocalPhase by remember { mutableStateOf<WearPhase?>(null) }
    val localSnap = localState?.let { localSnapshot(it, local.units) }
    LaunchedEffect(localSnap?.phase) {
        if (lastLocalPhase == WearPhase.REST && localSnap?.phase == WearPhase.WORK) vibrateRestEnd(context)
        lastLocalPhase = localSnap?.phase
    }

    // Which session is on screen: the phone's if it has one, else ours.
    val phoneActive = snapshot?.active == true
    val shown: SessionSnapshot? = if (phoneActive) snapshot else localSnap
    val isLocal = !phoneActive && localSnap != null

    // Heart rate only runs while a workout is actually in progress (not Idle, not the brief DONE
    // summary) — starting/stopping ExerciseClient outside that window would just burn battery for
    // no reason. Samples are buffered locally and flushed as one batch every 8s (see
    // docs/wear-companion-design.md section 3): continuous per-sample streaming over the radio is
    // the real battery cost in most watch apps, not the sensor sampling itself. In standalone mode
    // the same samples feed the local session instead of the radio.
    LaunchedEffect(shown?.active, shown?.phase, isLocal) {
        val trackingActive = shown?.active == true && shown.phase != WearPhase.DONE
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
                    val batch = pendingHrSamples.toList()
                    pendingHrSamples.clear()
                    if (isLocal) local.recordHeartRate(batch.map { HrPoint(it.atMs, Math.round(it.bpm).toInt()) })
                    else messenger.sendHeartRateBatch(HeartRateBatch(batch))
                }
            }
        } finally {
            heartRateMonitor.stop()
        }
    }

    // Ticks purely to trigger recomposition — the actual elapsed/rest-left values are always
    // recomputed from the snapshot's wall-clock anchors (restLeftSecFrom below), the same pattern
    // SessionEngine.kt uses on the phone, so a delayed or missed tick self-corrects instead of drifting.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }

    // Start on the wrist: ask the phone first; if no active snapshot comes back within 3 s (no
    // phone in range, or its app is closed), run the routine here.
    var starting by remember { mutableStateOf<String?>(null) }
    // Complication tap: start the requested routine (blank = the one up next) unless a session is on.
    LaunchedEffect(Unit) {
        startRequest.collect { req ->
            if (req == null) return@collect
            startRequest.value = null
            if (snapshot?.active == true || local.active) return@collect
            val cache = store.routines()
            val id = req.ifBlank { snapshot?.currentRoutineId?.takeIf { it.isNotBlank() } ?: cache.currentRoutineId }
            if (id.isNotBlank()) starting = id
        }
    }
    LaunchedEffect(starting) {
        val id = starting ?: return@LaunchedEffect
        messenger.sendAction(WatchAction(ActionKind.START_ROUTINE, routineId = id))
        delay(3_000)
        if (snapshot?.active != true) {
            val cache = store.routines()
            cache.routines.firstOrNull { it.id == id }?.let { local.start(it, cache) }
        }
        starting = null
    }

    val s = shown
    when {
        starting != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(indicatorColor = ForgeWearColors.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text("Starting…", style = MaterialTheme.typography.caption1, color = ForgeWearColors.onBackground)
            }
        }
        s == null || !s.active -> IdleScreen(
            snapshot = snapshot,
            cache = store.routines(),
            pendingUploads = pendingCount,
            onStart = { starting = it },
            onSelect = { messenger.sendAction(WatchAction(ActionKind.SELECT_ROUTINE, routineId = it)) },
        )
        s.phase == WearPhase.DONE -> SessionEndScreen(
            snapshot = s,
            local = isLocal,
            onFinish = { local.finish()?.let { messenger.sendUpload(it) }; pendingCount = store.pendingUploads().size },
        )
        s.phase == WearPhase.REST -> RestScreen(
            snapshot = s,
            bpm = bpm,
            nowMs = nowMs,
            onSkip = { if (isLocal) local.skip() else messenger.sendAction(WatchAction(ActionKind.SKIP_REST)) },
            onAddRest = { if (isLocal) local.addRest(15) else messenger.sendAction(WatchAction(ActionKind.ADJUST_REST, restDeltaSec = 15)) },
        )
        else -> ActiveExerciseScreen(
            snapshot = s,
            bpm = bpm,
            local = isLocal,
            lastLine = (snapshot?.routines?.takeIf { it.isNotEmpty() } ?: store.routines().routines)
                .flatMap { it.items }.firstOrNull { it.name == s.exerciseName }?.last?.takeIf { it.isNotBlank() },
            onDone = {
                if (isLocal) { if (local.doneSet()) vibrateRestEnd(context) }
                else messenger.sendAction(WatchAction(ActionKind.DONE_SET))
            },
            onWeight = { d -> if (isLocal) local.adjustW(d) else messenger.sendAction(WatchAction(ActionKind.ADJUST_WEIGHT, delta = d)) },
            onReps = { d -> if (isLocal) local.adjustR(d) else messenger.sendAction(WatchAction(ActionKind.ADJUST_REPS, delta = d)) },
            onEnd = { if (isLocal) { local.finish()?.let { messenger.sendUpload(it) }; pendingCount = store.pendingUploads().size } },
        )
    }
}

/**
 * No session running. With routines on hand this is the watch's own start screen: the one up next
 * on top as a Start chip, the others below — tapping one of those makes IT the next workout (the
 * phone's Home card follows), then Start begins it — on the phone if it's in range, else right
 * here. Without routines (phone app never opened since the update) it falls back to pointing at
 * the phone.
 */
@Composable
private fun IdleScreen(
    snapshot: SessionSnapshot?,
    cache: WatchStore.RoutineCache,
    pendingUploads: Int,
    onStart: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    // Live list from the phone if it just sent one, else what we cached last time.
    val routines = snapshot?.routines?.takeIf { it.isNotEmpty() } ?: cache.routines
    val currentId = snapshot?.currentRoutineId?.takeIf { it.isNotBlank() } ?: cache.currentRoutineId
    val current = routines.firstOrNull { it.id == currentId } ?: routines.firstOrNull()
    if (current == null) {
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Forge", color = ForgeWearColors.primary, style = MaterialTheme.typography.title2)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Open Forge on your phone once to load your routines",
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
        if (pendingUploads > 0) {
            item {
                Text(
                    "$pendingUploads workout${if (pendingUploads == 1) "" else "s"} waiting for the phone",
                    style = MaterialTheme.typography.caption3, color = ForgeWearColors.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ActiveExerciseScreen(
    snapshot: SessionSnapshot,
    bpm: Double?,
    local: Boolean,
    lastLine: String?,
    onDone: () -> Unit,
    onWeight: (Int) -> Unit,
    onReps: (Int) -> Unit,
    onEnd: () -> Unit,
) {
    // Horizontal padding keeps the name and steppers inside the round mask on 1.2" faces.
    Box(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                snapshot.exerciseName,
                style = MaterialTheme.typography.caption1,
                color = ForgeWearColors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                "Set ${snapshot.setNum}/${snapshot.totalSetsForExercise}" + if (local) " · on watch" else "",
                style = MaterialTheme.typography.caption2,
                color = ForgeWearColors.onBackground,
            )
            lastLine?.let {
                Text("Last: $it", style = MaterialTheme.typography.caption3, color = ForgeWearColors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp))
            }
            // Steppers: weight (unless bodyweight) and reps, the phone's stepper cards in 2 rows.
            if (!snapshot.bodyweight) {
                StepperRow("${fmtWeight(snapshot.curWeight)} ${snapshot.units}", onDec = { onWeight(-1) }, onInc = { onWeight(1) })
            }
            StepperRow("${snapshot.curReps} reps", onDec = { onReps(-1) }, onInc = { onReps(1) })
            Spacer(Modifier.height(2.dp))
            Chip(onClick = onDone, label = { Text("Done") }, colors = ChipDefaults.primaryChipColors())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bpm?.let {
                    Text("${it.toInt()} bpm", style = MaterialTheme.typography.caption2, color = ForgeWearColors.onSurfaceVariant)
                }
                if (local) {
                    Text(
                        "End", style = MaterialTheme.typography.caption2, color = ForgeWearColors.onSurfaceVariant,
                        modifier = Modifier.padding(4.dp).clickable(onClick = onEnd),
                    )
                }
            }
        }
    }
}

/** −  value  + on one line, with the smallest wear buttons so the Done chip keeps room. */
@Composable
private fun StepperRow(value: String, onDec: () -> Unit, onInc: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Button(onClick = onDec, modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize), colors = ButtonDefaults.secondaryButtonColors()) { Text("−") }
        Text(value, style = MaterialTheme.typography.title3, color = ForgeWearColors.onBackground, modifier = Modifier.width(78.dp), textAlign = TextAlign.Center, maxLines = 1)
        Button(onClick = onInc, modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize), colors = ButtonDefaults.secondaryButtonColors()) { Text("+") }
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
private fun SessionEndScreen(snapshot: SessionSnapshot, local: Boolean, onFinish: () -> Unit) {
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
            if (local) {
                Spacer(Modifier.height(8.dp))
                Chip(onClick = onFinish, label = { Text("Save") }, colors = ChipDefaults.primaryChipColors())
                Text(
                    "Syncs to the phone when it's near",
                    style = MaterialTheme.typography.caption3, color = ForgeWearColors.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text("Finish on the phone", style = MaterialTheme.typography.caption3, color = ForgeWearColors.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
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
