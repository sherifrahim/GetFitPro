package com.getfit.domain

const val DAY_MS = 86_400_000L

/** UTC-day floor. Deterministic for streak counting; see WeekAgg for local-day handling. */
fun floorDayUtc(ts: Long): Long = ts - (ts % DAY_MS)

/**
 * Consecutive calendar days (back from today) with at least one session (proto streakCount, L780).
 * Today is optional: if there's no session today, counting starts from yesterday.
 */
fun streakCount(sessionDates: List<Long>, now: Long): Int {
    if (sessionDates.isEmpty()) return 0
    val days = sessionDates.map { floorDayUtc(it) }.toHashSet()
    var s = 0
    var d = floorDayUtc(now)
    if (!days.contains(d)) d -= DAY_MS
    while (days.contains(d)) { s++; d -= DAY_MS }
    return s
}
