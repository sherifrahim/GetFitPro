package com.getfit.data.importexport

import com.getfit.data.db.ExerciseDao
import com.getfit.data.db.ExerciseEntity
import com.getfit.data.db.LogDao
import com.getfit.data.db.SessionDao
import com.getfit.data.db.SessionEntity
import com.getfit.data.db.SessionSetEntity
import com.getfit.data.db.SetLogEntity
import com.getfit.data.db.defaultReps
import com.getfit.data.db.titleCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportSummary(
    val format: SourceFormat,
    val sessionsImported: Int,
    val setsImported: Int,
    val exercisesCreated: Int,
    val skippedRows: Int,
)

sealed class ImportOutcome {
    data class Success(val summary: ImportSummary) : ImportOutcome()
    data class Failure(val message: String) : ImportOutcome()
}

private fun normName(s: String): String = s.trim().lowercase().replace(Regex("\\s+"), " ")

/** Import from / export to other workout apps' CSV exports. Writes go straight through the DAOs
 *  (bypassing WorkoutRepo.saveSession, which stamps "now" — imported data carries its own historical
 *  dates and needs ids that won't collide within one batch). */
class ImportExportRepo(
    private val exerciseDao: ExerciseDao,
    private val logDao: LogDao,
    private val sessionDao: SessionDao,
) {

    suspend fun importCsv(csvText: String, existingExercises: List<ExerciseEntity>): ImportOutcome {
        val parsed = parseImportCsv(csvText)
        if (parsed.rows.isEmpty()) {
            return ImportOutcome.Failure(
                "Couldn't find recognizable workout data in that file — expected columns like " +
                    "\"Exercise\", \"Weight\" and \"Reps\" (Hevy, Strong, FitNotes and Forge exports all work).",
            )
        }

        val byNormName = existingExercises.associateBy { normName(it.name) }.toMutableMap()
        val newExercises = mutableListOf<ExerciseEntity>()
        val everWeighted = parsed.rows.groupBy { normName(it.exerciseName) }
            .mapValues { (_, v) -> v.any { it.weightKg > 0.0 } }

        fun resolveExercise(rawName: String): ExerciseEntity {
            val key = normName(rawName)
            byNormName[key]?.let { return it }
            byNormName.entries.firstOrNull { (k, _) -> k.length > 3 && (k.startsWith(key) || key.startsWith(k)) }
                ?.let { return it.value }

            val clean = titleCase(rawName.trim())
            val equip = if (everWeighted[key] == true) "Barbell" else "Bodyweight"
            val slug = key.replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "exercise" }
            val entity = ExerciseEntity(
                id = "imp_$slug", name = clean, muscle = "Core", equipment = equip,
                level = "Intermediate", target = "Core", secondaryMuscles = "",
                reps = defaultReps("Core", equip), mediaId = null, cues = "", curated = false,
            )
            byNormName[key] = entity
            newExercises.add(entity)
            return entity
        }

        val grouped = parsed.rows.groupBy { it.sessionKey }.entries.sortedBy { it.value.first().dateMs }
        val sessions = mutableListOf<SessionEntity>()
        val sessionSets = mutableListOf<SessionSetEntity>()
        val logs = mutableListOf<SetLogEntity>()
        var setsImported = 0

        grouped.forEachIndexed { idx, entry ->
            val setRows = entry.value
            val dateMs = setRows.first().dateMs
            val name = setRows.first().sessionName
            val id = "imp${dateMs}_$idx"
            var volume = 0
            setRows.forEach { row ->
                val ex = resolveExercise(row.exerciseName)
                sessionSets.add(SessionSetEntity(0, id, ex.id, ex.name, row.weightKg, row.reps))
                logs.add(SetLogEntity(0, ex.id, row.weightKg, row.reps, dateMs))
                volume += (row.weightKg * row.reps).toInt()
                setsImported++
            }
            sessions.add(SessionEntity(id, dateMs, name, 0, setRows.size, volume, 0))
        }

        if (newExercises.isNotEmpty()) exerciseDao.insertAll(newExercises)
        sessions.forEach { sessionDao.insertSession(it) }
        if (sessionSets.isNotEmpty()) sessionDao.insertSets(sessionSets)
        if (logs.isNotEmpty()) logDao.insertAll(logs)

        return ImportOutcome.Success(
            ImportSummary(parsed.format, sessions.size, setsImported, newExercises.size, parsed.skippedRows),
        )
    }

    /** Every logged set, one row per set, grouped under its session — always in kg (canonical storage)
     *  with an explicit unit column, so it round-trips cleanly back through [importCsv]. */
    suspend fun exportCsv(): String {
        val sessions = sessionDao.allOnce()
        val sets = sessionDao.allSetsOnce().groupBy { it.sessionId }
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.append(Csv.writeRow(listOf("Date", "Workout Name", "Exercise Name", "Weight", "Weight Unit", "Reps"))).append('\n')
        sessions.forEach { s ->
            val dateStr = df.format(Date(s.dateMs))
            sets[s.id].orEmpty().forEach { row ->
                sb.append(Csv.writeRow(listOf(dateStr, s.name, row.name, fmtNum(row.weight), "kg", row.reps.toString()))).append('\n')
            }
        }
        return sb.toString()
    }

    private fun fmtNum(d: Double): String =
        if (d == Math.floor(d) && !d.isInfinite()) d.toLong().toString() else d.toString()
}
