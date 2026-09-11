package com.getfit.domain

import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Training-pattern analysis — new logic, not from the prototype, built to the same standard as
 * bestFor/streakCount/analyzeTrend: pure, deterministic, unit-tested, no network.
 *
 * Exists so the AI review can be handed *facts* about how someone actually trains (which days
 * they skip, how long their gaps are, whether legs ever get trained) rather than a raw session
 * list it would have to re-derive those from — and might get wrong. The model's job is to explain
 * and advise on the pattern; this file's job is to compute it. Same principle as domain/Trend.kt.
 */

/** One (session date, muscle group) pair — one per set, so a session contributes several. */
data class MuscleSet(val dateMs: Long, val muscle: String)

data class PatternReport(
    val weeksAnalysed: Int,
    val sessionsInWindow: Int,
    val avgSessionsPerWeek: Double,
    /** Oldest -> newest, one entry per Monday-aligned week in the window (partial current week last). */
    val weeklyCounts: List<Int>,
    val skippedWeeks: Int,
    val longestGapDays: Int,
    val daysSinceLast: Int,
    /** Mon..Sun. */
    val dayOfWeekCounts: List<Int>,
    val favouriteDays: List<String>,
    val neverTrainedDays: List<String>,
    /** Mean calendar days between consecutive sessions (1 = every day, 2 = every other day). */
    val avgDaysBetween: Double,
    val backToBackDays: Int,
    val avgDurationSec: Int,
    val avgSetsPerSession: Double,
    val avgMinutesPerSet: Double,
    val timeOfDay: Map<String, Int>,
    /** Oldest -> newest, kg. */
    val weeklyVolume: List<Int>,
    val volumeDirection: VolumeDirection,
    /** Sets per muscle group over the last 4 weeks. */
    val muscleSets: Map<String, Int>,
    val neglectedMuscles: List<String>,
)

enum class VolumeDirection { RISING, FLAT, FALLING, INSUFFICIENT_DATA }

private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** The groups worth flagging as neglected. Cardio and Glutes are deliberately excluded: the former
 *  isn't tracked by sets, the latter is trained implicitly by most Legs work. */
private val MAJOR_GROUPS = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")

/**
 * Analyses the last [weeks] Monday-aligned weeks ending at [now]. Sessions outside that window
 * are ignored; [muscleSets] are only counted for the last 28 days regardless of [weeks], because
 * muscle balance is a "what are you doing lately" question, not a two-month one.
 */
