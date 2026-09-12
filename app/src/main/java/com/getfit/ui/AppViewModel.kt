package com.getfit.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getfit.data.ai.AI_REVIEW_MAX_TOKENS
import com.getfit.data.ai.AiGateway
import com.getfit.data.ai.AiResult
import com.getfit.data.ai.COACH_MAX_TOKENS
import com.getfit.data.ai.CoachMode
import com.getfit.data.ai.SuggestedTarget
import com.getfit.data.ai.buildCoachBrief
import com.getfit.data.ai.coachSystemPrompt
import com.getfit.data.ai.parseCoachAnswer
import com.getfit.data.ai.BODY_CHECK_MAX_TOKENS
import com.getfit.data.ai.PROVIDER_ANTHROPIC
import com.getfit.data.ai.PROVIDER_COMPAT
import com.getfit.data.ai.aiReviewSystemPrompt
import com.getfit.data.ai.textBlock
import com.getfit.data.security.KeySlot
import com.getfit.data.sync.SyncOutcome
import com.getfit.data.ai.BodyGoal
import com.getfit.data.ai.ImageEncoder
import com.getfit.data.ai.PhotoView
import com.getfit.data.ai.bodyAnalysisSystemPrompt
import com.getfit.data.ai.buildBodyAnalysisContent
import com.getfit.data.ai.buildWorkoutSummary
import com.getfit.data.db.Curated
import com.getfit.data.db.TargetEntity
import com.getfit.data.backup.RestoreOutcome
import com.getfit.data.importexport.ImportOutcome
import com.getfit.data.prefs.PlanItemData
import com.getfit.data.prefs.Settings
import com.getfit.data.wear.ActionKind
import com.getfit.data.wear.toWearSnapshot
import com.getfit.di.AppContainer
import com.getfit.domain.Best
import com.getfit.domain.SessionSetRow
import com.getfit.domain.exerciseDeltas
import com.getfit.domain.fmtDur
import com.getfit.domain.fmtVol
import com.getfit.domain.recoveryByMuscle
import com.getfit.domain.suggestRoutine
import com.getfit.domain.HrPoint
import com.getfit.domain.LoggedSet
import com.getfit.domain.Units
import com.getfit.domain.bestFor
import com.getfit.domain.isBW
import com.google.android.gms.wearable.MessageClient
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext

/** Four views plus one close-up is plenty; more only adds tokens without adding information. */
private const val MAX_BODY_PHOTOS = 5

class AppViewModel(private val container: AppContainer) : ViewModel() {

    private val exerciseRepo = container.exerciseRepo
    private val workoutRepo = container.workoutRepo
    private val progressRepo = container.progressRepo
    private val settingsStore = container.settingsStore
    private val importExportRepo = container.importExportRepo
    private val backupRepo = container.backupRepo
    private val syncRepo = container.syncRepo
    private val aiGateway = AiGateway(container.secureKeyStore)

    val data: StateFlow<AppData> = combine(
        combine(
            exerciseRepo.exercises, exerciseRepo.logs, progressRepo.sessions,
            progressRepo.targets, workoutRepo.routines,
        ) { ex, logs, sessions, targets, routines ->
            val byId = ex.associateBy { it.id }
            val logsByEx = logs.groupBy { it.exerciseId }
            val bestMap = HashMap<String, Best>()
            logsByEx.forEach { (id, ls) ->
                val e = byId[id] ?: return@forEach
                bestFor(ls.map { LoggedSet(it.exerciseId, it.weight, it.reps, it.dateMs) }, isBW(e.equipment, e.reps))
                    ?.let { bestMap[id] = it }
            }
            AppData(ex, logs, sessions, targets, routines, emptyList(), emptyList(), bestMap, loaded = true)
        },
        progressRepo.measurements, progressRepo.sessionSets,
    ) { d, measurements, sessionSets -> d.copy(measurements = measurements, sessionSets = sessionSets) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppData())

