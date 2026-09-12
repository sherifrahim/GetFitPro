package com.getfit.data.wear

import android.content.Context
import com.getfit.domain.Phase
import com.getfit.domain.SessionState
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Phone-side counterpart to the watch's WatchMessenger (wear/.../wear/data/WatchMessenger.kt) —
 * same protocol, opposite direction: sends session snapshots out, listens for watch actions and
 * heart rate batches coming back. See docs/wear-companion-design.md section 2 for why MessageClient
 * (one-shot, on-change) rather than DataClient (durable, syncable) is the right fit here.
 */
class PhoneWearSync(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    // Avoids re-sending (and waking a paired watch's radio) when SessionController's ticker
    // recomputes state every second but nothing meaningful actually changed.
    private var lastSentJson: String? = null

    /**
     * [force] bypasses the dedupe. Used when the watch explicitly asks for the current state (it just
     * opened, or its listener service just started) — the bytes may be identical to the last send,
     * but the watch never received that one.
     */
    /** Tells the watch a standalone workout it uploaded is safely stored, so it can drop its copy. */
    fun sendUploadAck(id: String) = send(WearPaths.UPLOAD_ACK, json.encodeToString(UploadAck(id)))

    fun sendSnapshot(snapshot: SessionSnapshot, force: Boolean = false) {
        val body = json.encodeToString(snapshot)
        if (!force && body == lastSentJson) return
        lastSentJson = body
        send(WearPaths.SESSION_SNAPSHOT, body)
    }

    private fun send(path: String, jsonBody: String) {
        val payload = jsonBody.toByteArray()
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node -> messageClient.sendMessage(node.id, path, payload) }
        }
    }

    /** Registers a listener for incoming watch messages. Returns the listener so the caller can
     *  unregister it later via [stopListening] — there's no owning lifecycle here (this class is a
     *  singleton on AppContainer), so the caller (AppViewModel) owns that lifecycle instead. */
    fun listen(onAction: (WatchAction) -> Unit, onHeartRate: (HeartRateBatch) -> Unit): MessageClient.OnMessageReceivedListener {
        val listener = MessageClient.OnMessageReceivedListener { event ->
            when (event.path) {
                WearPaths.WATCH_ACTION ->
                    runCatching { json.decodeFromString<WatchAction>(String(event.data)) }.getOrNull()?.let(onAction)
                WearPaths.HEART_RATE ->
                    runCatching { json.decodeFromString<HeartRateBatch>(String(event.data)) }.getOrNull()?.let(onHeartRate)
            }
        }
        messageClient.addListener(listener)
        return listener
    }

    fun stopListening(listener: MessageClient.OnMessageReceivedListener) {
        messageClient.removeListener(listener)
    }
}

/** Maps the phone's SessionState (SessionEngine.kt) to the compact wire snapshot. Null state (no
 *  active session) becomes `active = false`, which is all the watch's Idle screen checks for. */
fun toWearSnapshot(
    state: SessionState?,
    units: String,
    wearRoutines: List<WearRoutine> = emptyList(),
    currentId: String = "",
    restDefault: Int = 60,
): SessionSnapshot {
    if (state == null) return SessionSnapshot(active = false, routines = wearRoutines, currentRoutineId = currentId, restDefault = restDefault)
    val item = state.current
    return SessionSnapshot(
        // true for the whole session including its brief DONE phase — the watch shows a "Session
        // end" summary screen for DONE, not Idle; Idle only happens when there's no session at all
        // (state == null, handled by the early return above), which SessionController sets once the
        // user actually finishes reviewing and ends the session on the phone (endAndSave()).
        active = true,
        exerciseName = item.name,
        setNum = state.setNum,
        totalSetsForExercise = item.sets,
        curWeight = state.curW,
        curReps = state.curR,
        units = units,
        bodyweight = item.bw,
        phase = when (state.phase) {
            Phase.WORK -> WearPhase.WORK
            Phase.REST -> WearPhase.REST
            Phase.DONE -> WearPhase.DONE
        },
        restEndAtMs = state.restEndAtMs,
        restTotal = state.restTotal,
        paused = state.paused,
        workStartedAtMs = state.workStartedAtMs,
        pausedAccumMs = state.pausedAccumMs,
        completedSets = state.completedSets,
        totalSets = state.totalSets,
        routines = wearRoutines,
        currentRoutineId = currentId,
        sessionName = state.name,
        restDefault = restDefault,
    )
}

/**
 * Routines as the watch needs them, resolved from the phone's data: display-unit prefill from the
 * last logged set (else the curated default, else 20), and current bests for on-wrist PR detection.
 * Built once per data change (see AppViewModel), not per snapshot.
 */
fun toWearRoutines(
    routines: List<com.getfit.data.prefs.Routine>,
    exercises: Map<String, com.getfit.data.db.ExerciseEntity>,
    lastKgByExercise: Map<String, Double>,
    bestMap: Map<String, com.getfit.domain.Best>,
    units: String,
    lastSetsByExercise: Map<String, String> = emptyMap(),
): List<WearRoutine> = routines.map { r ->
    val items = r.items.mapNotNull { p ->
        val e = exercises[p.id] ?: return@mapNotNull null
        val bw = com.getfit.domain.isBW(e.equipment, e.reps)
        val suggestKg = lastKgByExercise[p.id] ?: com.getfit.data.db.Curated.DEFAULT_WEIGHT[p.id] ?: 20.0
        val best = bestMap[p.id]
        WearItem(
            id = p.id, name = e.name, muscle = e.muscle, sets = p.sets, reps = p.reps, bw = bw,
            suggestW = com.getfit.domain.Units.roundDisplay(com.getfit.domain.Units.toDisplay(suggestKg, units)),
            equipment = e.equipment, superset = p.superset,
            bestW = best?.let { com.getfit.domain.Units.toDisplay(it.weight, units) } ?: 0.0, bestReps = best?.reps ?: 0,
            last = lastSetsByExercise[p.id].orEmpty(),
            restSec = p.restSec,
        )
    }
    WearRoutine(r.id, r.name, r.items.size, r.setsTotal, items)
}
