package com.getfit.domain

/**
 * Plate calculator + warm-up ramp: the two "every set" helpers Hevy and Strong put next to the
 * weight field. Both are pure and unit-aware only through the plate set they are given.
 */

/** Standard bar and plate sets per unit. Plates are what gyms actually stock, largest first. */
object Plates {
    const val BAR_KG = 20.0
    const val BAR_LB = 45.0
    val KG = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    val LB = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    fun barFor(units: String): Double = if (units == "lb") BAR_LB else BAR_KG
    fun setFor(units: String): List<Double> = if (units == "lb") LB else KG
}

data class PlateLoad(
    /** Plates on ONE side, largest first, e.g. [20, 5, 2.5]. */
    val perSide: List<Double>,
    /** Target minus what the bar + chosen plates actually make, in the same unit. 0 = exact. */
    val remainder: Double,
) {
    val exact: Boolean get() = remainder < 1e-9
}

/**
 * Greedy per-side loading for a target total weight on a bar. Greedy is correct here because every
 * standard plate set is "canonical" (each plate is at least twice the next smaller one, or the sum of
 * smaller ones can't beat it), so largest-first always minimises the plate count.
 *
 * Returns null when the target is below the bar — there is nothing to load.
 */
fun platesFor(targetTotal: Double, bar: Double, plates: List<Double>): PlateLoad? {
    if (targetTotal < bar - 1e-9) return null
    var perSideLeft = (targetTotal - bar) / 2.0
    val chosen = mutableListOf<Double>()
    for (p in plates.sortedDescending()) {
        while (perSideLeft + 1e-9 >= p) { chosen += p; perSideLeft -= p }
    }
    return PlateLoad(chosen, (perSideLeft * 2.0).coerceAtLeast(0.0).let { Math.round(it * 1000) / 1000.0 })
}

/** One warm-up set: a fraction of the working weight, rounded to something loadable, for [reps]. */
data class WarmupSet(val weight: Double, val reps: Int, val pct: Int)

/**
 * Strong-style ramp to a working weight: 50% × 8, 70% × 5, 90% × 2, each rounded down to the given
 * increment (the smallest plate pair: 2.5 kg / 5 lb) and never below the bar. Light working weights
 * get a shorter ramp (there's no point warming up to an empty bar with an empty bar), and anything
 * at or under the bar gets none.
 */
fun warmupRamp(workingWeight: Double, bar: Double, increment: Double): List<WarmupSet> {
    if (workingWeight <= bar + 1e-9) return emptyList()
    val steps = listOf(Triple(50, 8, 0.5), Triple(70, 5, 0.7), Triple(90, 2, 0.9))
    val out = mutableListOf<WarmupSet>()
    for ((pct, reps, f) in steps) {
        val raw = workingWeight * f
        val rounded = Math.floor(raw / increment) * increment
        val w = rounded.coerceAtLeast(bar)
        // Skip a step that collapsed onto the bar or onto the previous step — no information in it.
        if (w <= bar + 1e-9 || w >= workingWeight - 1e-9) continue
        if (out.isNotEmpty() && Math.abs(out.last().weight - w) < 1e-9) continue
        out += WarmupSet(Math.round(w * 100) / 100.0, reps, pct)
    }
    return out
}
