package com.getfit.data.ai

import com.getfit.data.db.ExerciseEntity
import com.getfit.data.db.SessionEntity
import com.getfit.data.db.SetLogEntity
import com.getfit.data.db.TargetEntity
import com.getfit.domain.Best
import com.getfit.domain.LoggedSet
import com.getfit.domain.SessionRecord
import com.getfit.domain.TrendVerdict
import com.getfit.domain.Units
import com.getfit.domain.analyzeTrend
import com.getfit.domain.fmtDur
import com.getfit.domain.fmtVol
import com.getfit.domain.fmtW
import com.getfit.domain.floorDayLocal
import com.getfit.domain.isBW
import com.getfit.domain.streakCount
import com.getfit.domain.weekAgg
import com.getfit.domain.weekStartLocal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns the app's logged workout data into a compact plain-text brief for the AI review prompt.
 * Lives in the data layer (not domain/) since it reaches into Room entity types directly; the
 * aggregation math it leans on — weekAgg, streakCount — IS the tested pure domain logic, just
 * fed real entities here instead of test fixtures.
 */
fun buildWorkoutSummary(
    sessions: List<SessionEntity>,
    targets: List<TargetEntity>,
    bestMap: Map<String, Best>,
    exercises: List<ExerciseEntity>,
    units: String,
    logs: List<SetLogEntity> = emptyList(),
): String {
    if (sessions.isEmpty()) return "No workouts logged yet."

    val byId = exercises.associateBy { it.id }
    val now = System.currentTimeMillis()
    val df = SimpleDateFormat("MMM d", Locale.US)
    val sorted = sessions.sortedByDescending { it.dateMs }
    val streak = streakCount(sessions.map { it.dateMs }, now, ::floorDayLocal)
    val thisWeek = weekAgg(
        sessions.map { SessionRecord(it.id, it.dateMs, it.name, it.durationSec, it.totalSets, it.volume, it.prs) },
        weekStartLocal(now),
    )

    val sb = StringBuilder()
    sb.appendLine("Workout log summary (most recent first). ${sessions.size} sessions logged total, current streak $streak day${if (streak == 1) "" else "s"}.")
    sb.appendLine(
        "This week so far: ${thisWeek.workouts} workout${if (thisWeek.workouts == 1) "" else "s"}, " +
            "${fmtDur(thisWeek.durationSec)} total time, ${fmtVol(Units.volDisplay(thisWeek.totalVolume, units))} $units volume.",
    )
    sb.appendLine()

    sb.appendLine("Last ${minOf(10, sorted.size)} sessions:")
    sorted.take(10).forEach { s ->
        val prNote = if (s.prs > 0) ", ${s.prs} PR${if (s.prs > 1) "s" else ""}" else ""
        sb.appendLine(
            "- ${df.format(Date(s.dateMs))}: ${s.name}, ${s.totalSets} sets, ${fmtDur(s.durationSec)}, " +
                "${fmtVol(Units.volDisplay(s.volume, units))} $units volume$prNote.",
        )
    }

    if (bestMap.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("Current bests:")
        bestMap.entries.sortedBy { byId[it.key]?.name ?: it.key }.take(15).forEach { (id, best) ->
            val name = byId[id]?.name ?: id
            val value = if (best.bodyweight) "${best.reps} reps" else "${Units.fmtDisplay(best.weight, units)} $units x ${best.reps}"
            sb.appendLine("- $name: $value")
        }
    }

    if (logs.isNotEmpty()) {
        // Pre-computed on-device trend verdicts (see domain/Trend.kt) fed in as plain-language facts
        // rather than raw numbers, so the model reasons from the same classification the app itself
        // shows on the Detail screen instead of re-deriving (and possibly disagreeing with) it.
        val logsByEx = logs.groupBy { it.exerciseId }
        val trendLines = logsByEx.entries.mapNotNull { (id, exLogs) ->
            val ex = byId[id] ?: return@mapNotNull null
            val bw = isBW(ex.equipment, ex.reps)
            val loggedSets = exLogs.map { LoggedSet(it.exerciseId, it.weight, it.reps, it.dateMs) }
            val trend = analyzeTrend(loggedSets, bw)
            if (trend.verdict == TrendVerdict.INSUFFICIENT_DATA) null else ex.name to trend
        }
        if (trendLines.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Trend classification (pre-computed, not for you to recompute):")
            trendLines.sortedBy { it.first }.forEach { (name, trend) ->
                sb.appendLine("- $name: ${trend.verdict.name.lowercase()} — ${trend.message}")
            }
        }
    }

    if (targets.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("Active targets:")
        targets.forEach { t ->
            val ex = byId[t.exId]
            val name = ex?.name ?: t.exId
            val bw = ex?.let { isBW(it.equipment, it.reps) } ?: false
            val fmt: (Double) -> String = { v -> if (bw) "${v.toInt()} reps" else "${fmtW(Units.toDisplay(v, units))} $units" }
            sb.appendLine("- $name: currently ${fmt(t.start)} -> goal ${fmt(t.target)}, within ${t.weeks} weeks.")
        }
    }

    return sb.toString()
}
