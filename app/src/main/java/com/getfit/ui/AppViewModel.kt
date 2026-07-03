package com.getfit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getfit.data.db.Curated
import com.getfit.data.db.TargetEntity
import com.getfit.data.prefs.PlanItemData
import com.getfit.data.prefs.Settings
import com.getfit.di.AppContainer
import com.getfit.domain.Best
import com.getfit.domain.LoggedSet
import com.getfit.domain.bestFor
import com.getfit.domain.isBW
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(private val container: AppContainer) : ViewModel() {

    private val exerciseRepo = container.exerciseRepo
    private val workoutRepo = container.workoutRepo
    private val progressRepo = container.progressRepo
    private val settingsStore = container.settingsStore

    val data: StateFlow<AppData> = combine(
        exerciseRepo.exercises, exerciseRepo.logs, progressRepo.sessions,
        progressRepo.targets, workoutRepo.plan,
    ) { ex, logs, sessions, targets, plan ->
        val byId = ex.associateBy { it.id }
        val logsByEx = logs.groupBy { it.exerciseId }
        val bestMap = HashMap<String, Best>()
        logsByEx.forEach { (id, ls) ->
            val e = byId[id] ?: return@forEach
            bestFor(ls.map { LoggedSet(it.exerciseId, it.weight, it.reps, it.dateMs) }, isBW(e.equipment, e.reps))
                ?.let { bestMap[id] = it }
        }
        AppData(ex, logs, sessions, targets, plan, bestMap, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppData())

    val settings: StateFlow<Settings> =
        settingsStore.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    private val _nav = MutableStateFlow(NavState())
    val nav: StateFlow<NavState> = _nav.asStateFlow()

    private var toastJob: Job? = null
    private var clearJob: Job? = null

    init {
        viewModelScope.launch {
            delay(1050); _nav.update { it.copy(booted = true) }
            delay(350); _nav.update { it.copy(charted = true) }
        }
    }

    // ---- navigation ----
    fun selectTab(t: String) = _nav.update { it.copy(tab = t, detailId = null, settingsOpen = false) }
    fun goExercises() = selectTab(TAB_EXERCISES)
    fun goCategory(muscle: String) = _nav.update { it.copy(tab = TAB_EXERCISES, filter = muscle, query = "") }
    fun setQuery(q: String) = _nav.update { it.copy(query = q) }
    fun clearQuery() = _nav.update { it.copy(query = "") }
    fun setFilter(m: String) = _nav.update { it.copy(filter = m) }
    fun openDetail(id: String) = _nav.update { it.copy(detailId = id) }
    fun closeDetail() = _nav.update { it.copy(detailId = null) }
    fun openSettings() = _nav.update { it.copy(settingsOpen = true) }
    fun closeSettings() = _nav.update { it.copy(settingsOpen = false) }

    // ---- plan ----
    fun addToPlan(id: String) = viewModelScope.launch {
        val ex = data.value.exercise(id) ?: exerciseRepo.byId(id) ?: return@launch
        val added = workoutRepo.addToPlan(id, ex.reps)
        toast(if (added) "Added to Push Day" else "Already in workout", if (added) "add_circle" else "info")
    }
    fun removeFromPlan(id: String) = viewModelScope.launch { workoutRepo.removeFromPlan(id) }
    fun movePlan(index: Int, dir: Int) = viewModelScope.launch { workoutRepo.movePlan(index, dir) }
    fun setSets(id: String, delta: Int) = viewModelScope.launch { workoutRepo.setSets(id, delta) }
    fun setIntensity(v: String) = viewModelScope.launch { settingsStore.setIntensity(v) }

    // ---- settings ----
    fun toggleUnits() = viewModelScope.launch {
        settingsStore.setUnits(if (settings.value.units == "kg") "lb" else "kg")
    }
    fun setSound(v: Boolean) = viewModelScope.launch { settingsStore.setSound(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { settingsStore.setHaptics(v) }
    fun setAutorest(v: Boolean) = viewModelScope.launch { settingsStore.setAutorest(v) }
    fun setRest(v: Int) = viewModelScope.launch { settingsStore.setRestDefault(v) }

    fun clearAll() {
        if (!_nav.value.confirmClear) {
            _nav.update { it.copy(confirmClear = true) }
            clearJob?.cancel()
            clearJob = viewModelScope.launch { delay(3000); _nav.update { it.copy(confirmClear = false) } }
            return
        }
        clearJob?.cancel()
        viewModelScope.launch {
            container.clearAllData()
            _nav.update { it.copy(confirmClear = false, settingsOpen = false) }
            toast("All data cleared", "delete")
        }
    }

    // ---- onboarding ----
    fun finishOnboarding() = viewModelScope.launch { settingsStore.setOnboarded(true) }
    fun obNext() {
        if (_nav.value.obSlide < 2) _nav.update { it.copy(obSlide = it.obSlide + 1) } else finishOnboarding()
    }
    fun obSkip() = finishOnboarding()
    fun obSetSlide(i: Int) = _nav.update { it.copy(obSlide = i) }

    // ---- goals ----
    fun goalCur(id: String): Double {
        val ex = data.value.exercise(id) ?: return Curated.DEFAULT_WEIGHT[id] ?: 20.0
        val b = data.value.bestMap[id]
        val bw = isBW(ex.equipment, ex.reps)
        return b?.let { if (bw) it.reps.toDouble() else it.weight } ?: (Curated.DEFAULT_WEIGHT[id] ?: 20.0)
    }
    fun openGoal(id: String) {
        val bw = data.value.exercise(id)?.let { isBW(it.equipment, it.reps) } ?: false
        val cur = goalCur(id)
        _nav.update { it.copy(detailId = null, goalOpen = true, goalEx = id, goalWeeks = 8, goalTarget = cur + if (bw) 3.0 else 10.0) }
    }
    fun openNewGoal() = openGoal(Curated.GOAL_EX.first())
    fun selectGoalEx(id: String) {
        val bw = data.value.exercise(id)?.let { isBW(it.equipment, it.reps) } ?: false
        _nav.update { it.copy(goalEx = id, goalTarget = goalCur(id) + if (bw) 3.0 else 10.0) }
    }
    fun adjustGoal(dir: Int) {
        val bw = data.value.exercise(_nav.value.goalEx)?.let { isBW(it.equipment, it.reps) } ?: false
        val step = if (bw) 1.0 else if (settings.value.units == "lb") 5.0 else 2.5
        _nav.update { it.copy(goalTarget = (it.goalTarget + dir * step).coerceAtLeast(1.0).let { v -> Math.round(v * 100) / 100.0 }) }
    }
    fun setGoalWeeks(w: Int) = _nav.update { it.copy(goalWeeks = w) }
    fun closeGoal() = _nav.update { it.copy(goalOpen = false) }
    fun saveGoal() = viewModelScope.launch {
        val s = _nav.value
        val cur = goalCur(s.goalEx)
        if (s.goalTarget <= cur) return@launch
        val now = System.currentTimeMillis()
        progressRepo.addTarget(TargetEntity("t$now", s.goalEx, s.goalTarget, cur, now, s.goalWeeks))
        _nav.update { it.copy(goalOpen = false) }
        toast("Target set", "flag")
    }
    fun deleteTarget(id: String) = viewModelScope.launch { progressRepo.deleteTarget(id) }

    // ---- session ----
    val sessionController = SessionController(container, viewModelScope) { text, icon -> toast(text, icon) }
    val session get() = sessionController.state

    fun startSession(items: List<PlanItemData>) { sessionController.start(items) }
    fun startSingle(id: String, reps: String) { sessionController.start(listOf(PlanItemData(id, 3, reps))) }
    fun endSession() = viewModelScope.launch { sessionController.endAndSave(); selectTab(TAB_PROGRESS) }

    // ---- toast ----
    fun toast(text: String, icon: String = "check_circle") {
        toastJob?.cancel()
        _nav.update { it.copy(toast = ToastMsg(text, icon)) }
        toastJob = viewModelScope.launch { delay(1900); _nav.update { it.copy(toast = null) } }
    }
}
