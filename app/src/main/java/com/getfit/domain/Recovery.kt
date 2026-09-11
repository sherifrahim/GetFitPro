package com.getfit.domain

/**
 * Fitbod-style "what should I train next" from nothing but the user's own log: how long since
 * each muscle group was last worked, and how much it took in the last week.
 *
 * The model is deliberately simple and explainable — it exists to nudge the routine rotation and
 * to give the AI coach a fact to reason from, not to be a physiology engine:
 *   readiness = clamp(hoursSinceTrained / 60h, 0..1) × (1 − 0.2 × min(1, setsLast7d / 25))
 * i.e. a muscle is "ready" ~2.5 days after it was hit, a little later if the week was heavy, and a
 * muscle never trained is fully ready.
 */
data class MuscleRecovery(
    val muscle: String,
    /** null = never trained. */
    val lastTrainedMs: Long?,
    val setsLast7d: Int,
    /** 0 = just trained, 1 = fully recovered. */
    val readiness: Double,
) {
    fun daysSince(now: Long): Int? = lastTrainedMs?.let { ((now - it) / DAY_MS).toInt() }
}

data class RoutineSuggestion(val routineId: String, val score: Double, val reason: String)

private const val RECOVERY_HOURS = 60.0

fun recoveryByMuscle(
    sessionDates: Map<String, Long>,           // sessionId -> dateMs
    sets: List<Pair<String, String>>,          // (sessionId, exerciseId)
    muscleOf: (String) -> String?,
    now: Long,
    muscles: List<String> = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Glutes", "Cardio"),
): List<MuscleRecovery> {
    val last = HashMap<String, Long>()
    val week = HashMap<String, Int>()
    for ((sid, exId) in sets) {
        val date = sessionDates[sid] ?: continue
        val m = muscleOf(exId) ?: continue
        if ((last[m] ?: Long.MIN_VALUE) < date) last[m] = date
        if (now - date <= 7 * DAY_MS) week[m] = (week[m] ?: 0) + 1
    }
    return muscles.map { m ->
        val l = last[m]
        val hours = l?.let { (now - it) / 3_600_000.0 }
        val base = if (hours == null) 1.0 else (hours / RECOVERY_HOURS).coerceIn(0.0, 1.0)
        val fatigue = 1.0 - 0.2 * ((week[m] ?: 0) / 25.0).coerceIn(0.0, 1.0)
        MuscleRecovery(m, l, week[m] ?: 0, (base * fatigue).coerceIn(0.0, 1.0))
    }
}

/**
 * Scores each routine by the mean readiness of the muscles it trains (weighted by how many of its
 * exercises hit each) and returns the best, with a one-line reason. Null when there is nothing to
 * choose between (no routines, or none with exercises).
 */
fun suggestRoutine(
    routines: List<Pair<String, List<String>>>,   // (routineId, exerciseIds)
    recovery: List<MuscleRecovery>,
    muscleOf: (String) -> String?,
    now: Long,
): RoutineSuggestion? {
    val readiness = recovery.associate { it.muscle to it }
    var best: RoutineSuggestion? = null
    for ((id, exIds) in routines) {
        val ms = exIds.mapNotNull(muscleOf)
        if (ms.isEmpty()) continue
        val score = ms.map { readiness[it]?.readiness ?: 1.0 }.average()
        if (best == null || score > best.score + 1e-9) {
            // Reason: the most-worked muscle of this routine and when it was last hit.
            val top = ms.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key
            val days = readiness[top]?.daysSince(now)
            val reason = when {
                days == null -> "$top hasn't been trained yet"
                days == 0 -> "$top was trained today"
                days == 1 -> "$top was last trained yesterday"
                else -> "$top was last trained $days days ago"
            }
            best = RoutineSuggestion(id, score, reason)
        }
    }
    return best
}
