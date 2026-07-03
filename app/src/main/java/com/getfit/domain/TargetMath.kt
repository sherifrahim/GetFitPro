package com.getfit.domain

import kotlin.math.ceil

/** Target progress fraction, clamped to 0..1 (proto L951). */
fun targetPct(cur: Double, start: Double, target: Double): Double {
    val denom = (target - start).let { if (it == 0.0) 1.0 else it }
    return ((cur - start) / denom).coerceIn(0.0, 1.0)
}

/** Days remaining until a target deadline (proto L951, daysLeft). */
fun targetDaysLeft(startDMs: Long, weeks: Int, now: Long): Int =
    ceil((startDMs + weeks * 7L * DAY_MS - now).toDouble() / DAY_MS).toInt()
