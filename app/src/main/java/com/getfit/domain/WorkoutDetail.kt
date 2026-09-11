package com.getfit.domain

import kotlin.math.roundToInt

/** One set of a saved session, in canonical kg. */
data class SessionSetRow(val exerciseId: String, val name: String, val weightKg: Double, val reps: Int)

/**
 * How this session's top set for an exercise compares with the previous session that did it —
 * the "improvements in loads" a workout feed is for. Weighted lifts compare top weight (tiebreak
 * reps), bodyweight ones compare top reps. [delta] is null when there is no earlier session.
 */
data class ExerciseDelta(
    val exerciseId: String,
    val bodyweight: Boolean,
    val topThis: Double,
    val topPrev: Double?,
) {
    val delta: Double? get() = topPrev?.let { topThis - it }
}

fun exerciseDeltas(
    sessionDateMs: Long,
    sets: List<SessionSetRow>,
    allLogs: List<LoggedSet>,
    isBodyweight: (String) -> Boolean,
): Map<String, ExerciseDelta> {
    val byEx = sets.groupBy { it.exerciseId }
    return byEx.mapValues { (exId, rows) ->
        val bw = isBodyweight(exId)
        val topThis = if (bw) rows.maxOf { it.reps }.toDouble() else rows.maxOf { it.weightKg }
        // Previous = the most recent earlier day this exercise was logged on.
        val earlier = allLogs.filter { it.exerciseId == exId && it.dateMs < sessionDateMs }
        val prevDay = earlier.maxOfOrNull { it.dateMs }
        val topPrev = prevDay?.let { d ->
            val same = earlier.filter { it.dateMs == d }
            if (bw) same.maxOf { it.reps }.toDouble() else same.maxOf { it.weight }
        }
        ExerciseDelta(exId, bw, topThis, topPrev)
    }
}

/** Share of a session's sets per muscle group, largest first, percentages summing to ~100. */
fun muscleSplit(sets: List<SessionSetRow>, muscleOf: (String) -> String?): List<Pair<String, Int>> {
    if (sets.isEmpty()) return emptyList()
    val counts = sets.groupingBy { muscleOf(it.exerciseId) ?: "Other" }.eachCount()
    return counts.entries.sortedByDescending { it.value }
        .map { it.key to (it.value * 100.0 / sets.size).roundToInt() }
}
