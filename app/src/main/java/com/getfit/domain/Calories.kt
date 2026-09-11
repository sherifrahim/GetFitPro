package com.getfit.domain

import kotlin.math.roundToInt

/** Body-weight fallback when the user hasn't entered one — a middle-of-the-road adult. */
const val DEFAULT_BODY_WEIGHT_KG = 75.0
const val DEFAULT_AGE_YEARS = 30

/**
 * Energy estimate for a finished session, the way Hevy shows "Calories" on a workout.
 *
 * With a heart-rate average (from the watch) this is the Keytel et al. (2005) regression, which is
 * what most wearables use for steady-state work; without one it falls back to a MET table entry
 * (resistance training, vigorous ≈ 6.0 MET). Both are estimates with a wide error band — this is a
 * motivational number, not a nutrition one, and the UI should never present it as more than that.
 *
 * [male] = null (unset in profile) averages the two Keytel equations rather than guessing.
 */
fun estimateCalories(
    durationSec: Int,
    avgBpm: Int,
    weightKg: Double = DEFAULT_BODY_WEIGHT_KG,
    ageYears: Int = DEFAULT_AGE_YEARS,
    male: Boolean? = null,
): Int {
    if (durationSec <= 0) return 0
    val minutes = durationSec / 60.0
    val w = if (weightKg > 0) weightKg else DEFAULT_BODY_WEIGHT_KG
    val a = if (ageYears > 0) ageYears else DEFAULT_AGE_YEARS
    val perMin = if (avgBpm >= 60) {
        val m = (-55.0969 + 0.6309 * avgBpm + 0.1988 * w + 0.2017 * a) / 4.184
        val f = (-20.4022 + 0.4472 * avgBpm - 0.1263 * w + 0.074 * a) / 4.184
        when (male) { true -> m; false -> f; null -> (m + f) / 2 }
    } else {
        // MET × 3.5 × kg / 200 = kcal per minute.
        6.0 * 3.5 * w / 200.0
    }
    return (perMin.coerceAtLeast(0.0) * minutes).roundToInt()
}

/** Average of a heart-rate trace, 0 when empty. */
fun avgBpm(points: List<HrPoint>): Int =
    if (points.isEmpty()) 0 else (points.sumOf { it.bpm }.toDouble() / points.size).roundToInt()

fun maxBpm(points: List<HrPoint>): Int = points.maxOfOrNull { it.bpm } ?: 0
