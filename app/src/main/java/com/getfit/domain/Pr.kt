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
