package com.getfit.domain

import kotlinx.serialization.Serializable

/**
 * Pure work/rest session state machine, ported from the prototype (L836-893).
 * All transitions are pure functions on an immutable SessionState — no Android, fully unit-tested.
 * Serializable so an in-progress session survives process death (persisted via DataStore).
 *
 * Timing is wall-clock anchored, not tick-counted: [workStartedAtMs]/[pausedAccumMs] derive
 * [elapsed] and [restEndAtMs] derives [restLeft]. [tick] recomputes both from `now` on every call
 * instead of blindly subtracting 1, so a delayed or missed tick (Doze, a backgrounded coroutine,
 * even a killed-and-restored process) self-corrects to the true elapsed time instead of drifting.
 * [restLeft]/[elapsed] stay as plain stored Int fields — kept in sync by every function below —
 * so existing UI call sites (SessionScreen.kt) that just read `s.restLeft` / `s.elapsed` need no
 * changes.
 */

@Serializable
data class SessionItem(
    val id: String,
    val name: String,
    val muscle: String,
    val sets: Int,
    val reps: String,
    val bw: Boolean,
    val suggestW: Double,
    /** Equipment string from the library; "Barbell" turns on the plate calculator and warm-up ramp. */
    val equipment: String = "",
    /** Linked with the NEXT item as a superset: their sets alternate with no rest in between. */
    val superset: Boolean = false,
)

@Serializable
data class LoggedSetFull(val id: String, val name: String, val weight: Double, val reps: Int)

@Serializable
data class PrItem(val id: String, val name: String, val value: String)

@Serializable
data class HrPoint(val atMs: Long, val bpm: Int)

/** Bucket size for [SessionState.hr]; see [addHeartRate]. */
const val HR_BUCKET_MS = 5_000L

/**
 * Merges new watch samples into the trace, keeping the LAST reading per 5s bucket. Samples arrive
 * in ~8s batches and may overlap the previous batch, so this is idempotent on re-delivery.
 */
fun addHeartRate(s: SessionState, samples: List<HrPoint>): SessionState {
    if (samples.isEmpty()) return s
    val byBucket = LinkedHashMap<Long, HrPoint>()
    (s.hr + samples).sortedBy { it.atMs }.forEach { p -> if (p.bpm > 0) byBucket[p.atMs / HR_BUCKET_MS] = p }
    return s.copy(hr = byBucket.values.toList())
}

enum class Phase { WORK, REST, DONE }

@Serializable
data class SessionState(
    val items: List<SessionItem>,
    val idx: Int = 0,
    val setNum: Int = 1,
    val phase: Phase = Phase.WORK,
    val restLeft: Int = 0,
    val restTotal: Int = 60,
    val elapsed: Int = 0,
    val paused: Boolean = false,
    val completedSets: Int = 0,
    val totalSets: Int = 0,
    val log: List<LoggedSetFull> = emptyList(),
    val newPRs: List<PrItem> = emptyList(),
    val volume: Double = 0.0,
    val preBest: Map<String, Pair<Double, Int>> = emptyMap(),
    val curW: Double = 0.0,
    val curR: Int = 10,
    // Wall-clock anchors (epoch ms). Source of truth for elapsed/restLeft; see class doc.
    val workStartedAtMs: Long = 0,
    val pausedAccumMs: Long = 0,
    val pauseBeganAtMs: Long = 0, // 0 = not currently paused
    val restEndAtMs: Long = 0,    // meaningful only while phase == REST
    // Routine context (v2). [name] is what the saved history record is called; [routineId] is ""
    // for an ad-hoc single-exercise session. [hr] is the watch heart-rate trace, downsampled to one
    // point per 5s so persisting the state on every tap stays cheap.
    val name: String = "Workout",
    val routineId: String = "",
    val hr: List<HrPoint> = emptyList(),
) {
    val current: SessionItem get() = items[idx]
}

/** Initial rep target from a rep string ("8" -> 8, "45s" -> 45, else 10). */
fun initReps(reps: String): Int = reps.takeWhile { it.isDigit() }.toIntOrNull() ?: 10
fun isTimeBased(reps: String): Boolean = reps.endsWith("s")

private fun elapsedSec(s: SessionState, now: Long): Int =
    ((now - s.workStartedAtMs - s.pausedAccumMs) / 1000).toInt().coerceAtLeast(0)

private fun restLeftSec(s: SessionState, now: Long): Int =
    ((s.restEndAtMs - now) / 1000).toInt()

