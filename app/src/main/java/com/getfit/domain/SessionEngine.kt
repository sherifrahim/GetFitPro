package com.getfit.domain

/**
 * Pure work/rest session state machine, ported from the prototype (L836-893).
 * All transitions are pure functions on an immutable SessionState — no Android, fully unit-tested.
 */

data class SessionItem(
    val id: String,
    val name: String,
    val muscle: String,
    val sets: Int,
    val reps: String,
    val bw: Boolean,
    val suggestW: Double,
)

data class LoggedSetFull(val id: String, val name: String, val weight: Double, val reps: Int)
data class PrItem(val id: String, val name: String, val value: String)

enum class Phase { WORK, REST, DONE }

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
) {
    val current: SessionItem get() = items[idx]
}

/** Initial rep target from a rep string ("8" -> 8, "45s" -> 45, else 10). */
fun initReps(reps: String): Int = reps.takeWhile { it.isDigit() }.toIntOrNull() ?: 10
fun isTimeBased(reps: String): Boolean = reps.endsWith("s")

/** Build the initial state. preBest maps exerciseId -> (bestWeight, bestReps). */
fun startSession(items: List<SessionItem>, restDefault: Int, preBest: Map<String, Pair<Double, Int>>): SessionState {
    val first = items.first()
    return SessionState(
        items = items,
        restTotal = restDefault,
        totalSets = items.sumOf { it.sets },
        preBest = preBest,
        curW = first.suggestW,
        curR = initReps(first.reps),
    )
}

/** 1-second tick: advances elapsed and counts down rest. */
fun tick(s: SessionState): SessionState {
    if (s.paused || s.phase == Phase.DONE) return s
    return if (s.phase == Phase.REST) {
        val rl = s.restLeft - 1
        if (rl <= 0) advanceFromRest(s, fromTick = true)
        else s.copy(restLeft = rl, elapsed = s.elapsed + 1)
    } else {
        s.copy(elapsed = s.elapsed + 1)
    }
}

/** Rest finished (or skipped): advance to the next set/exercise and re-enter work. */
fun advanceFromRest(s: SessionState, fromTick: Boolean): SessionState {
    val it = s.items[s.idx]
    var idx = s.idx
    var setNum = s.setNum
    var curW = s.curW
    var curR = s.curR
    if (s.setNum < it.sets) {
        setNum += 1
    } else {
        idx += 1
        setNum = 1
        val ni = s.items[idx]
        curW = ni.suggestW
        curR = initReps(ni.reps)
    }
    return s.copy(
        idx = idx, setNum = setNum, curW = curW, curR = curR,
        phase = Phase.WORK, restLeft = 0, elapsed = if (fromTick) s.elapsed + 1 else s.elapsed,
    )
}

/** Log the current set, detect PRs, then move to rest (or finish). Returns state + whether a PR hit. */
data class DoneResult(val state: SessionState, val pr: Boolean)

fun doneSet(s: SessionState, units: String): DoneResult {
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
    val lastSet = s.setNum >= it.sets
    val lastEx = s.idx >= s.items.size - 1
    val next = if (lastSet && lastEx) {
        s.copy(log = log, completedSets = completedSets, volume = volume, preBest = preBest, newPRs = newPRs, phase = Phase.DONE)
    } else {
        s.copy(log = log, completedSets = completedSets, volume = volume, preBest = preBest, newPRs = newPRs, phase = Phase.REST, restLeft = s.restTotal)
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

fun addRest(s: SessionState, delta: Int): SessionState =
    if (s.phase == Phase.REST) s.copy(restLeft = (s.restLeft + delta).coerceAtLeast(5)) else s

/** Live PR-pace indicator for the current set. */
fun prPace(s: SessionState): Boolean {
    val it = s.items[s.idx]
    val pb = s.preBest[it.id] ?: (0.0 to 0)
    return if (!it.bw) (s.curW > pb.first && s.curW > 0) else (s.curR > pb.second)
}
