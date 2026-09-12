package com.getfit.wear.data

import com.getfit.domain.HrPoint
import com.getfit.domain.Phase
import com.getfit.domain.SessionItem
import com.getfit.domain.SessionState
import com.getfit.domain.addHeartRate
import com.getfit.domain.addRest
import com.getfit.domain.adjustR
import com.getfit.domain.adjustW
import com.getfit.domain.advanceFromRest
import com.getfit.domain.doneSet
import com.getfit.domain.startSession
import com.getfit.domain.tick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A workout run entirely on the wrist: the phone is out of range (or its app is closed), so the
 * watch drives the SAME engine the phone uses (the shared :engine module) from the routine cache
 * the phone last sent, and hands the finished session over as a [WearSessionUpload] the moment
 * the phone can be reached again (see [MainActivity]'s upload loop and the phone's
 * WearUploadService).
 *
 * A thin mirror of the phone's SessionController: wall-clock anchored ticker, persisted on every
 * action so process death mid-set loses nothing. Weights are in the cached display unit.
 */
class LocalSession(private val store: WatchStore, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<SessionState?>(store.localSession()?.let { if (it.phase == Phase.DONE) it else tick(it, System.currentTimeMillis()) })
    val state: StateFlow<SessionState?> = _state.asStateFlow()

    /** The display unit the session was started in (from the routine cache). */
    var units: String = store.routines().units
        private set

    private var ticker: Job? = null

    init { if (_state.value != null && _state.value?.phase != Phase.DONE) startTicker() }

    val active: Boolean get() = _state.value != null

    fun start(routine: WearRoutine, cache: WatchStore.RoutineCache) {
        if (_state.value != null || routine.items.isEmpty()) return
        units = cache.units
        val items = routine.items.map { SessionItem(it.id, it.name, it.muscle, it.sets, it.reps, it.bw, it.suggestW, it.equipment, it.superset) }
        val preBest = routine.items.associate { it.id to (it.bestW to it.bestReps) }
        _state.value = startSession(items, cache.restDefault, preBest, System.currentTimeMillis())
            .copy(name = routine.name, routineId = routine.id)
        startTicker()
        persist()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(1000)
                val s = _state.value ?: break
                if (s.phase == Phase.DONE) break
                _state.update { cur -> cur?.let { tick(it, System.currentTimeMillis()) } }
            }
        }
    }

    private fun persist() = store.saveLocalSession(_state.value)

    /** Returns true when the set just logged was a PR (the phone toasts; the watch vibrates). */
    fun doneSet(): Boolean {
        var pr = false
        _state.update { s -> s?.let { val r = doneSet(it, units, System.currentTimeMillis()); pr = r.pr; r.state } }
        persist()
        return pr
    }
    fun skip() { _state.update { s -> if (s?.phase == Phase.REST) advanceFromRest(s, false, System.currentTimeMillis()) else s }; persist() }
    fun addRest(delta: Int) { _state.update { it?.let { s -> addRest(s, delta, System.currentTimeMillis()) } }; persist() }
    fun adjustW(dir: Int) { _state.update { it?.let { s -> adjustW(s, dir, units) } }; persist() }
    fun adjustR(dir: Int) { _state.update { it?.let { s -> adjustR(s, dir) } }; persist() }
    fun recordHeartRate(samples: List<HrPoint>) { _state.update { it?.let { s -> addHeartRate(s, samples) } }; persist() }

    /**
     * Ends the session. Anything logged becomes a pending upload (kept until the phone acks);
     * nothing logged is simply discarded. Returns the upload, or null.
     */
    fun finish(): WearSessionUpload? {
        ticker?.cancel()
        val s = _state.value
        _state.value = null
        store.saveLocalSession(null)
        if (s == null || s.log.isEmpty()) return null
        val bwById = s.items.associate { it.id to it.bw }
        val upload = WearSessionUpload(
            id = "w${s.workStartedAtMs}",
            name = s.name,
            routineId = s.routineId,
            startedAtMs = s.workStartedAtMs,
            durationSec = s.elapsed,
            units = units,
            sets = s.log.map { WearLoggedSet(it.id, it.name, it.weight, it.reps, bwById[it.id] == true) },
            prs = s.newPRs.size,
            hr = s.hr.map { HeartRateSample(it.bpm.toDouble(), it.atMs) },
        )
        store.enqueueUpload(upload)
        return upload
    }
}