/** Build the initial state. preBest maps exerciseId -> (bestWeight, bestReps). */
fun startSession(
    items: List<SessionItem>,
    restDefault: Int,
    preBest: Map<String, Pair<Double, Int>>,
    now: Long = System.currentTimeMillis(),
): SessionState {
    val first = items.first()
    return SessionState(
        items = items,
        restTotal = restDefault,
        totalSets = items.sumOf { it.sets },
        preBest = preBest,
        curW = first.suggestW,
        curR = initReps(first.reps),
        workStartedAtMs = now,
    )
}

/**
 * Recompute elapsed/restLeft from the wall clock. Called both by the periodic 1s ticker and once
 * immediately after restoring a persisted session, so a gap of any length (a throttled ticker, or
 * the app having been closed) self-corrects instead of resuming from a stale counted value.
 */
fun tick(s: SessionState, now: Long = System.currentTimeMillis()): SessionState {
    if (s.paused || s.phase == Phase.DONE) return s
    return if (s.phase == Phase.REST) {
        val rl = restLeftSec(s, now)
        if (rl <= 0) advanceFromRest(s, fromTick = true, now = now)
        else s.copy(restLeft = rl, elapsed = elapsedSec(s, now))
    } else {
        s.copy(elapsed = elapsedSec(s, now))
    }
}

/**
 * Where the session goes after the set at (idx, setNum) is logged: the next (item, set) and whether
 * a rest comes first. Null = that was the last set of the session.
 *
 * Supersets: consecutive items linked by [SessionItem.superset] form a group. A group is worked in
 * rounds — set 1 of each member back-to-back with NO rest, then a rest, then set 2 of each, and so
 * on. Members with fewer sets simply drop out of later rounds. Everything else is the prototype's
 * linear order: next set of the same exercise, else the next exercise, resting in between.
 */
data class NextPosition(val idx: Int, val setNum: Int, val restFirst: Boolean)

fun nextPosition(s: SessionState): NextPosition? {
    val items = s.items
    var start = s.idx
    while (start > 0 && items[start - 1].superset) start--
    var end = s.idx
    while (end < items.size - 1 && items[end].superset) end++

    if (start == end) {
        // Plain exercise (prototype path).
        return when {
            s.setNum < items[s.idx].sets -> NextPosition(s.idx, s.setNum + 1, restFirst = true)
            s.idx + 1 < items.size -> NextPosition(s.idx + 1, 1, restFirst = true)
            else -> null
        }
    }
    // Same round, next member that still has this set number: straight into it, no rest.
    for (k in s.idx + 1..end) if (items[k].sets >= s.setNum) return NextPosition(k, s.setNum, restFirst = false)
    // Next round, first member with a set left.
    val round = s.setNum + 1
    for (k in start..end) if (items[k].sets >= round) return NextPosition(k, round, restFirst = true)
    // Group exhausted.
    return if (end + 1 < items.size) NextPosition(end + 1, 1, restFirst = true) else null
}

/** Applies a [NextPosition] to the state: new item/set, that item's prefilled weight/reps, WORK. */
private fun moveTo(s: SessionState, p: NextPosition, now: Long): SessionState {
    val ni = s.items[p.idx]
    val sameItem = p.idx == s.idx
    // Same exercise again: keep what the user dialled in. Coming back to an exercise (superset round
    // 2): its last logged set. A fresh exercise: the prefill.
    val last = if (sameItem) null else s.log.lastOrNull { it.id == ni.id }
    return s.copy(
        idx = p.idx, setNum = p.setNum,
        curW = if (sameItem) s.curW else last?.weight ?: ni.suggestW,
        curR = if (sameItem) s.curR else last?.reps ?: initReps(ni.reps),
        phase = Phase.WORK, restLeft = 0, elapsed = elapsedSec(s, now),
    )
}

/** Rest finished (or skipped): advance to the next set/exercise and re-enter work. */
fun advanceFromRest(s: SessionState, fromTick: Boolean, now: Long = System.currentTimeMillis()): SessionState {
    val p = nextPosition(s) ?: return s.copy(phase = Phase.DONE, elapsed = elapsedSec(s, now))
    return moveTo(s, p, now)
}

/** Log the current set, detect PRs, then move to rest (or finish). Returns state + whether a PR hit. */
data class DoneResult(val state: SessionState, val pr: Boolean)

