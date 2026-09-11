package com.getfit.domain

import kotlin.math.roundToInt

/**
 * Bodyweight test (proto isBW, L765): bodyweight equipment OR a time-based rep string ("45s").
 * Such exercises track reps, never weight.
 */
fun isBW(equipment: String, reps: String): Boolean =
    equipment == "Bodyweight" || reps.endsWith("s")

/**
 * Personal best (proto bestFor, L769-774).
 * Weighted: max weight, tiebreak on reps; estimated 1RM = round(w * (1 + reps/30)).
 * Bodyweight: max reps (tiebreak most recent), no weight/1RM.
 */
/** Estimated one-rep max, the prototype's formula: round(w * (1 + reps/30)). */
fun e1rm(weight: Double, reps: Int): Int = (weight * (1 + reps / 30.0)).roundToInt()

fun bestFor(sets: List<LoggedSet>, bodyweight: Boolean): Best? {
    if (sets.isEmpty()) return null
    return if (bodyweight) {
        var b = sets[0]
        for (x in sets) if (x.reps > b.reps || (x.reps == b.reps && x.dateMs > b.dateMs)) b = x
        Best(true, 0.0, b.reps, 0, b.dateMs)
    } else {
        var b = sets[0]
        for (x in sets) if (x.weight > b.weight || (x.weight == b.weight && x.reps > b.reps)) b = x
        Best(false, b.weight, b.reps, (b.weight * (1 + b.reps / 30.0)).roundToInt(), b.dateMs)
    }
}

/** A moment the best for an exercise moved: what it became and when. */
data class PrEvent(val exerciseId: String, val dateMs: Long, val weight: Double, val reps: Int, val e1rm: Int, val bodyweight: Boolean)

/**
 * Every personal record ever set, newest first — the browsable history behind the moment-of-PR
 * toast. Walks each exercise's log in date order and emits an event each time a set beats the
 * running best by the same rule [bestFor] uses (weight, tiebreak reps; reps for bodyweight). The
 * very first set of an exercise is not a "record" — there was nothing to beat.
 */
fun prHistory(logs: List<LoggedSet>, isBodyweight: (String) -> Boolean): List<PrEvent> {
    val out = mutableListOf<PrEvent>()
    logs.groupBy { it.exerciseId }.forEach { (exId, sets) ->
        val bw = isBodyweight(exId)
        var best: LoggedSet? = null
        // Stable on date so two sets logged in the same second keep insertion order.
        for (s in sets.sortedBy { it.dateMs }) {
            val b = best
            val beats = when {
                b == null -> false
                bw -> s.reps > b.reps
                else -> s.weight > b.weight || (s.weight == b.weight && s.reps > b.reps)
            }
            if (beats) out += PrEvent(exId, s.dateMs, s.weight, s.reps, if (bw) 0 else e1rm(s.weight, s.reps), bw)
            if (b == null || beats) best = s
        }
    }
    return out.sortedByDescending { it.dateMs }
}
