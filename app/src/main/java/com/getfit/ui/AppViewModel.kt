package com.getfit.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getfit.data.ai.AiResult
import com.getfit.data.ai.AiReviewClient
import com.getfit.data.ai.AnthropicClient
import com.getfit.data.ai.BodyGoal
import com.getfit.data.ai.ImageEncoder
import com.getfit.data.ai.PhotoView
import com.getfit.data.ai.bodyAnalysisSystemPrompt
import com.getfit.data.ai.buildBodyAnalysisContent
import com.getfit.data.ai.AiReviewResult
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

    val hasAiKey: StateFlow<Boolean> =
        container.secureKeyStore.hasKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    private val _syncBusy = MutableStateFlow(false)
    val syncUi: StateFlow<SyncUiState> = combine(syncRepo.state, _syncBusy) { s, busy -> SyncUiState(s, busy) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncUiState())

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

    fun startSession(items: List<PlanItemData>) { sessionController.start(items) }
    fun startSingle(id: String, reps: String) { sessionController.start(listOf(PlanItemData(id, 3, reps))) }
    fun endSession() = viewModelScope.launch { sessionController.endAndSave(); selectTab(TAB_PROGRESS) }

    // ---- wear OS companion (mirror session state to a paired watch, apply actions/heart rate it
    // sends back) — see docs/wear-companion-design.md. Placed here, after sessionController exists,
    // rather than in the class's first init{} block above: that block runs before sessionController's
    // own initializer, so referencing it there would read it before construction.
    private val _liveHeartRateBpm = MutableStateFlow<Double?>(null)
    val liveHeartRateBpm: StateFlow<Double?> = _liveHeartRateBpm.asStateFlow()

    private val wearListener: MessageClient.OnMessageReceivedListener

    init {
        viewModelScope.launch {
            sessionController.state.collect { s ->
                container.phoneWearSync.sendSnapshot(toWearSnapshot(s, settings.value.units))
            }
        }
        wearListener = container.phoneWearSync.listen(
            onAction = { action ->
                when (action.kind) {
                    ActionKind.DONE_SET -> sessionController.doneSet()
                    ActionKind.SKIP_REST -> sessionController.skip()
                    ActionKind.ADJUST_REST -> sessionController.addRest(action.restDeltaSec)
                }
            },
            onHeartRate = { batch -> batch.samples.lastOrNull()?.let { _liveHeartRateBpm.value = it.bpm } },
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
            val apiKey = container.secureKeyStore.getApiKey()
            if (apiKey.isNullOrBlank()) {
                _aiReview.update { it.copy(loading = false, error = "Add your API key in Settings first.") }
                return@launch
            }
            val summary = currentWorkoutSummary()
            when (val result = AiReviewClient.review(apiKey, settings.value.aiModel, summary)) {
                is AiReviewResult.Success -> _aiReview.update { it.copy(loading = false, text = result.text, error = null) }
                is AiReviewResult.Failure -> _aiReview.update { it.copy(loading = false, error = result.message) }
            }
        }
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
            val apiKey = container.secureKeyStore.getApiKey()
            if (apiKey.isNullOrBlank()) {
                _bodyCheck.update { it.copy(loading = false, error = "Add your API key in Settings first.") }
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
            val result = AnthropicClient.send(apiKey, settings.value.aiModel, bodyAnalysisSystemPrompt(), content, maxTokens = 8000)
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
                    val s = outcome.summary
                    val msg = buildString {
                        append("Imported ${s.setsImported} sets across ${s.sessionsImported} sessions from ${s.format.label}")
                        if (s.exercisesCreated > 0) append(" — added ${s.exercisesCreated} new exercises")
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
                _backup.update {
                    it.copy(busy = false, lastResult = "Restored ${outcome.info.records} records.", lastError = null)
                }
                toast("Restored", "check_circle")
            }
            is RestoreOutcome.Failure ->
                _backup.update { it.copy(busy = false, lastError = outcome.message) }
        }
    }

    // ---- cloud sync (client-side groundwork; inert until a server URL is set) ----
    fun setSyncServerUrl(url: String) = viewModelScope.launch { syncRepo.setServerUrl(url) }

    fun syncNow() {
        if (_syncBusy.value) return
        _syncBusy.value = true
        viewModelScope.launch {
            syncRepo.syncNow()
            _syncBusy.value = false
        }
    }

    // ---- toast ----
    fun toast(text: String, icon: String = "check_circle") {
        toastJob?.cancel()
        _nav.update { it.copy(toast = ToastMsg(text, icon)) }
        toastJob = viewModelScope.launch { delay(1900); _nav.update { it.copy(toast = null) } }
    }
}