fun doneSet(s: SessionState, units: String, now: Long = System.currentTimeMillis()): DoneResult {
    val it = s.items[s.idx]
    val w = s.curW; val reps = s.curR
    val log = s.log + LoggedSetFull(it.id, it.name, w, reps)
    val completedSets = minOf(s.totalSets, s.completedSets + 1)
    val volume = if (it.bw) s.volume else s.volume + w * reps
    val pb = s.preBest[it.id] ?: (0.0 to 0)
    var pr = false
    var preBest = s.preBest
    if (it.bw) {
        if (reps > pb.second) { pr = true; preBest = preBest + (it.id to (0.0 to reps)) }
    } else if (w > pb.first) {
        pr = true; preBest = preBest + (it.id to (w to reps))
    }
    val newPRs = if (pr) {
        s.newPRs.filterNot { it2 -> it2.id == it.id } +
            PrItem(it.id, it.name, if (it.bw) "$reps reps" else "${fmtW(w)} $units × $reps")
    } else s.newPRs
    val elapsed = elapsedSec(s, now)
    val logged = s.copy(log = log, completedSets = completedSets, volume = volume, preBest = preBest, newPRs = newPRs, elapsed = elapsed)
    val p = nextPosition(s)
    val next = when {
        p == null -> logged.copy(phase = Phase.DONE)
        // Superset partner: straight into its set, no rest.
        !p.restFirst -> moveTo(logged, p, now)
        else -> logged.copy(phase = Phase.REST, restLeft = s.restTotal, restEndAtMs = now + s.restTotal * 1000L)
    }
    return DoneResult(next, pr)
}

fun adjustW(s: SessionState, dir: Int, units: String): SessionState {
    val step = if (units == "lb") 5.0 else 2.5
    val v = (s.curW + dir * step).coerceAtLeast(0.0)
    return s.copy(curW = Math.round(v * 100) / 100.0)
}

fun adjustR(s: SessionState, dir: Int): SessionState {
    val step = if (isTimeBased(s.items[s.idx].reps)) 5 else 1
    return s.copy(curR = (s.curR + dir * step).coerceAtLeast(1))
}

/** +/- to the rest countdown while resting; floor of 5s left, mirrored in both the anchor and the display field. */
fun addRest(s: SessionState, delta: Int, now: Long = System.currentTimeMillis()): SessionState {
    if (s.phase != Phase.REST) return s
    val minEnd = now + 5_000L
    val newEnd = (s.restEndAtMs + delta * 1000L).coerceAtLeast(minEnd)
    return s.copy(restEndAtMs = newEnd, restLeft = ((newEnd - now) / 1000).toInt())
}

/**
 * Pause/resume. Pausing freezes elapsed (by accumulating the paused span into [SessionState.pausedAccumMs]
 * once resumed) and, if resting, shifts [SessionState.restEndAtMs] forward by the paused span so the rest
 * countdown doesn't lose time while paused — matching the pre-existing behavior where tick() was simply a
 * no-op while paused, just now robust to the app being backgrounded mid-pause.
 *
 * [pauseSession] and [resumeSession] are exposed separately (not just as a toggle) because some
 * callers need to unconditionally pause — e.g. holding the rest timer when auto-start-rest is off —
 * without caring or asserting what the current paused state already was.
 */
fun pauseSession(s: SessionState, now: Long = System.currentTimeMillis()): SessionState =
    if (s.paused) s else s.copy(paused = true, pauseBeganAtMs = now)

fun resumeSession(s: SessionState, now: Long = System.currentTimeMillis()): SessionState {
    if (!s.paused) return s
    val pausedSpan = if (s.pauseBeganAtMs > 0) (now - s.pauseBeganAtMs).coerceAtLeast(0) else 0L
    val newRestEnd = if (s.phase == Phase.REST) s.restEndAtMs + pausedSpan else s.restEndAtMs
    return s.copy(
        paused = false, pauseBeganAtMs = 0, pausedAccumMs = s.pausedAccumMs + pausedSpan,
        restEndAtMs = newRestEnd,
    )
}

fun togglePause(s: SessionState, now: Long = System.currentTimeMillis()): SessionState =
    if (!s.paused) pauseSession(s, now) else resumeSession(s, now)

/** Live PR-pace indicator for the current set. */
fun prPace(s: SessionState): Boolean {
    val it = s.items[s.idx]
    val pb = s.preBest[it.id] ?: (0.0 to 0)
    return if (!it.bw) (s.curW > pb.first && s.curW > 0) else (s.curR > pb.second)
}
