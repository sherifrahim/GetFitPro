package com.getfit.ui

import com.getfit.data.db.Curated
import com.getfit.data.prefs.PlanItemData
import com.getfit.di.AppContainer
import com.getfit.domain.LoggedSet
import com.getfit.domain.Phase
import com.getfit.domain.SessionItem
import com.getfit.domain.SessionState
import com.getfit.domain.addRest
import com.getfit.domain.adjustR
import com.getfit.domain.adjustW
import com.getfit.domain.advanceFromRest
import com.getfit.domain.bestFor
import com.getfit.domain.doneSet
import com.getfit.domain.isBW
import com.getfit.domain.startSession
import com.getfit.domain.tick
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

    init {
        scope.launch { container.settingsStore.flow.collect { units = it.units } }
    }

    fun start(items: List<PlanItemData>) {
        scope.launch {
            val settings = container.settingsStore.flow.first()
            val exs = container.exerciseRepo.byIds(items.map { it.id }).associateBy { it.id }
            val logsByEx = container.exerciseRepo.logs.first().groupBy { it.exerciseId }

            val sessItems = items.mapNotNull { pi ->
                val e = exs[pi.id] ?: return@mapNotNull null
                val bw = isBW(e.equipment, e.reps)
                val last = logsByEx[pi.id]?.maxByOrNull { it.dateMs }?.weight
                val suggest = last ?: Curated.DEFAULT_WEIGHT[pi.id] ?: 20.0
                SessionItem(pi.id, e.name, e.muscle, pi.sets, pi.reps, bw, suggest)
            }
            if (sessItems.isEmpty()) return@launch

            val preBest = sessItems.associate { si ->
                val ls = logsByEx[si.id].orEmpty().map { LoggedSet(it.exerciseId, it.weight, it.reps, it.dateMs) }
                val b = bestFor(ls, si.bw)
                si.id to ((b?.weight ?: 0.0) to (b?.reps ?: 0))
            }
            _state.value = startSession(sessItems, settings.restDefault, preBest)
            startTicker()
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(1000)
                val s = _state.value ?: break
                if (s.phase == Phase.DONE) break
                _state.update { it?.let(::tick) }
            }
        }
    }

    fun doneSet() = _state.update { s ->
        s ?: return@update null
        val r = doneSet(s, units)
        if (r.pr) toast("New personal record!", "local_fire_department")
        r.state
    }

    fun skip() = _state.update { s -> if (s?.phase == Phase.REST) advanceFromRest(s, false) else s }
    fun addRest(delta: Int) = _state.update { it?.let { s -> addRest(s, delta) } }
    fun togglePause() = _state.update { it?.copy(paused = !it.paused) }
    fun incW() = _state.update { it?.let { s -> adjustW(s, 1, units) } }
    fun decW() = _state.update { it?.let { s -> adjustW(s, -1, units) } }
    fun incR() = _state.update { it?.let { s -> adjustR(s, 1) } }
    fun decR() = _state.update { it?.let { s -> adjustR(s, -1) } }

    /** Persist the session (if anything logged) and clear. */
    suspend fun endAndSave() {
        ticker?.cancel()
        val s = _state.value
        if (s != null && s.log.isNotEmpty()) {
            container.workoutRepo.saveSession(
                now = System.currentTimeMillis(),
                name = "Push Day",
                durationSec = s.elapsed,
                sets = s.log.map { SessionSaveSet(it.id, it.name, it.weight, it.reps) },
                volume = Math.round(s.volume).toInt(),
                prs = s.newPRs.size,
            )
            toast("Workout saved", "check_circle")
        }
        _state.value = null
    }
}