fun analyzePatterns(
    sessions: List<SessionRecord>,
    now: Long,
    weeks: Int = 8,
    muscleSets: List<MuscleSet> = emptyList(),
): PatternReport {
    val thisWeekStart = weekStartLocal(now)
    val windowStart = thisWeekStart - (weeks - 1) * 7L * DAY_MS
    val inWindow = sessions.filter { it.dateMs >= windowStart && it.dateMs <= now }.sortedBy { it.dateMs }

    // One count per week, oldest first. Week index by days-since-windowStart / 7 (weeks are
    // Monday-aligned so this lines up with the calendar week).
    val weeklyCounts = IntArray(weeks)
    val weeklyVolume = IntArray(weeks)
    inWindow.forEach { s ->
        val idx = ((s.dateMs - windowStart) / (7L * DAY_MS)).toInt().coerceIn(0, weeks - 1)
        weeklyCounts[idx]++
        weeklyVolume[idx] += s.volume
    }
    // Skipped = a *completed* week with nothing in it. The current week is still in progress and
    // shouldn't count against anyone on a Tuesday.
    val skippedWeeks = weeklyCounts.dropLast(1).count { it == 0 }

    // Day-level facts. Dedupe to calendar days so two sessions on one day count as one trained day.
    val trainedDays = inWindow.map { floorDayLocal(it.dateMs) }.distinct().sorted()
    val gaps = trainedDays.zipWithNext { a, b -> ((b - a) / DAY_MS).toInt() }
    val longestGap = gaps.maxOrNull() ?: 0
    val avgDaysBetween = if (gaps.isEmpty()) 0.0 else gaps.average()
    val backToBack = gaps.count { it == 1 }
    val daysSinceLast = trainedDays.lastOrNull()?.let { ((floorDayLocal(now) - it) / DAY_MS).toInt() } ?: Int.MAX_VALUE

    val dow = IntArray(7)
    trainedDays.forEach { dow[mondayBasedDay(it)]++ }
    val maxDow = dow.maxOrNull() ?: 0
    val favouriteDays = if (maxDow == 0) emptyList() else DAY_NAMES.filterIndexed { i, _ -> dow[i] == maxDow }
    val neverTrained = if (trainedDays.size < 4) emptyList() else DAY_NAMES.filterIndexed { i, _ -> dow[i] == 0 }

    // Session shape.
    val avgDuration = if (inWindow.isEmpty()) 0 else inWindow.map { it.durationSec }.average().roundToInt()
    val avgSets = if (inWindow.isEmpty()) 0.0 else inWindow.map { it.totalSets }.average()
    val timed = inWindow.filter { it.durationSec > 0 && it.totalSets > 0 }
    val avgMinPerSet = if (timed.isEmpty()) 0.0 else timed.map { it.durationSec / 60.0 / it.totalSets }.average()

    val tod = linkedMapOf("morning" to 0, "afternoon" to 0, "evening" to 0, "night" to 0)
    inWindow.forEach { s -> val k = timeOfDayBucket(s.dateMs); tod[k] = (tod[k] ?: 0) + 1 }

    // Volume direction: last two completed weeks vs the two before them. Needs all four to have
    // data, otherwise a rest week reads as a collapse.
    val completed = weeklyVolume.dropLast(1)
    val direction = if (completed.size < 4 || completed.takeLast(4).any { it == 0 }) {
        VolumeDirection.INSUFFICIENT_DATA
    } else {
        val recent = completed.takeLast(2).average()
        val prior = completed.dropLast(2).takeLast(2).average()
        val change = (recent - prior) / prior
        when {
            change >= 0.10 -> VolumeDirection.RISING
            change <= -0.10 -> VolumeDirection.FALLING
            else -> VolumeDirection.FLAT
        }
    }

    // Muscle balance over the last 28 days.
    val fourWeeksAgo = now - 28L * DAY_MS
    val recentMuscle = muscleSets.filter { it.dateMs >= fourWeeksAgo && it.dateMs <= now }
    val muscleCounts = recentMuscle.groupingBy { it.muscle }.eachCount().toSortedMap()
    val totalSets = muscleCounts.values.sum()
    val neglected = if (totalSets < 20) emptyList() else MAJOR_GROUPS.filter { g ->
        (muscleCounts[g] ?: 0).toDouble() / totalSets < 0.08
    }

    return PatternReport(
        weeksAnalysed = weeks,
        sessionsInWindow = inWindow.size,
        avgSessionsPerWeek = inWindow.size.toDouble() / weeks,
        weeklyCounts = weeklyCounts.toList(),
        skippedWeeks = skippedWeeks,
        longestGapDays = longestGap,
        daysSinceLast = daysSinceLast,
        dayOfWeekCounts = dow.toList(),
        favouriteDays = favouriteDays,
        neverTrainedDays = neverTrained,
        avgDaysBetween = avgDaysBetween,
        backToBackDays = backToBack,
        avgDurationSec = avgDuration,
        avgSetsPerSession = avgSets,
        avgMinutesPerSet = avgMinPerSet,
        timeOfDay = tod,
        weeklyVolume = weeklyVolume.toList(),
        volumeDirection = direction,
        muscleSets = muscleCounts,
        neglectedMuscles = neglected,
    )
}

/**
 * The report as plain-language facts for a prompt. Every line is something the model should treat
 * as given, not recompute. Kept terse: this rides along with the whole workout summary.
 */
