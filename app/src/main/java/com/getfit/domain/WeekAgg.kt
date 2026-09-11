package com.getfit.domain

import java.util.Calendar

data class WeekResult(
    val dayVolume: IntArray,
    val dayHit: BooleanArray,
    val workouts: Int,
    val durationSec: Int,
    val totalVolume: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WeekResult) return false
        return dayVolume.contentEquals(other.dayVolume) && dayHit.contentEquals(other.dayHit) &&
            workouts == other.workouts && durationSec == other.durationSec && totalVolume == other.totalVolume
    }
    override fun hashCode(): Int {
        var r = dayVolume.contentHashCode()
        r = 31 * r + dayHit.contentHashCode(); r = 31 * r + workouts
        r = 31 * r + durationSec; r = 31 * r + totalVolume
        return r
    }
}

/**
 * Bucket sessions into the 7 days of the current week from weekStartMs (proto L921-925).
 * Index 0 = the day at weekStartMs. Only sessions on/after weekStartMs count.
 */
fun weekAgg(sessions: List<SessionRecord>, weekStartMs: Long): WeekResult {
    val vol = IntArray(7); val hit = BooleanArray(7)
    var workouts = 0; var dur = 0; var total = 0
    for (h in sessions) if (h.dateMs >= weekStartMs) {
        val i = ((h.dateMs - weekStartMs) / DAY_MS).toInt()
        if (i in 0..6) { vol[i] += h.volume; hit[i] = true }
        workouts++; dur += h.durationSec; total += h.volume
    }
    return WeekResult(vol, hit, workouts, dur, total)
}

/** Monday-00:00 (local) of the current week (proto weekStart, L778). [mondayStart] = false gives
 *  the Sunday-start week Hevy offers as a preference; the default keeps the prototype's maths. */
fun weekStartLocal(now: Long, mondayStart: Boolean = true): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = now
    // Calendar: Sunday=1..Saturday=7 -> Monday-based offset (Mon=0..Sun=6), or Sunday-based (Sun=0..Sat=6)
    val mondayBased = if (mondayStart) (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 else c.get(Calendar.DAY_OF_WEEK) - 1
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    c.add(Calendar.DAY_OF_MONTH, -mondayBased)
    return c.timeInMillis
}

/** Local-day floor for real timestamps (matches prototype's setHours(0,0,0,0)). */
fun floorDayLocal(ts: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ts
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

/** Index of today within the week strip (0 = the first day of the week), matching [weekStartLocal]. */
fun todayWeekIndex(mondayStart: Boolean = true, c: Calendar = Calendar.getInstance()): Int =
    if (mondayStart) (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 else c.get(Calendar.DAY_OF_WEEK) - 1

/** Day letters for the week strip, in the order the week is shown. */
fun weekDayLetters(mondayStart: Boolean = true): List<String> =
    if (mondayStart) listOf("M", "T", "W", "T", "F", "S", "S") else listOf("S", "M", "T", "W", "T", "F", "S")
