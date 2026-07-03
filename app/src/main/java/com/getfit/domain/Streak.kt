package com.getfit.domain

const val DAY_MS = 86_400_000L

/** UTC-day floor. Deterministic for streak counting; see WeekAgg for local-day handling. */
fun floorDayUtc(ts: Long): Long = ts - (ts % DAY_MS)

/**
 * Consecutive calendar days (back from today) with at least one session (proto streakCount, L780).
 * Today is optional: if there's no session today, counting starts from yesterday.
 *
 * [dayFloor] controls the day boundary. Tests use the UTC default for determinism; the app passes
 * [floorDayLocal] so streaks respect the device's local midnight (consistent with weekly aggregation).
 */
fun streakCount(sessionDates: List<Long>, now: Long, dayFloor: (Long) -> Long = ::floorDayUtc): Int {
    if (sessionDates.isEmpty()) return 0
    val days = sessionDates.map { dayFloor(it) }.toHashSet()
    var s = 0
    var d = dayFloor(now)
    if (!days.contains(d)) d -= DAY_MS
    while (days.contains(d)) { s++; d -= DAY_MS }
    return s
}