fun describePatterns(r: PatternReport, units: String = "kg"): String {
    if (r.sessionsInWindow == 0) return "No sessions in the last ${r.weeksAnalysed} weeks."
    val sb = StringBuilder()
    sb.appendLine(
        "Over the last ${r.weeksAnalysed} weeks: ${r.sessionsInWindow} sessions, " +
            "${fmt1(r.avgSessionsPerWeek)} per week on average (weekly goal is 5). " +
            "Sessions per week, oldest to newest: ${r.weeklyCounts.joinToString(", ")}" +
            (if (r.skippedWeeks > 0) " — ${r.skippedWeeks} completed week${plural(r.skippedWeeks)} with no training at all." else "."),
    )
    sb.appendLine(
        "Longest gap between sessions: ${r.longestGapDays} day${plural(r.longestGapDays)}. " +
            "Typical spacing: every ${fmt1(r.avgDaysBetween)} days" +
            (if (r.backToBackDays > 0) ", with ${r.backToBackDays} back-to-back day${plural(r.backToBackDays)}." else ".") +
            (if (r.daysSinceLast in 1..Int.MAX_VALUE - 1) " Last trained ${r.daysSinceLast} day${plural(r.daysSinceLast)} ago." else ""),
    )
    if (r.favouriteDays.isNotEmpty()) {
        sb.appendLine(
            "Most-trained day${plural(r.favouriteDays.size)}: ${r.favouriteDays.joinToString("/")}." +
                (if (r.neverTrainedDays.isNotEmpty()) " Never trains on: ${r.neverTrainedDays.joinToString(", ")}." else ""),
        )
    }
    val busiest = r.timeOfDay.maxByOrNull { it.value }
    if (busiest != null && busiest.value > 0) {
        sb.appendLine("Usually trains in the ${busiest.key} (${busiest.value} of ${r.sessionsInWindow} sessions).")
    }
    sb.appendLine(
        "Average session: ${fmtDur(r.avgDurationSec)}, ${fmt1(r.avgSetsPerSession)} sets" +
            (if (r.avgMinutesPerSet > 0) " (~${fmt1(r.avgMinutesPerSet)} min per set including rest)." else "."),
    )
    val volDesc = when (r.volumeDirection) {
        VolumeDirection.RISING -> "rising"
        VolumeDirection.FALLING -> "falling"
        VolumeDirection.FLAT -> "flat"
        VolumeDirection.INSUFFICIENT_DATA -> "not enough consecutive weeks to call a direction"
    }
    sb.appendLine(
        "Weekly volume ($units), oldest to newest: ${r.weeklyVolume.joinToString(", ")} — $volDesc.",
    )
    if (r.muscleSets.isNotEmpty()) {
        val total = r.muscleSets.values.sum()
        val breakdown = r.muscleSets.entries.sortedByDescending { it.value }
            .joinToString(", ") { "${it.key} ${it.value} (${(100.0 * it.value / total).roundToInt()}%)" }
        sb.appendLine("Sets by muscle group, last 4 weeks: $breakdown.")
        if (r.neglectedMuscles.isNotEmpty()) {
            sb.appendLine("Neglected (under 8% of sets): ${r.neglectedMuscles.joinToString(", ")}.")
        }
    }
    return sb.toString().trimEnd()
}

// --- helpers ---

/** Mon=0 .. Sun=6 for a local timestamp. */
private fun mondayBasedDay(ts: Long): Int {
    val c = Calendar.getInstance()
    c.timeInMillis = ts
    return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
}

private fun timeOfDayBucket(ts: Long): String {
    val c = Calendar.getInstance()
    c.timeInMillis = ts
    return when (c.get(Calendar.HOUR_OF_DAY)) {
        in 5..10 -> "morning"
        in 11..16 -> "afternoon"
        in 17..21 -> "evening"
        else -> "night"
    }
}

private fun fmt1(v: Double): String =
    if (abs(v - v.roundToInt()) < 1e-9) v.roundToInt().toString() else String.format(Locale.US, "%.1f", v)

private fun plural(n: Int): String = if (n == 1) "" else "s"
