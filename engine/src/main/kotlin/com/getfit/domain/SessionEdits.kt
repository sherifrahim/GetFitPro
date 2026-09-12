package com.getfit.domain

/*
 * Mid-workout edits: Hevy's per-exercise menu ("Replace", "Remove", skip) and its "Add exercise"
 * button on the active workout. Pure transitions on SessionState, like everything in
 * SessionEngine.kt, so the phone and the standalone watch behave identically.
 */

/**
 * [SessionState.totalSets] after the item list changed: every planned set still in the list, plus
 * any set already logged for an exercise that is no longer in it (replaced or removed), so the
 * progress denominator never drops below what has been done.
 */
private fun recomputeTotals(s: SessionState): SessionState =
    s.copy(totalSets = s.items.sumOf { it.sets } + s.log.count { l -> s.items.none { it.id == l.id } })

private fun elapsedNow(s: SessionState, now: Long): Int =
    ((now - s.workStartedAtMs - s.pausedAccumMs) / 1000).toInt().coerceAtLeast(0)

/** Append an exercise to the running session. Nothing currently in progress moves. */
fun addItem(s: SessionState, item: SessionItem): SessionState {
    if (s.items.any { it.id == item.id }) return s
    return recomputeTotals(s.copy(items = s.items + item))
}

/**
 * Swap the exercise at [idx] for another. Sets already logged for the old one stay in the log; if
 * it is the current exercise, the new one starts from set 1 with its own prefill. A superset link
 * on the slot is kept — the pairing is positional, not about the exercise.
 */
fun replaceItem(s: SessionState, idx: Int, item: SessionItem, now: Long = System.currentTimeMillis()): SessionState {
    if (idx !in s.items.indices || s.items.any { it.id == item.id }) return s
    val replaced = item.copy(superset = s.items[idx].superset)
    val items = s.items.toMutableList().also { it[idx] = replaced }
    val base = s.copy(items = items)
    return recomputeTotals(
        if (idx == s.idx) {
            base.copy(setNum = 1, curW = replaced.suggestW, curR = initReps(replaced.reps), phase = Phase.WORK, restLeft = 0, elapsed = elapsedNow(s, now))
        } else base,
    )
}

/** True when the exercise at [idx] can be removed: nothing logged for it, and it is not the only one. */
fun canRemoveItem(s: SessionState, idx: Int): Boolean =
    idx in s.items.indices && s.items.size > 1 && s.log.none { it.id == s.items[idx].id }

/**
 * Drop an unstarted exercise. Removing the current one jumps to what would have followed it. The
 * superset chain is repaired so no link dangles onto the removed slot.
 */
fun removeItem(s: SessionState, idx: Int, now: Long = System.currentTimeMillis()): SessionState {
    if (!canRemoveItem(s, idx)) return s
    val items = s.items.toMutableList()
    val removed = items.removeAt(idx)
    // The item before the hole stays linked only if the removed item itself linked onward.
    if (idx > 0 && items[idx - 1].superset) items[idx - 1] = items[idx - 1].copy(superset = removed.superset && idx < items.size)
    // The last item can never link forward.
    items[items.lastIndex] = items[items.lastIndex].copy(superset = false)
    val newIdx = when {
        idx < s.idx -> s.idx - 1
        idx == s.idx -> minOf(s.idx, items.lastIndex)
        else -> s.idx
    }
    val base = s.copy(items = items, idx = newIdx)
    return recomputeTotals(
        if (idx == s.idx) {
            val ni = items[newIdx]
            base.copy(setNum = 1, curW = ni.suggestW, curR = initReps(ni.reps), phase = Phase.WORK, restLeft = 0, elapsed = elapsedNow(s, now))
        } else base,
    )
}

/**
 * Give up on the rest of the current exercise and move on. Its planned sets shrink to what was
 * logged (so totals stay honest) and the session advances as if those sets were done — into the
* next exercise (a superset partner if there is one), or DONE if this was the last.
 */
fun skipExercise(s: SessionState, now: Long = System.currentTimeMillis()): SessionState {
    if (s.phase == Phase.DONE) return s
    val cur = s.items[s.idx]
    val logged = s.log.count { it.id == cur.id }
    val items = s.items.toMutableList()
    // Keep at least one planned set so nextPosition's arithmetic stays valid; a skipped exercise
    // with nothing logged shows as one planned set that never happened.
    items[s.idx] = cur.copy(sets = maxOf(1, logged))
    val trimmed = recomputeTotals(s.copy(items = items, setNum = items[s.idx].sets, phase = Phase.WORK))
    // advanceFromRest is exactly "the set at (idx, setNum) is over, go to what follows" — DONE when nothing does.
    return advanceFromRest(trimmed, fromTick = false, now = now)
}
