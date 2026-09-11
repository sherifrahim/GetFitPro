package com.getfit.ui

import com.getfit.data.db.ExerciseEntity
import com.getfit.data.db.SessionEntity
import com.getfit.data.db.SessionSetEntity
import com.getfit.data.db.SetLogEntity
import com.getfit.data.db.TargetEntity
import com.getfit.data.db.MeasurementEntity
import com.getfit.data.prefs.PlanItemData
import com.getfit.data.prefs.Routine
import com.getfit.data.prefs.RoutinesData
import com.getfit.domain.Best

/** Combined, hydrated data snapshot (the prototype's derived state, computed once). */
data class AppData(
    val exercises: List<ExerciseEntity> = emptyList(),
    val logs: List<SetLogEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val targets: List<TargetEntity> = emptyList(),
    val routinesData: RoutinesData = RoutinesData(),
    val measurements: List<MeasurementEntity> = emptyList(),
    /** Every session's sets (history feed previews, workout detail). */
    val sessionSets: List<SessionSetEntity> = emptyList(),
    val bestMap: Map<String, Best> = emptyMap(),
    val loaded: Boolean = false,
) {
    fun exercise(id: String): ExerciseEntity? = exercises.firstOrNull { it.id == id }
    fun routine(id: String?): Routine? = routinesData.routines.firstOrNull { it.id == id }

    val routines: List<Routine> get() = routinesData.routines
    /** The routine that is up next — the one Home offers and the watch starts by default. */
    val currentRoutine: Routine? get() = routinesData.current
    /** Its items: the single plan the prototype had. Kept so the session and stats code reads the same. */
    val plan: List<PlanItemData> get() = currentRoutine?.items.orEmpty()
}

/** Transient navigation + overlay state (tab, search, overlays, toast). */
data class NavState(
    val tab: String = "home",
    val query: String = "",
    val filter: String = "All",
    val detailId: String? = null,
    val settingsOpen: Boolean = false,
    val goalOpen: Boolean = false,
    val aiReviewOpen: Boolean = false,
    val bodyCheckOpen: Boolean = false,
    /** Build tab: the routine whose editor is open (null = the routines list). */
    val editRoutineId: String? = null,
    /** Build tab: the routine whose "⋯" sheet is open. */
    val routineMenuId: String? = null,
    /** Build tab: the program templates sheet. */
    val programsOpen: Boolean = false,
    /** Progress tab: the session whose detail overlay is open. */
    val sessionDetailId: String? = null,
    val recapOpen: Boolean = false,
    val profileOpen: Boolean = false,
    val measurementsOpen: Boolean = false,
    val booted: Boolean = false,
    val charted: Boolean = false,
    val confirmClear: Boolean = false,
    val obSlide: Int = 0,
    val goalEx: String = "bench",
    val goalTarget: Double = 110.0,
    val goalWeeks: Int = 8,
    val toast: ToastMsg? = null,
)

data class ToastMsg(val text: String, val icon: String)

/** State for the AI review overlay: idle (nothing requested yet), loading, a result, or an error. */
data class AiReviewUiState(
    val loading: Boolean = false,
    val text: String? = null,
    val error: String? = null,
)

/** State for the Settings "Import & export" section. */
data class ImportExportUiState(
    val busy: Boolean = false,
    val lastResult: String? = null,
    val lastError: String? = null,
)

/**
 * State for the Settings "Backup" section. [stored] is the on-device backup's metadata, or null
 * when none has been made yet. [confirmingRestore] gates the restore behind a second tap, because
 * restoring replaces the user's current history.
 */
data class BackupUiState(
    val busy: Boolean = false,
    val stored: com.getfit.data.backup.BackupInfo? = null,
    val confirmingRestore: Boolean = false,
    val lastResult: String? = null,
    val lastError: String? = null,
)

/** State for the Settings "Cloud sync" section: the persisted sync state plus a transient
 *  in-progress flag that isn't itself persisted. */
data class SyncUiState(
    val state: com.getfit.data.sync.SyncState = com.getfit.data.sync.SyncState(),
    val hasToken: Boolean = false,
    val busy: Boolean = false,
    /** Restore-from-cloud replaces local data, so it is confirm-gated like the local restore. */
    val confirmingRestore: Boolean = false,
    /** Transient result of the last button press, shown under the buttons. */
    val lastResult: String? = null,
) {
    val configured: Boolean get() = state.serverUrl.isNotBlank() && hasToken
}

const val TAB_HOME = "home"
const val TAB_EXERCISES = "exercises"
const val TAB_BUILD = "build"
const val TAB_PROGRESS = "progress"

/** One photo chosen for the body check: the picker URI (never persisted) and which view it shows. */
data class PickedPhoto(val uri: android.net.Uri, val view: com.getfit.data.ai.PhotoView)

/**
 * State for the body check overlay. [consented] must be re-ticked every time the screen is opened —
 * it is reset on close on purpose, because the photos leave the device and one-time consent must
 * not silently become standing consent.
 */
data class BodyCheckUiState(
    val photos: List<PickedPhoto> = emptyList(),
    val goal: com.getfit.data.ai.BodyGoal = com.getfit.data.ai.BodyGoal.BUILD_MUSCLE,
    val focusArea: String = "",
    val consented: Boolean = false,
    val loading: Boolean = false,
    val text: String? = null,
    val error: String? = null,
)
