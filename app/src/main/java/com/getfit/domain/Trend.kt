package com.getfit.domain

/**
 * Trend classification for one exercise's recent progression — new logic, not from the prototype,
 * but built to the same standard as bestFor/streakCount: pure, deterministic, unit-tested, no
 * network. Deliberately coarse thresholds (±5%) rather than reacting to single-session noise; a
 * heavier or lighter set on one day is normal variance, not a trend.
 */
enum class TrendVerdict { IMPROVING, PLATEAUED, DECLINING, INSUFFICIENT_DATA }

data class TrendResult(val verdict: TrendVerdict, val message: String, val changePct: Double?)

/**
 * Buckets logged sets to one point per calendar day (the day's best set, same ordering rule as
 * [bestFor] — max weight tiebroken by reps for weighted lifts, max reps for bodyweight), takes the
 * most recent [maxPoints] days that actually have data, and compares the earliest to the latest in
 * that window. Needs at least 3 data points to call a trend; fewer than that is just noise.
 */
fun analyzeTrend(logs: List<LoggedSet>, bodyweight: Boolean, maxPoints: Int = 6): TrendResult {
    val byDay = logs.groupBy { floorDayLocal(it.dateMs) }
    val points = byDay.entries.sortedBy { it.key }.takeLast(maxPoints).map { (_, sets) ->
        if (bodyweight) {
            sets.maxOf { it.reps }.toDouble()
        } else {
            sets.reduce { a, b -> if (b.weight > a.weight || (b.weight == a.weight && b.reps > a.reps)) b else a }.weight
        }
    }
    if (points.size < 3) return TrendResult(TrendVerdict.INSUFFICIENT_DATA, "Log a few more sessions to see a trend.", null)

    val first = points.first()
    val last = points.last()
    if (first <= 0.0) return TrendResult(TrendVerdict.INSUFFICIENT_DATA, "Not enough data yet.", null)
    val changePct = (last - first) / first * 100.0
    val sessions = points.size

    return when {
        changePct >= 5.0 -> TrendResult(
            TrendVerdict.IMPROVING,
            "Up ${pct(changePct)} over the last $sessions sessions — solid progress, keep it up.",
            changePct,
        )
        changePct <= -5.0 -> TrendResult(
            TrendVerdict.DECLINING,
            "Down ${pct(-changePct)} over the last $sessions sessions — could be fatigue or recovery. Worth a lighter deload.",
            changePct,
        )
        else -> {
            val addWhat = if (bodyweight) "a rep" else "weight or a rep"
            TrendResult(
                TrendVerdict.PLATEAUED,
                "Holding steady over the last $sessions sessions. Try adding $addWhat to push past the plateau.",
                changePct,
            )
        }
    }
}

private fun pct(v: Double): String = "${Math.round(v)}%"

/** One point on an exercise's progress chart: the day's top set (weight, or reps for bodyweight). */
data class ProgressPoint(val dayMs: Long, val value: Double, val e1rm: Int)

/**
 * The per-exercise progress series a chart draws — the same "top set per day" reduction
 * [analyzeTrend] classifies, kept whole (up to [maxPoints], oldest first) instead of just its ends.
 */
fun progressSeries(logs: List<LoggedSet>, bodyweight: Boolean, maxPoints: Int = 30): List<ProgressPoint> =
    logs.groupBy { floorDayLocal(it.dateMs) }.entries.sortedBy { it.key }.takeLast(maxPoints).map { (day, sets) ->
        if (bodyweight) {
            ProgressPoint(day, sets.maxOf { it.reps }.toDouble(), 0)
        } else {
            val top = sets.reduce { a, b -> if (b.weight > a.weight || (b.weight == a.weight && b.reps > a.reps)) b else a }
            ProgressPoint(day, top.weight, e1rm(top.weight, top.reps))
        }
    }
