package com.getfit.domain

/**
 * Strava-style period recap, computed from data the app already has. Every field is derived: no
 * new tracking, so any period (month, year, all time) is just a filter on the same history.
 */
data class RecapStats(
    val workouts: Int,
    val durationSec: Int,
    val volumeKg: Double,
    val sets: Int,
    val prs: Int,
    /** Longest run of consecutive training days inside the period. */
    val bestStreak: Int,
    /** Distinct days trained. */
    val activeDays: Int,
    val topMuscle: String?,
    val topMuscleSets: Int,
    val topExercise: String?,
    val topExerciseSets: Int,
    val avgPerWeek: Double,
    val calories: Int,
)

data class RecapSession(val id: String, val dateMs: Long, val durationSec: Int, val volume: Int, val calories: Int)

fun recap(
    sessions: List<RecapSession>,
    sets: List<Pair<String, String>>,          // (sessionId, exerciseId)
    logs: List<LoggedSet>,
    fromMs: Long,
    toMs: Long,
    muscleOf: (String) -> String?,
    nameOf: (String) -> String?,
    isBodyweight: (String) -> Boolean,
    floorDay: (Long) -> Long,
): RecapStats {
    val inRange = sessions.filter { it.dateMs in fromMs..toMs }
    val ids = inRange.map { it.id }.toSet()
    val rangeSets = sets.filter { it.first in ids }

    // Streak: longest run of consecutive calendar days with a session.
    val days = inRange.map { floorDay(it.dateMs) }.distinct().sorted()
    var best = 0; var run = 0; var prev = Long.MIN_VALUE
    for (d in days) {
        run = if (prev != Long.MIN_VALUE && d - prev == DAY_MS) run + 1 else 1
        best = maxOf(best, run); prev = d
    }

    val muscleCounts = rangeSets.mapNotNull { muscleOf(it.second) }.groupingBy { it }.eachCount()
    val exCounts = rangeSets.groupingBy { it.second }.eachCount()
    val topMuscle = muscleCounts.maxByOrNull { it.value }
    val topEx = exCounts.maxByOrNull { it.value }

    // PRs are counted from the log, the same way PR history is, so imports count too.
    val prs = prHistory(logs.filter { it.dateMs in fromMs..toMs || it.dateMs < fromMs }, isBodyweight)
        .count { it.dateMs in fromMs..toMs }

    val weeks = ((toMs - fromMs).coerceAtLeast(DAY_MS) / (7.0 * DAY_MS)).coerceAtLeast(1.0 / 7)
    return RecapStats(
        workouts = inRange.size,
        durationSec = inRange.sumOf { it.durationSec },
        volumeKg = inRange.sumOf { it.volume.toDouble() },
        sets = rangeSets.size,
        prs = prs,
        bestStreak = best,
        activeDays = days.size,
        topMuscle = topMuscle?.key,
        topMuscleSets = topMuscle?.value ?: 0,
        topExercise = topEx?.let { nameOf(it.key) ?: it.key },
        topExerciseSets = topEx?.value ?: 0,
        avgPerWeek = if (inRange.isEmpty()) 0.0 else inRange.size / weeks,
        calories = inRange.sumOf { it.calories },
    )
}
