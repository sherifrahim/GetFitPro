package com.getfit.data.wear

import kotlinx.serialization.Serializable

/**
 * Message paths for the Data Layer API's MessageClient. Deliberately duplicated on both the phone
 * (this file) and the watch (wear/src/.../wear/data/WearProtocol.kt) rather than pulled from a
 * shared module — see docs/wear-companion-design.md section 1 for why: a shared module is a
 * reasonable follow-up once this is proven on real hardware, but not something to introduce blind
 * alongside a brand-new Gradle module with zero build verification.
 *
 * The two copies only need to agree on JSON shape, not on Kotlin package/class identity — that's
 * all kotlinx.serialization cares about across the wire.
 */
object WearPaths {
    const val SESSION_SNAPSHOT = "/forge/session"
    const val WATCH_ACTION = "/forge/action"
    const val HEART_RATE = "/forge/heartrate"
    // Standalone watch workouts: the watch uploads a finished session, the phone acknowledges by id.
    const val SESSION_UPLOAD = "/forge/upload"
    const val UPLOAD_ACK = "/forge/upload-ack"
}

@Serializable
enum class WearPhase { WORK, REST, DONE }

/**
 * Compact, one-shot snapshot of the phone's active session, sent whenever SessionController's
 * state actually changes — not on every 1s tick. [workStartedAtMs]/[pausedAccumMs]/[restEndAtMs]
 * are wall-clock anchors, the same ones SessionState itself uses (see SessionEngine.kt), so the
 * watch can recompute a live elapsed/rest-countdown display every second purely locally, without
 * needing a new message each tick. [active] = false means no session running; the watch shows Idle.
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
    // Routines, for the watch's Idle screen. [currentRoutineId] is the one up next; the list is in
    // rotation order. Present on every snapshot (active or not) so a watch that missed the last
    // routine edit still catches up on the next state change. Defaults keep old snapshots decodable.
    val routines: List<WearRoutine> = emptyList(),
    val currentRoutineId: String = "",
    // Name of the running session, shown on the Active/Rest screens. "" on older phones.
    val sessionName: String = "",
    // Rest length the phone would use, so a standalone watch session rests the same way.
    val restDefault: Int = 60,
)

/**
 * A routine as the watch needs it. [items] is everything a standalone session needs to run the
 * shared engine on the wrist: prefilled weights (display units), bodyweight flags and current bests
 * for PR detection, all resolved by the phone from its log.
 */
@Serializable
data class WearRoutine(val id: String, val name: String, val exercises: Int, val sets: Int, val items: List<WearItem> = emptyList())

@Serializable
data class WearItem(
    val id: String,
    val name: String,
    val muscle: String,
    val sets: Int,
    val reps: String,
    val bw: Boolean,
    val suggestW: Double,
    val equipment: String = "",
    val superset: Boolean = false,
    val bestW: Double = 0.0,
    val bestReps: Int = 0,
)

enum class ActionKind { DONE_SET, SKIP_REST, ADJUST_REST, REQUEST_STATE, SELECT_ROUTINE, START_ROUTINE, ADJUST_WEIGHT, ADJUST_REPS }

/**
 * Action taken on the watch, sent phone-ward. The phone applies it exactly like a tap on its own
 * session screen (SessionController.doneSet() / .skip() / .addRest()) — one source of truth, no
 * separate watch-side session logic to keep in sync. [restDeltaSec] is only meaningful for
 * ADJUST_REST (the watch's Rest screen has a single "+15" action in v1). [routineId] is only
 * meaningful for SELECT_ROUTINE (make it the one up next) and START_ROUTINE (begin a session for it;
 * blank = whichever is current).
 */
@Serializable
data class WatchAction(val kind: ActionKind, val restDeltaSec: Int = 0, val routineId: String = "", val delta: Int = 0)

@Serializable
data class HeartRateSample(val bpm: Double, val atMs: Long)

/** Batched, not per-sample — continuous per-beat streaming over the radio is the actual battery
 *  cost in most watch apps, so the watch buffers locally and flushes every ~8s (see HeartRateMonitor
 *  usage on the watch side). */
@Serializable
data class HeartRateBatch(val samples: List<HeartRateSample>)

/**
 * A workout finished on the watch without the phone, sent when the two reconnect. Weights are in
 * [units] (the watch works in display units like the phone's session screen); the phone converts to
 * kg, recomputes calories with the profile, and replies with [UploadAck]. [id] is stable across
 * retries so a duplicate delivery is a no-op on the phone.
 */
@Serializable
data class WearSessionUpload(
    val id: String,
    val name: String,
    val routineId: String,
    val startedAtMs: Long,
    val durationSec: Int,
    val units: String,
    val sets: List<WearLoggedSet>,
    val prs: Int,
    val hr: List<HeartRateSample> = emptyList(),
)

@Serializable
data class WearLoggedSet(val exerciseId: String, val name: String, val weight: Double, val reps: Int, val bw: Boolean)

@Serializable
data class UploadAck(val id: String)
