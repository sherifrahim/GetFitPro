package com.getfit.ui

import com.getfit.data.db.Curated
import com.getfit.data.prefs.PlanItemData
import com.getfit.di.AppContainer
import com.getfit.domain.LoggedSet
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

    init {
        scope.launch { container.settingsStore.flow.collect { units = it.units; autorest = it.autorest } }
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

    fun start(items: List<PlanItemData>) {
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
                SessionItem(pi.id, e.name, e.muscle, pi.sets, pi.reps, bw, Units.roundDisplay(Units.toDisplay(suggestKg, u)))
            }
            if (sessItems.isEmpty()) return@launch

            val preBest = sessItems.associate { si ->
                val ls = logsByEx[si.id].orEmpty().map { LoggedSet(it.exerciseId, it.weight, it.reps, it.dateMs) }
                val b = bestFor(ls, si.bw)
                si.id to ((b?.let { Units.toDisplay(it.weight, u) } ?: 0.0) to (b?.reps ?: 0))
            }
            _state.value = startSession(sessItems, settings.restDefault, preBest, System.currentTimeMillis())
            startTicker()
            persist()
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(1000)
                val s = _state.value ?: break
                if (s.phase == Phase.DONE) break
                _state.update { it?.let { cur -> tick(cur, System.currentTimeMillis()) } }
            }
        }
    }

    fun doneSet() {
        _state.update { s ->
            s ?: return@update null
            val r = doneSet(s, units, System.currentTimeMillis())
            if (r.pr) toast("New personal record!", "local_fire_department")
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
    fun decW() { _state.update { it?.let { s -> adjustW(s, -1, units) } }; persist() }
    fun incR() { _state.update { it?.let { s -> adjustR(s, 1) } }; persist() }
    fun decR() { _state.update { it?.let { s -> adjustR(s, -1) } }; persist() }

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
            container.workoutRepo.saveSession(
                now = System.currentTimeMillis(),
                name = "Push Day",
                durationSec = s.elapsed,
                sets = setsKg,
                volume = Math.round(volumeKg).toInt(),
                prs = s.newPRs.size,
            )
            toast("Workout saved", "check_circle")
        }
        _state.value = null
        container.sessionStore.clear()
    }
}
