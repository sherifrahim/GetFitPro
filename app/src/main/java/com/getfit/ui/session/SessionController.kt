package com.getfit.ui

import com.getfit.core.util.SessionFeedback
import com.getfit.data.db.Curated
import com.getfit.data.prefs.PlanItemData
import com.getfit.data.prefs.Routine
import com.getfit.di.AppContainer
import com.getfit.domain.HrPoint
import com.getfit.domain.LoggedSet
import com.getfit.domain.addHeartRate
import com.getfit.domain.addItem
import com.getfit.domain.convertSessionUnits
import com.getfit.domain.removeItem
import com.getfit.domain.replaceItem
import com.getfit.domain.skipExercise
import com.getfit.domain.avgBpm
import com.getfit.domain.estimateCalories
import com.getfit.domain.maxBpm
import com.getfit.domain.Phase
import com.getfit.domain.SessionItem
import com.getfit.domain.SessionState
import com.getfit.domain.Units
import com.getfit.domain.addRest
import com.getfit.domain.adjustR
import com.getfit.domain.adjustW
import com.getfit.domain.advanceFromRest
import com.getfit.domain.bestFor
import com.getfit.domain.doneSet
import com.getfit.domain.isBW
import com.getfit.domain.pauseSession
import com.getfit.domain.startSession
import com.getfit.domain.tick
import com.getfit.domain.togglePause
import com.getfit.data.repo.SessionSaveSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drives the pure SessionEngine: builds session items from the plan, runs a 1s ticker, applies
 * user actions and persists the completed session. UI reads [state] (nullable = no active session).
 *
 * Timing is wall-clock anchored (see SessionEngine.kt doc): the ticker's job is just to trigger a
 * UI recomposition every second, not to BE the clock. Every call into the engine passes the real
 * `now`, so a throttled ticker (screen off, Doze) or a process restart mid-session self-corrects on
 * the very next tick instead of drifting or resuming from a stale counted value.
 */
