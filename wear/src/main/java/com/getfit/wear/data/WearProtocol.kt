package com.getfit.wear.data

import kotlinx.serialization.Serializable

/**
 * Message paths for the Data Layer API's MessageClient. Deliberately duplicated on both the watch
 * (this file) and the phone (app/src/.../data/wear/WearProtocol.kt) rather than pulled from a
 * shared module — see docs/wear-companion-design.md section 1 for why. The two copies only need to
 * agree on JSON shape, not Kotlin package/class identity — that's all kotlinx.serialization cares
 * about across the wire.
 */
object WearPaths {
    const val SESSION_SNAPSHOT = "/forge/session"
    const val WATCH_ACTION = "/forge/action"
    const val HEART_RATE = "/forge/heartrate"
}

@Serializable
enum class WearPhase { WORK, REST, DONE }

/**
 * Compact, one-shot snapshot of the phone's active session, sent whenever SessionController's
 * state actually changes — not on every 1s tick. [workStartedAtMs]/[pausedAccumMs]/[restEndAtMs]
 * are the same wall-clock anchors SessionState itself uses on the phone, so this watch can
 * recompute a live elapsed/rest-countdown display every second purely locally (see the ticker in
 * MainActivity.kt), without needing a new message each tick. [active] = false means no session
 * running; the watch shows Idle.
 */
@Serializable
data class SessionSnapshot(
    val active: Boolean,
    val exerciseName: String = "",
    val setNum: Int = 1,
    val totalSetsForExercise: Int = 1,
    val curWeight: Double = 0.0,
    val curReps: Int = 0,
    val units: String = "kg",
    val bodyweight: Boolean = false,
    val phase: WearPhase = WearPhase.WORK,
    val restEndAtMs: Long = 0,
    val restTotal: Int = 60,
    val paused: Boolean = false,
    val workStartedAtMs: Long = 0,
    val pausedAccumMs: Long = 0,
    val completedSets: Int = 0,
    val totalSets: Int = 0,
)

enum class ActionKind { DONE_SET, SKIP_REST, ADJUST_REST, REQUEST_STATE }

/**
 * Action taken on the watch, sent phone-ward. The phone applies it exactly like a tap on its own
 * session screen (SessionController.doneSet() / .skip() / .addRest()) — one source of truth, no
 * separate watch-side session logic to keep in sync. [restDeltaSec] is only meaningful for
 * ADJUST_REST (the watch's Rest screen has a single "+15" action in v1).
 */
@Serializable
data class WatchAction(val kind: ActionKind, val restDeltaSec: Int = 0)

@Serializable
data class HeartRateSample(val bpm: Double, val atMs: Long)

/** Batched, not per-sample — continuous per-beat streaming over the radio is the actual battery
 *  cost in most watch apps, so this watch buffers locally and flushes every ~8s. */
@Serializable
data class HeartRateBatch(val samples: List<HeartRateSample>)