    val settings: StateFlow<Settings> =
        settingsStore.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    val hasAiKey: StateFlow<Boolean> =
        container.secureKeyStore.hasKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hasCompatKey: StateFlow<Boolean> =
        container.secureKeyStore.hasCompatKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Whether the CONFIGURED provider is ready — what the AI screens gate on, not just the Anthropic key. */
    val aiReady: StateFlow<Boolean> = combine(settings, hasAiKey, hasCompatKey) { s, anth, _ ->
        when (s.aiProvider) {
            PROVIDER_COMPAT -> s.compatBaseUrl.isNotBlank() && s.compatModel.isNotBlank()
            else -> anth
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _nav = MutableStateFlow(NavState())
    val nav: StateFlow<NavState> = _nav.asStateFlow()

    private val _aiReview = MutableStateFlow(AiReviewUiState())
    val aiReview: StateFlow<AiReviewUiState> = _aiReview.asStateFlow()

    private val _bodyCheck = MutableStateFlow(BodyCheckUiState())
    val bodyCheck: StateFlow<BodyCheckUiState> = _bodyCheck.asStateFlow()

    private val _backup = MutableStateFlow(BackupUiState())
    val backup: StateFlow<BackupUiState> = _backup.asStateFlow()

    private val _importExport = MutableStateFlow(ImportExportUiState())
    val importExport: StateFlow<ImportExportUiState> = _importExport.asStateFlow()

    // Transient bits of the sync UI (busy, confirm gate, last message) live here; the persisted
    // part comes from SyncRepo.state and the token flag from SecureKeyStore.
    private val _syncTransient = MutableStateFlow(SyncUiState())
    val syncUi: StateFlow<SyncUiState> = combine(syncRepo.state, syncRepo.hasToken, _syncTransient) { s, tok, t ->
        t.copy(state = s, hasToken = tok)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncUiState())

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

    // ---- routines ----
    /** Plan edits from Detail ("Add to workout") target the routine being edited, else the current one. */
    private fun targetRoutineId(): String? = _nav.value.editRoutineId

    fun addToPlan(id: String) = viewModelScope.launch {
        val ex = data.value.exercise(id) ?: exerciseRepo.byId(id) ?: return@launch
        val rid = targetRoutineId()
        val added = workoutRepo.addToPlan(id, ex.reps, routineId = rid)
        val name = (data.value.routine(rid) ?: data.value.currentRoutine)?.name ?: "workout"
        toast(if (added) "Added to $name" else "Already in $name", if (added) "add_circle" else "info")
    }
    fun removeFromPlan(id: String) = viewModelScope.launch { workoutRepo.removeFromPlan(id, targetRoutineId()) }
    fun movePlan(index: Int, dir: Int) = viewModelScope.launch { workoutRepo.movePlan(index, dir, targetRoutineId()) }
    fun setSets(id: String, delta: Int) = viewModelScope.launch { workoutRepo.setSets(id, delta, targetRoutineId()) }
    fun setReps(id: String, reps: String) = viewModelScope.launch { workoutRepo.setReps(id, reps, targetRoutineId()) }
    fun toggleSuperset(id: String) = viewModelScope.launch {
        val r = data.value.routine(targetRoutineId()) ?: data.value.currentRoutine ?: return@launch
        val item = r.items.firstOrNull { it.id == id } ?: return@launch
        workoutRepo.setSuperset(id, !item.superset, targetRoutineId())
    }
    fun setIntensity(v: String) = viewModelScope.launch { settingsStore.setIntensity(v) }

    fun openRoutineEditor(id: String) = _nav.update { it.copy(tab = TAB_BUILD, editRoutineId = id, routineMenuId = null) }
    fun closeRoutineEditor() = _nav.update { it.copy(editRoutineId = null) }
    fun openRoutineMenu(id: String) = _nav.update { it.copy(routineMenuId = id) }
    fun closeRoutineMenu() = _nav.update { it.copy(routineMenuId = null) }

    fun createRoutine(name: String) = viewModelScope.launch {
        val r = workoutRepo.createRoutine(name)
        openRoutineEditor(r.id)
    }
    fun renameRoutine(id: String, name: String) = viewModelScope.launch { workoutRepo.renameRoutine(id, name) }
    fun duplicateRoutine(id: String) = viewModelScope.launch {
        workoutRepo.duplicateRoutine(id); closeRoutineMenu(); toast("Routine duplicated", "content_copy")
    }
    fun deleteRoutine(id: String) = viewModelScope.launch {
        val ok = workoutRepo.deleteRoutine(id)
        closeRoutineMenu()
        if (_nav.value.editRoutineId == id) closeRoutineEditor()
        toast(if (ok) "Routine deleted" else "Keep at least one routine", if (ok) "delete" else "info")
    }
    fun moveRoutine(index: Int, dir: Int) = viewModelScope.launch { workoutRepo.moveRoutine(index, dir) }
    /** "Do this one next" — Home's card and the watch's Idle screen follow it. */
    fun setCurrentRoutine(id: String) = viewModelScope.launch {
        workoutRepo.setCurrentRoutine(id); closeRoutineMenu()
        data.value.routine(id)?.let { toast("${it.name} is up next", "event_upcoming") }
    }
    fun openPrograms() = _nav.update { it.copy(programsOpen = true) }
    fun closePrograms() = _nav.update { it.copy(programsOpen = false) }
    /** Adds a template's routines (after the existing ones) and makes its first day the one up next. */
    fun addProgram(id: String) = viewModelScope.launch {
        val p = Curated.PROGRAMS.firstOrNull { it.id == id } ?: return@launch
        var first: String? = null
        p.days.forEach { d ->
            val r = workoutRepo.createRoutine(d.name, d.items.map { PlanItemData(it.id, it.sets, it.reps) })
            if (first == null) first = r.id
        }
        first?.let { workoutRepo.setCurrentRoutine(it) }
        closePrograms()
        toast("Added ${p.days.size} routines from ${p.name}", "playlist_add_check")
    }

    fun saveSessionAsRoutine(sessionId: String) = viewModelScope.launch {
        val r = workoutRepo.routineFromSession(sessionId)
        toast(if (r != null) "Saved as routine \"${r.name}\"" else "Nothing to save", if (r != null) "playlist_add_check" else "info")
    }

    // ---- settings ----
    fun toggleUnits() = viewModelScope.launch {
        settingsStore.setUnits(if (settings.value.units == "kg") "lb" else "kg")
    }
    fun setSound(v: Boolean) = viewModelScope.launch { settingsStore.setSound(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { settingsStore.setHaptics(v) }
    fun setAutorest(v: Boolean) = viewModelScope.launch { settingsStore.setAutorest(v) }
    fun setRest(v: Int) = viewModelScope.launch { settingsStore.setRestDefault(v) }
    fun setKeepAwake(v: Boolean) = viewModelScope.launch { settingsStore.setKeepAwake(v) }
    fun setPrNotify(v: Boolean) = viewModelScope.launch { settingsStore.setPrNotify(v) }
    fun setWeekStartsMonday(v: Boolean) = viewModelScope.launch { settingsStore.setWeekStartsMonday(v) }
    fun setWeeklyGoal(v: Int) = viewModelScope.launch { settingsStore.setWeeklyGoal(v) }

    // ---- profile (Hevy: Edit Profile + Measurements) ----
    fun openProfile() = _nav.update { it.copy(profileOpen = true) }
    fun closeProfile() = _nav.update { it.copy(profileOpen = false) }
    fun setProfileName(v: String) = viewModelScope.launch { settingsStore.setName(v) }
    fun setProfileSex(v: String) = viewModelScope.launch { settingsStore.setSex(v) }
    fun setProfileBirthYear(v: Int) = viewModelScope.launch { settingsStore.setBirthYear(v) }
    fun setProfileHeightCm(v: Int) = viewModelScope.launch { settingsStore.setHeightCm(v) }
    /** Body weight arrives in the display unit; stored in kg like every other weight. */
    fun setProfileBodyWeight(display: Double) = viewModelScope.launch {
        settingsStore.setBodyWeightKg(Units.fromDisplay(display, settings.value.units))
    }

    fun openMeasurements() = _nav.update { it.copy(measurementsOpen = true) }
    fun closeMeasurements() = _nav.update { it.copy(measurementsOpen = false) }
    /** Logging a weight also updates the profile weight, so the calorie estimate follows the scale. */
    fun addMeasurement(weightDisplay: Double, bodyFatPct: Double?, note: String) = viewModelScope.launch {
        val kg = Units.fromDisplay(weightDisplay, settings.value.units)
        if (kg <= 0.0) return@launch
        val now = System.currentTimeMillis()
        progressRepo.addMeasurement(com.getfit.data.db.MeasurementEntity("m$now", now, Math.round(kg * 10) / 10.0, bodyFatPct, note.trim()))
        settingsStore.setBodyWeightKg(kg)
        toast("Measurement logged", "check_circle")
    }
    fun deleteMeasurement(id: String) = viewModelScope.launch { progressRepo.deleteMeasurement(id) }

    /** Re-run the first-launch tour from Settings ("Getting started guide"). */
    fun showOnboardingAgain() = viewModelScope.launch {
        _nav.update { it.copy(settingsOpen = false, obSlide = 0) }
        settingsStore.setOnboarded(false)
    }

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
    // Weights are stored in kg; targets are stored/compared in the exercise's natural unit
    // (kg for weighted, reps for bodyweight). The goal sheet works in DISPLAY units and converts.
    private fun isBwEx(id: String): Boolean =
        data.value.exercise(id)?.let { isBW(it.equipment, it.reps) } ?: false

    /** Current best in natural units (kg or reps). */
    fun goalCur(id: String): Double {
        val ex = data.value.exercise(id) ?: return Curated.DEFAULT_WEIGHT[id] ?: 20.0
        val b = data.value.bestMap[id]
        return b?.let { if (isBW(ex.equipment, ex.reps)) it.reps.toDouble() else it.weight }
            ?: (Curated.DEFAULT_WEIGHT[id] ?: 20.0)
    }

    /** Current best in the display unit shown in the sheet. */
    fun goalCurDisplay(id: String): Double =
        if (isBwEx(id)) goalCur(id) else Units.toDisplay(goalCur(id), settings.value.units)

    fun openGoal(id: String) {
        val bw = isBwEx(id)
        _nav.update { it.copy(detailId = null, goalOpen = true, goalEx = id, goalWeeks = 8, goalTarget = goalCurDisplay(id) + if (bw) 3.0 else 10.0) }
    }
    fun openNewGoal() = openGoal(Curated.GOAL_EX.first())
    fun selectGoalEx(id: String) {
        val bw = isBwEx(id)
        _nav.update { it.copy(goalEx = id, goalTarget = goalCurDisplay(id) + if (bw) 3.0 else 10.0) }
    }
    fun adjustGoal(dir: Int) {
        val bw = isBwEx(_nav.value.goalEx)
        val step = if (bw) 1.0 else Units.step(settings.value.units)
        _nav.update { it.copy(goalTarget = (it.goalTarget + dir * step).coerceAtLeast(1.0).let { v -> Math.round(v * 100) / 100.0 }) }
    }
    fun setGoalWeeks(w: Int) = _nav.update { it.copy(goalWeeks = w) }
    fun closeGoal() = _nav.update { it.copy(goalOpen = false) }
    fun saveGoal() = viewModelScope.launch {
        val s = _nav.value
        val bw = isBwEx(s.goalEx)
        val curNatural = goalCur(s.goalEx)
        val targetNatural = if (bw) s.goalTarget else Units.fromDisplay(s.goalTarget, settings.value.units)
        if (targetNatural <= curNatural) return@launch
        val now = System.currentTimeMillis()
        progressRepo.addTarget(TargetEntity("t$now", s.goalEx, targetNatural, curNatural, now, s.goalWeeks))
        _nav.update { it.copy(goalOpen = false) }
        toast("Target set", "flag")
    }
    fun deleteTarget(id: String) = viewModelScope.launch { progressRepo.deleteTarget(id) }

    // ---- session ----
    val sessionController = SessionController(container, viewModelScope) { text, icon -> toast(text, icon) }
    val session get() = sessionController.state

    fun startRoutine(id: String) {
        val r = data.value.routine(id) ?: return
        if (r.items.isEmpty()) { toast("Add an exercise first", "info"); return }
        closeRoutineEditor()
        sessionController.start(r)
    }
    fun startCurrentRoutine() { data.value.currentRoutine?.let { startRoutine(it.id) } }
    fun startSingle(id: String, reps: String) {
        val name = data.value.exercise(id)?.name ?: "Workout"
        sessionController.start(listOf(PlanItemData(id, 3, reps)), name = name)
    }
    fun endSession() = viewModelScope.launch { sessionController.endAndSave(); selectTab(TAB_PROGRESS) }

    // Mid-workout edits: the sheet on the session screen, and the exercise list doubling as a picker.
    fun openSessionMenu() = _nav.update { it.copy(sessionMenuOpen = true) }
    fun closeSessionMenu() = _nav.update { it.copy(sessionMenuOpen = false) }
    fun startSessionPick(mode: String) = _nav.update { it.copy(sessionMenuOpen = false, sessionPick = mode, query = "", filter = "All") }
    fun cancelSessionPick() = _nav.update { it.copy(sessionPick = null) }
    fun pickExercise(id: String) {
        when (_nav.value.sessionPick) {
            "add" -> { sessionController.addExercise(id); toast("Added to this workout", "add_circle") }
            "replace" -> { sessionController.replaceCurrent(id); toast("Exercise replaced", "check_circle") }
        }
        _nav.update { it.copy(sessionPick = null) }
    }
    fun sessionSkipExercise() { closeSessionMenu(); sessionController.skipExercise() }
    fun sessionRemoveCurrent() { closeSessionMenu(); sessionController.removeCurrent() }

    // ---- history ----
    fun openSessionDetail(id: String) = _nav.update { it.copy(sessionDetailId = id) }
    fun openRecap() = _nav.update { it.copy(recapOpen = true) }
    fun closeRecap() = _nav.update { it.copy(recapOpen = false) }
    fun closeSessionDetail() = _nav.update { it.copy(sessionDetailId = null) }
    fun deleteSession(id: String) = viewModelScope.launch {
        workoutRepo.deleteSession(id)
        _nav.update { if (it.sessionDetailId == id) it.copy(sessionDetailId = null) else it }
        toast("Workout deleted", "delete")
    }
    suspend fun sessionSets(id: String) = workoutRepo.sessionSets(id)
    suspend fun sessionHeartRate(id: String) = workoutRepo.sessionHeartRate(id)

    // ---- wear OS companion (mirror session state to a paired watch, apply actions/heart rate it
    // sends back) — see docs/wear-companion-design.md. Placed here, after sessionController exists,
    // rather than in the class's first init{} block above: that block runs before sessionController's
    // own initializer, so referencing it there would read it before construction.
    private val _liveHeartRateBpm = MutableStateFlow<Double?>(null)
    val liveHeartRateBpm: StateFlow<Double?> = _liveHeartRateBpm.asStateFlow()

    private val wearListener: MessageClient.OnMessageReceivedListener

    init {
        // The snapshot also carries the routine list, so the watch's Idle screen can offer "start
        // Pull Day" and "make Legs next" without the phone app being open. Routines change rarely
        // and the send is deduped on identical bytes, so folding them in costs nothing per tick.
        viewModelScope.launch {
            combine(sessionController.state, workoutRepo.routines, settings) { s, r, st -> Triple(s, r, st) }
                .collect { (s, r, st) -> container.phoneWearSync.sendSnapshot(toWearSnapshot(s, st.units, r)) }
        }
        wearListener = container.phoneWearSync.listen(
            onAction = { action ->
                when (action.kind) {
                    ActionKind.DONE_SET -> sessionController.doneSet()
                    ActionKind.SKIP_REST -> sessionController.skip()
                    ActionKind.ADJUST_REST -> sessionController.addRest(action.restDeltaSec)
                    // The watch app just came to the foreground and wants the current state — including
                    // "no session" (active = false), so a stale Active screen clears too.
                    ActionKind.REQUEST_STATE -> container.phoneWearSync.sendSnapshot(
                        toWearSnapshot(sessionController.state.value, settings.value.units, data.value.routinesData),
                        force = true,
                    )
                    // Routine picked on the wrist: same code paths as the phone's own buttons.
                    ActionKind.SELECT_ROUTINE -> viewModelScope.launch { workoutRepo.setCurrentRoutine(action.routineId) }
                    ActionKind.START_ROUTINE -> {
                        if (sessionController.state.value == null) {
                            val id = action.routineId.ifBlank { data.value.currentRoutine?.id.orEmpty() }
                            startRoutine(id)
                        }
                    }
                }
            },
            onHeartRate = { batch ->
                batch.samples.lastOrNull()?.let { _liveHeartRateBpm.value = it.bpm }
                sessionController.recordHeartRate(batch.samples.map { HrPoint(it.atMs, Math.round(it.bpm).toInt()) })
            },
        )
    }

    override fun onCleared() {
        container.phoneWearSync.stopListening(wearListener)
        super.onCleared()
    }

    // ---- AI review ----
    fun openAiReview() = _nav.update { it.copy(aiReviewOpen = true) }
    fun closeAiReview() = _nav.update { it.copy(aiReviewOpen = false) }

    fun setAiApiKey(key: String) = viewModelScope.launch { container.secureKeyStore.setApiKey(key) }
    fun clearAiApiKey() = viewModelScope.launch { container.secureKeyStore.clear() }
    fun setAiModel(v: String) = viewModelScope.launch { settingsStore.setAiModel(v) }
    fun setAiProvider(v: String) = viewModelScope.launch { settingsStore.setAiProvider(v) }
    fun setCompatBaseUrl(v: String) = viewModelScope.launch { settingsStore.setCompatBaseUrl(v) }
    fun setCompatModel(v: String) = viewModelScope.launch { settingsStore.setCompatModel(v) }
    fun setCompatApiKey(key: String) = viewModelScope.launch { container.secureKeyStore.setApiKey(key, KeySlot.COMPAT) }
    fun clearCompatApiKey() = viewModelScope.launch { container.secureKeyStore.clear(KeySlot.COMPAT) }

    /** The full brief both AI features share: sessions, bests, targets, trends and training patterns. */
    private suspend fun currentWorkoutSummary(): String {
        val d = data.value
        val sessionSets = withContext(Dispatchers.IO) { container.db.sessionDao().allSetsOnce() }
        return buildWorkoutSummary(d.sessions, d.targets, d.bestMap, d.exercises, settings.value.units, d.logs, sessionSets)
    }

    fun runAiReview() {
        if (_aiReview.value.loading) return
        _aiReview.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            aiGateway.missingConfig(settings.value)?.let { msg ->
                _aiReview.update { it.copy(loading = false, error = msg) }
                return@launch
            }
            val summary = currentWorkoutSummary()
            when (val result = aiGateway.send(settings.value, aiReviewSystemPrompt(), listOf(textBlock(summary)), AI_REVIEW_MAX_TOKENS)) {
                is AiResult.Success -> _aiReview.update { it.copy(loading = false, text = result.text, error = null) }
                is AiResult.Failure -> _aiReview.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    // ---- AI coach (critique the last workout / what to do next + PR attempts / goal review) ----
    private val _coach = MutableStateFlow(CoachUiState())
    val coach: StateFlow<CoachUiState> = _coach.asStateFlow()

    /** Opening with a different mode or session clears the previous answer; same ask keeps it. */
    fun openCoach(mode: CoachMode, sessionId: String? = null) {
        _coach.update { c ->
            if (c.mode == mode && c.sessionId == sessionId) c else CoachUiState(mode = mode, sessionId = sessionId)
        }
        _nav.update { it.copy(coachOpen = true, sessionDetailId = null) }
    }
    fun closeCoach() = _nav.update { it.copy(coachOpen = false) }
    fun setCoachMode(mode: CoachMode) = openCoach(mode, null)

    /** "Session under review": the workout, its sets, and each lift's top set vs the previous time. */
    private fun sessionSection(sessionId: String?): String? {
        val d = data.value
        val s = (sessionId?.let { id -> d.sessions.firstOrNull { it.id == id } } ?: d.sessions.maxByOrNull { it.dateMs }) ?: return null
        val units = settings.value.units
        val sets = d.sessionSets.filter { it.sessionId == s.id }
        val rows = sets.map { SessionSetRow(it.exerciseId, it.name, it.weight, it.reps) }
        val bwOf: (String) -> Boolean = { exId -> d.exercise(exId)?.let { isBW(it.equipment, it.reps) } ?: rows.filter { it.exerciseId == exId }.all { it.weightKg == 0.0 } }
        val deltas = exerciseDeltas(s.dateMs, rows, d.logs.map { LoggedSet(it.exerciseId, it.weight, it.reps, it.dateMs) }, bwOf)
        val routine = d.routine(s.routineId)
        val df = java.text.SimpleDateFormat("EEE d MMM, h:mm a", java.util.Locale.US)
        val sb = StringBuilder()
        sb.appendLine("Session under review: \"${s.name}\" on ${df.format(java.util.Date(s.dateMs))}.")
        sb.appendLine(
            "Duration ${if (s.durationSec > 0) fmtDur(s.durationSec) else "unknown"}, ${s.totalSets} sets, " +
                "${fmtVol(Units.volDisplay(s.volume, units))} $units volume, ${s.prs} PRs" +
                (if (s.avgBpm > 0) ", avg heart rate ${s.avgBpm} bpm (max ${s.maxBpm})" else "") +
                (if (s.calories > 0) ", ~${s.calories} kcal" else "") + ".",
        )
        if (routine != null) {
            sb.appendLine("Routine plan was: " + routine.items.joinToString("; ") { p -> "${d.exercise(p.id)?.name ?: p.id} ${p.sets}x${p.reps}" } + ".")
        }
        sb.appendLine("Sets as logged, with the top set vs the previous session of that lift:")
        sets.groupBy { it.exerciseId }.forEach { (exId, rows2) ->
            val bw = bwOf(exId)
            val list = rows2.joinToString(", ") { r -> if (bw) "${r.reps} reps" else "${Units.fmtDisplay(Units.toDisplay(r.weight, units), units)}x${r.reps}" }
            val dlt = deltas[exId]
            val vs = when {
                dlt?.delta == null -> "first time logged"
                bw -> "vs last: ${if (dlt.delta!! >= 0) "+" else ""}${dlt.delta!!.toInt()} reps"
                else -> "vs last: ${if (dlt.delta!! >= 0) "+" else ""}${Units.fmtDisplay(Units.toDisplay(dlt.delta!!, units), units)} $units on the top set"
            }
            sb.appendLine("- ${rows2.first().name}: $list ($vs)")
        }
        return sb.toString()
    }

    /** "Recovery": per-muscle readiness, the rotation's next routine, and every routine's contents. */
    private fun recoverySection(): String {
        val d = data.value
        val now = System.currentTimeMillis()
        val recovery = recoveryByMuscle(
            d.sessions.associate { it.id to it.dateMs }, d.sessionSets.map { it.sessionId to it.exerciseId },
            { id -> d.exercise(id)?.muscle }, now,
        )
        val pick = suggestRoutine(d.routines.map { r -> r.id to r.items.map { it.id } }, recovery, { id -> d.exercise(id)?.muscle }, now)
        val sb = StringBuilder()
        sb.appendLine("Recovery (pre-computed readiness per muscle group, 0 = just trained, 100 = fully recovered):")
        recovery.sortedByDescending { it.readiness }.forEach { r ->
            val since = r.daysSince(now)?.let { "last trained ${if (it == 0) "today" else "$it day${if (it == 1) "" else "s"} ago"}" } ?: "never trained"
            sb.appendLine("- ${r.muscle}: ${(r.readiness * 100).toInt()}% ($since, ${r.setsLast7d} sets in the last 7 days)")
        }
        sb.appendLine()
        sb.appendLine("Routines (id: name — exercises):")
        d.routines.forEach { r ->
            sb.appendLine("- ${r.id}: ${r.name} — " + r.items.joinToString(", ") { p -> "${d.exercise(p.id)?.name ?: p.id} ${p.sets}x${p.reps}" })
        }
        d.currentRoutine?.let { sb.appendLine("The rotation says next is: ${it.name}.") }
        pick?.let { p -> sb.appendLine("Recovery heuristic prefers: ${d.routine(p.routineId)?.name ?: p.routineId} (${p.reason}).") }
        return sb.toString()
    }

    private fun targetableSection(): String {
        val d = data.value
        // Anything with a logged best, plus the curated goal set, so the coach can only name real ids.
        val ids = (d.bestMap.keys + Curated.GOAL_EX).distinct()
        return ids.mapNotNull { id -> d.exercise(id)?.let { "- $id: ${it.name}${if (isBW(it.equipment, it.reps)) " (bodyweight, target in reps)" else ""}" } }.joinToString("\n")
    }

    fun runCoach() {
        val c = _coach.value
        if (c.loading) return
        _coach.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            aiGateway.missingConfig(settings.value)?.let { msg ->
                _coach.update { it.copy(loading = false, error = msg) }
                return@launch
            }
            val units = settings.value.units
            val summary = currentWorkoutSummary()
            val brief = buildCoachBrief(
                c.mode, summary,
                sessionSection = if (c.mode == CoachMode.LAST_WORKOUT) sessionSection(c.sessionId) ?: "No workouts logged yet." else null,
                recoverySection = if (c.mode == CoachMode.NEXT) recoverySection() else null,
                targetable = targetableSection(),
            )
            when (val result = aiGateway.send(settings.value, coachSystemPrompt(c.mode, units), listOf(textBlock(brief)), COACH_MAX_TOKENS)) {
                is AiResult.Success -> {
                    val allowed = data.value.exercises.map { it.id }.toSet()
                    val (prose, targets) = parseCoachAnswer(result.text, allowed)
                    _coach.update { it.copy(loading = false, text = prose, suggestions = targets, error = null) }
                }
                is AiResult.Failure -> _coach.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    /** Turns a coach suggestion into a real target (the same record the goal sheet writes). */
    fun acceptSuggestedTarget(s: SuggestedTarget) = viewModelScope.launch {
        val bw = isBwEx(s.exerciseId)
        val cur = goalCur(s.exerciseId)
        val targetNatural = if (bw) s.value else Units.fromDisplay(s.value, settings.value.units)
        if (targetNatural <= cur) { toast("That's not above your current best", "info"); return@launch }
        val now = System.currentTimeMillis()
        progressRepo.addTarget(TargetEntity("t$now", s.exerciseId, targetNatural, cur, now, s.weeks))
        _coach.update { it.copy(accepted = it.accepted + s.exerciseId) }
        toast("Target set", "flag")
    }

    // ---- body check (photos -> physique assessment + training changes) ----
    fun openBodyCheck() = _nav.update { it.copy(bodyCheckOpen = true) }

    /** Closing drops the photos and the consent; the last result text is kept for re-reading. */
    fun closeBodyCheck() {
        _nav.update { it.copy(bodyCheckOpen = false) }
        _bodyCheck.update { it.copy(photos = emptyList(), consented = false, error = null) }
    }

    /** Assigns views in the natural order Front, Back, Left, Right, then "Specific area". */
    fun bodyCheckAddPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _bodyCheck.update { s ->
            val existing = s.photos
            val room = (MAX_BODY_PHOTOS - existing.size).coerceAtLeast(0)
            val views = PhotoView.values()
            val added = uris.take(room).mapIndexed { i, uri ->
                PickedPhoto(uri, views.getOrElse(existing.size + i) { PhotoView.DETAIL })
            }
            s.copy(photos = existing + added, error = null)
        }
    }

    fun bodyCheckRemovePhoto(index: Int) = _bodyCheck.update { s ->
        s.copy(photos = s.photos.filterIndexed { i, _ -> i != index })
    }

    fun bodyCheckCycleView(index: Int) = _bodyCheck.update { s ->
        val views = PhotoView.values()
        s.copy(photos = s.photos.mapIndexed { i, p ->
            if (i == index) p.copy(view = views[(p.view.ordinal + 1) % views.size]) else p
        })
    }

    fun bodyCheckSetGoal(g: BodyGoal) = _bodyCheck.update { it.copy(goal = g) }
    fun bodyCheckSetFocus(v: String) = _bodyCheck.update { it.copy(focusArea = v) }
    fun bodyCheckSetConsent(v: Boolean) = _bodyCheck.update { it.copy(consented = v) }

    fun runBodyCheck() {
        val s = _bodyCheck.value
        if (s.loading) return
        if (s.photos.isEmpty()) { _bodyCheck.update { it.copy(error = "Add at least one photo.") }; return }
        if (!s.consented) { _bodyCheck.update { it.copy(error = "Tick the box to confirm you're okay sending these photos.") }; return }
        _bodyCheck.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            aiGateway.missingConfig(settings.value)?.let { msg ->
                _bodyCheck.update { it.copy(loading = false, error = msg) }
                return@launch
            }
            // Encode off the main thread; a 12 MP photo decode is not free. Held in memory only for
            // the duration of this request.
            val results = withContext(Dispatchers.IO) {
                s.photos.map { p -> p.view to ImageEncoder.encode(container.appContext, p.uri) }
            }
            val encoded = results.mapNotNull { (view, r) -> r.getOrNull()?.let { view to it } }
            if (encoded.isEmpty()) {
                val why = results.firstNotNullOfOrNull { it.second.exceptionOrNull()?.message }
                _bodyCheck.update { it.copy(loading = false, error = "Couldn't read those photos" + (why?.let { ": $it" } ?: ".")) }
                return@launch
            }
            val content = buildBodyAnalysisContent(encoded, s.goal, s.focusArea, currentWorkoutSummary())
            val result = aiGateway.send(settings.value, bodyAnalysisSystemPrompt(), content, BODY_CHECK_MAX_TOKENS)
            when (result) {
                is AiResult.Success -> _bodyCheck.update { it.copy(loading = false, text = result.text, error = null) }
                is AiResult.Failure -> _bodyCheck.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    // ---- import / export ----
    fun importCsv(resolver: ContentResolver, uri: Uri) {
        if (_importExport.value.busy) return
        _importExport.update { it.copy(busy = true, lastError = null, lastResult = null) }
        viewModelScope.launch {
            val text = try {
                withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
            } catch (e: Exception) { null }

            if (text.isNullOrBlank()) {
                _importExport.update { it.copy(busy = false, lastError = "Couldn't read that file.") }
                return@launch
            }
            when (val outcome = importExportRepo.importCsv(text, data.value.exercises)) {
                is ImportOutcome.Success -> {
                    syncRepo.noteChange()
                    val s = outcome.summary
                    val msg = buildString {
                        append("Imported ${s.setsImported} sets across ${s.sessionsImported} sessions from ${s.format.label}")
                        if (s.exercisesCreated > 0) append(" — added ${s.exercisesCreated} new exercises")
                        if (s.routinesCreated > 0) append(" — created ${s.routinesCreated} routines from the workout names")
                        if (s.skippedRows > 0) append(" (skipped ${s.skippedRows} unrecognized rows)")
                    }
                    _importExport.update { it.copy(busy = false, lastResult = msg, lastError = null) }
                    toast("Import complete", "check_circle")
                }
                is ImportOutcome.Failure -> _importExport.update { it.copy(busy = false, lastError = outcome.message) }
            }
        }
    }

    fun exportCsv(resolver: ContentResolver, uri: Uri) {
        if (_importExport.value.busy) return
        _importExport.update { it.copy(busy = true, lastError = null, lastResult = null) }
        viewModelScope.launch {
            try {
                val csv = importExportRepo.exportCsv()
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                }
                _importExport.update { it.copy(busy = false, lastResult = "Export saved.") }
                toast("Exported", "check_circle")
            } catch (e: Exception) {
                _importExport.update { it.copy(busy = false, lastError = "Couldn't write that file.") }
            }
        }
    }

    // ---- on-device backup ----

    /** Refreshes the stored-backup metadata shown in Settings. Cheap; safe to call on open. */
    fun refreshBackupInfo() {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) { backupRepo.localBackupInfo() }
            _backup.update { it.copy(stored = info) }
        }
    }

    fun backupToDevice() {
        if (_backup.value.busy) return
        _backup.update { it.copy(busy = true, lastError = null, lastResult = null) }
        viewModelScope.launch {
            backupRepo.backupToDevice().fold(
                onSuccess = { info ->
                    _backup.update {
                        it.copy(busy = false, stored = info, lastResult = "Backed up ${info.records} records to this device.")
                    }
                    toast("Backed up", "check_circle")
                },
                onFailure = { e ->
                    _backup.update { it.copy(busy = false, lastError = e.message ?: "Couldn't write the backup.") }
                },
            )
        }
    }

    /** First tap arms the confirmation, second actually restores — same pattern as "Clear all data". */
    fun askRestore() = _backup.update { it.copy(confirmingRestore = true, lastError = null, lastResult = null) }
    fun cancelRestore() = _backup.update { it.copy(confirmingRestore = false) }

    fun restoreFromDevice() {
        if (_backup.value.busy) return
        _backup.update { it.copy(busy = true, confirmingRestore = false, lastError = null, lastResult = null) }
        viewModelScope.launch { applyRestore(backupRepo.restoreFromDevice()) }
    }

    /** Writes the same format the local slot uses, so a saved copy can be restored on any install. */
    fun exportBackup(resolver: ContentResolver, uri: Uri) {
        if (_backup.value.busy) return
        _backup.update { it.copy(busy = true, lastError = null, lastResult = null) }
        viewModelScope.launch {
            try {
                val text = backupRepo.exportText()
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                        ?: error("Couldn't open that file for writing.")
                }
                _backup.update { it.copy(busy = false, lastResult = "Backup file saved.") }
                toast("Saved", "check_circle")
            } catch (e: Exception) {
                _backup.update { it.copy(busy = false, lastError = "Couldn't write that file.") }
            }
        }
    }

    fun restoreFromFile(resolver: ContentResolver, uri: Uri) {
        if (_backup.value.busy) return
        _backup.update { it.copy(busy = true, confirmingRestore = false, lastError = null, lastResult = null) }
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            }
            if (text == null) {
                _backup.update { it.copy(busy = false, lastError = "Couldn't read that file.") }
                return@launch
            }
            applyRestore(backupRepo.restoreFromText(text))
        }
    }

    private fun applyRestore(outcome: RestoreOutcome) {
        when (outcome) {
            is RestoreOutcome.Success -> {
                syncRepo.noteChange()
                _backup.update {
                    it.copy(busy = false, lastResult = "Restored ${outcome.info.records} records.", lastError = null)
                }
                toast("Restored", "check_circle")
            }
            is RestoreOutcome.Failure ->
                _backup.update { it.copy(busy = false, lastError = outcome.message) }
        }
    }

    // ---- cloud sync (snapshot sync against the user's own forge-sync server) ----
    fun setSyncServerUrl(url: String) = viewModelScope.launch { syncRepo.setServerUrl(url) }
    fun setSyncToken(token: String) = viewModelScope.launch { syncRepo.setToken(token) }
    fun clearSyncToken() = viewModelScope.launch { syncRepo.clearToken() }
    fun setAutoSync(on: Boolean) = viewModelScope.launch { syncRepo.setAutoSync(on) }

    private fun syncBusy(block: suspend () -> String) {
        if (_syncTransient.value.busy) return
        _syncTransient.update { it.copy(busy = true, lastResult = null, confirmingRestore = false) }
        viewModelScope.launch {
            val msg = block()
            _syncTransient.update { it.copy(busy = false, lastResult = msg) }
        }
    }

    fun testSyncConnection(url: String) = syncBusy {
        syncRepo.testConnection(url).fold(
            onSuccess = { "Connected — the server is up." },
            onFailure = { it.message ?: "Couldn't reach the server." },
        )
    }

    fun syncNow() = syncBusy {
        when (val r = syncRepo.syncNow()) {
            is SyncOutcome.Uploaded -> if (r.unchanged) "Already up to date (version ${r.version})." else "Uploaded snapshot version ${r.version}."
            is SyncOutcome.Failure -> r.message
            SyncOutcome.NotConfigured -> "Set the server URL and token first."
            is SyncOutcome.Restored -> "Restored version ${r.version}."
        }
    }

    fun askCloudRestore() = _syncTransient.update { it.copy(confirmingRestore = true, lastResult = null) }
    fun cancelCloudRestore() = _syncTransient.update { it.copy(confirmingRestore = false) }

    fun restoreFromCloud() = syncBusy {
        when (val r = syncRepo.restoreFromCloud()) {
            is SyncOutcome.Restored -> { toast("Restored from cloud", "check_circle"); "Restored ${r.records} records (version ${r.version})." }
            is SyncOutcome.Failure -> r.message
            SyncOutcome.NotConfigured -> "Set the server URL and token first."
            is SyncOutcome.Uploaded -> "Uploaded."
        }
    }

    // ---- toast ----
    fun toast(text: String, icon: String = "check_circle") {
        toastJob?.cancel()
        _nav.update { it.copy(toast = ToastMsg(text, icon)) }
        toastJob = viewModelScope.launch { delay(1900); _nav.update { it.copy(toast = null) } }
    }
}