class SessionController(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val toast: (String, String) -> Unit,
) {
    private val _state = MutableStateFlow<SessionState?>(null)
    val state: StateFlow<SessionState?> = _state.asStateFlow()

    private var ticker: Job? = null
    @Volatile private var units: String = "kg"
    @Volatile private var autorest: Boolean = true
    @Volatile private var soundOn: Boolean = true
    @Volatile private var hapticsOn: Boolean = true
    @Volatile private var prNotify: Boolean = true

    init {
        scope.launch {
            container.settingsStore.flow.collect {
                // A kg<->lb toggle mid-session converts every weight the state holds (see
                // convertSessionUnits) — the session works in display units end to end.
                val prev = units
                if (prev != it.units && _state.value != null) {
                    _state.update { s -> s?.let { st -> convertSessionUnits(st, prev, it.units) } }
                    persist()
                }
                units = it.units; autorest = it.autorest; soundOn = it.sound; hapticsOn = it.haptics; prNotify = it.prNotify
            }
        }
        // Restore an in-progress session after process death, resyncing its clock immediately
        // (rather than waiting for the next tick) so a long gap since the last persist — the app
        // having been closed, not just backgrounded — is reflected right away.
        scope.launch {
            val saved = container.sessionStore.load()
            if (saved != null) {
                val now = System.currentTimeMillis()
                _state.value = if (saved.phase != Phase.DONE) tick(saved, now) else saved
                if (saved.phase != Phase.DONE) startTicker()
            }
        }
    }

    /** Persist current session state (or clear it when the session ends). */
    private fun persist() = scope.launch {
        val s = _state.value
        if (s == null) container.sessionStore.clear() else container.sessionStore.save(s)
    }

    /** Start a session for a whole routine. The saved record is named after it and, on finish,
     *  the rotation moves to the routine after it. */
    fun start(routine: Routine) = start(routine.items, routine.name, routine.id)

    /** An ad-hoc session (e.g. one exercise from its detail screen): no routine, no rotation. */
    fun start(items: List<PlanItemData>, name: String = "Workout", routineId: String = "") {
        scope.launch {
            val settings = container.settingsStore.flow.first()
            val u = settings.units
            val exs = container.exerciseRepo.byIds(items.map { it.id }).associateBy { it.id }
            val logsByEx = container.exerciseRepo.logs.first().groupBy { it.exerciseId }

            // Session operates in the user's DISPLAY unit; kg is converted in/out at the boundary.
            val sessItems = items.mapNotNull { pi ->
                val e = exs[pi.id] ?: return@mapNotNull null
                val bw = isBW(e.equipment, e.reps)
                val lastKg = logsByEx[pi.id]?.maxByOrNull { it.dateMs }?.weight
                val suggestKg = lastKg ?: Curated.DEFAULT_WEIGHT[pi.id] ?: 20.0
                SessionItem(pi.id, e.name, e.muscle, pi.sets, pi.reps, bw, Units.roundDisplay(Units.toDisplay(suggestKg, u)), e.equipment, pi.superset)
            }
            if (sessItems.isEmpty()) return@launch

            val preBest = sessItems.associate { si ->
                val ls = logsByEx[si.id].orEmpty().map { LoggedSet(it.exerciseId, it.weight, it.reps, it.dateMs) }
                val b = bestFor(ls, si.bw)
                si.id to ((b?.let { Units.toDisplay(it.weight, u) } ?: 0.0) to (b?.reps ?: 0))
            }
            _state.value = startSession(sessItems, settings.restDefault, preBest, System.currentTimeMillis())
                .copy(name = name, routineId = routineId)
            startTicker()
            persist()
        }
    }

    /** Watch heart-rate batch. Kept on the state (downsampled) so it survives process death and
     *  lands in the saved session for the workout detail's HR graph. */
    fun recordHeartRate(samples: List<HrPoint>) {
        _state.update { it?.let { s -> addHeartRate(s, samples) } }
        persist()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(1000)
                val s = _state.value ?: break
                if (s.phase == Phase.DONE) break
                // Stays an atomic update rather than a read-compute-write: skip()/addRest()/
                // doneSet()/togglePause() all mutate _state from the main thread while this ticker
                // runs, and a plain `_state.value = next` would silently clobber a tap that landed
                // after this iteration read the state. `update` re-runs its lambda on contention,
                // so restJustEnded always reflects the attempt that actually won.
                var restJustEnded = false
                _state.update { cur ->
                    if (cur == null) return@update null
                    val next = tick(cur, System.currentTimeMillis())
                    restJustEnded = cur.phase == Phase.REST && next.phase == Phase.WORK
                    next
                }
                // Rest ending here means it expired naturally (not a user tap on Skip/±15, which
                // already have their own tap-driven feedback in SessionScreen.kt) — the one point in
                // the flow with no Compose click handler to hang a cue off, so it's fired here
                // instead. advanceFromRest always lands on WORK, never DONE, so this check alone is
                // exhaustive for "rest just ended" whether that's the next set or the next exercise.
                // Fired after the state write, never before: ToneGenerator construction can block
                // for tens of ms, and holding the write behind it is what created the race above.
                if (restJustEnded) {
                    SessionFeedback.restEnded(container.appContext, soundOn, hapticsOn)
                }
            }
        }
    }

    fun doneSet() {
        _state.update { s ->
            s ?: return@update null
            val r = doneSet(s, units, System.currentTimeMillis())
            // The PR is still recorded either way; the pref only silences the live celebration.
            if (r.pr && prNotify) toast("New personal record!", "local_fire_department")
            // If auto-start-rest is off, hold the rest timer paused until the user starts/skips it.
            if (r.state.phase == Phase.REST && !autorest) pauseSession(r.state, System.currentTimeMillis()) else r.state
        }
        persist()
    }

    fun skip() {
        _state.update { s -> if (s?.phase == Phase.REST) advanceFromRest(s, false, System.currentTimeMillis()) else s }
        persist()
    }
    fun addRest(delta: Int) {
        val now = System.currentTimeMillis()
        _state.update { it?.let { s -> addRest(s, delta, now) } }
        persist()
    }
    fun togglePause() {
        _state.update { it?.let { s -> togglePause(s, System.currentTimeMillis()) } }
        persist()
    }
    fun incW() { _state.update { it?.let { s -> adjustW(s, 1, units) } }; persist() }
    /** Typed entry: an exact weight / rep count from the keyboard, on top of the steppers. */
    fun setW(v: Double) { _state.update { it?.let { s -> s.copy(curW = Units.roundDisplay(v.coerceIn(0.0, 2000.0))) } }; persist() }
    fun setR(v: Int) { _state.update { it?.let { s -> s.copy(curR = v.coerceIn(1, 999)) } }; persist() }
    fun decW() { _state.update { it?.let { s -> adjustW(s, -1, units) } }; persist() }
    fun incR() { _state.update { it?.let { s -> adjustR(s, 1) } }; persist() }
    fun decR() { _state.update { it?.let { s -> adjustR(s, -1) } }; persist() }

    /** Builds a session item for [exerciseId] the same way [start] does: prefill from the last log. */
    private suspend fun buildItem(exerciseId: String, sets: Int, reps: String?): SessionItem? {
        val e = container.exerciseRepo.byId(exerciseId) ?: return null
        val u = container.settingsStore.flow.first().units
        val bw = isBW(e.equipment, e.reps)
        val lastKg = container.exerciseRepo.lastWeight(exerciseId)
        val suggestKg = lastKg ?: Curated.DEFAULT_WEIGHT[exerciseId] ?: 20.0
        return SessionItem(e.id, e.name, e.muscle, sets, reps ?: e.reps, bw, Units.roundDisplay(Units.toDisplay(suggestKg, u)), e.equipment)
    }

    // ---- mid-workout edits (see SessionEdits.kt) ----

    /** Hevy's "Add exercise" on the active workout: appended after the planned ones. */
    fun addExercise(exerciseId: String, sets: Int = 3) {
        scope.launch {
            val item = buildItem(exerciseId, sets, null) ?: return@launch
            _state.update { it?.let { s -> addItem(s, item) } }
            persist()
        }
    }

    /** Swap the current exercise for another; sets already logged for it stay. */
    fun replaceCurrent(exerciseId: String) {
        scope.launch {
            val cur = _state.value?.current ?: return@launch
            val item = buildItem(exerciseId, cur.sets, null) ?: return@launch
            _state.update { it?.let { s -> replaceItem(s, s.idx, item, System.currentTimeMillis()) } }
            persist()
        }
    }

    fun skipExercise() {
        _state.update { it?.let { s -> skipExercise(s, System.currentTimeMillis()) } }
        persist()
    }

    fun removeCurrent() {
        _state.update { it?.let { s -> removeItem(s, s.idx, System.currentTimeMillis()) } }
        persist()
    }

    /** Persist the session (if anything logged) and clear. */
    suspend fun endAndSave() {
        ticker?.cancel()
        val s = _state.value
        if (s != null && s.log.isNotEmpty()) {
            val u = units
            val bwById = s.items.associate { it.id to it.bw }
            // Convert logged display-unit weights back to canonical kg for storage.
            val setsKg = s.log.map { SessionSaveSet(it.id, it.name, Units.fromDisplay(it.weight, u), it.reps) }
            val volumeKg = setsKg.sumOf { if (bwById[it.exerciseId] == true) 0.0 else it.weight * it.reps }
            val avg = avgBpm(s.hr)
            val profile = container.settingsStore.flow.first()
            container.workoutRepo.saveSession(
                now = System.currentTimeMillis(),
                name = s.name,
                durationSec = s.elapsed,
                sets = setsKg,
                volume = Math.round(volumeKg).toInt(),
                prs = s.newPRs.size,
                routineId = s.routineId,
                avgBpm = avg,
                maxBpm = maxBpm(s.hr),
                calories = estimateCalories(s.elapsed, avg, profile.bodyWeightKg, profile.ageYears(), profile.isMale()),
                heartRate = s.hr,
            )
            container.workoutRepo.advanceAfter(s.routineId)
            toast("Workout saved", "check_circle")
        }
        _state.value = null
        container.sessionStore.clear()
    }
}
