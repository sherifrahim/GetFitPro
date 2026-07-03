package com.getfit.ui

import com.getfit.data.db.ExerciseEntity
import com.getfit.data.db.SessionEntity
import com.getfit.data.db.SetLogEntity
import com.getfit.data.db.TargetEntity
import com.getfit.data.prefs.PlanItemData
import com.getfit.domain.Best

/** Combined, hydrated data snapshot (the prototype's derived state, computed once). */
data class AppData(
    val exercises: List<ExerciseEntity> = emptyList(),
    val logs: List<SetLogEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val targets: List<TargetEntity> = emptyList(),
    val plan: List<PlanItemData> = emptyList(),
    val bestMap: Map<String, Best> = emptyMap(),
    val loaded: Boolean = false,
) {
    fun exercise(id: String): ExerciseEntity? = exercises.firstOrNull { it.id == id }
}

/** Transient navigation + overlay state (tab, search, overlays, toast). */
data class NavState(
    val tab: String = "home",
    val query: String = "",
    val filter: String = "All",
    val detailId: String? = null,
    val settingsOpen: Boolean = false,
    val goalOpen: Boolean = false,
    val booted: Boolean = false,
    val charted: Boolean = false,
    val confirmClear: Boolean = false,
    val goalEx: String = "bench",
    val goalTarget: Double = 110.0,
    val goalWeeks: Int = 8,
    val toast: ToastMsg? = null,
)

data class ToastMsg(val text: String, val icon: String)

const val TAB_HOME = "home"
const val TAB_EXERCISES = "exercises"
const val TAB_BUILD = "build"
const val TAB_PROGRESS = "progress"
