package com.getfit.data.db

import android.content.Context
import com.getfit.domain.DAY_MS
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Seeds the library from assets/exercises.json (1,342) plus the 18 curated exercises, then the
 * prototype's demo history and targets — so a fresh install mirrors the prototype's first-run state.
 */
object Seeder {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun seedIfEmpty(context: Context, db: GetFitDatabase, now: Long = System.currentTimeMillis()) {
        if (db.exerciseDao().count() > 0) return
        seed(context, db, now)
    }

    /** Full (re)seed. Used on first launch and after "Clear all data". */
    suspend fun seed(context: Context, db: GetFitDatabase, now: Long = System.currentTimeMillis()) {
        val raw = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val dtos = json.decodeFromString<List<ExerciseDto>>(raw)

        val entities = dtos.map { it.toEntity() } + Curated.EXERCISES.map { it.toEntity() }
        db.exerciseDao().insertAll(entities)

        val logs = Curated.SEED.flatMap { (id, sets) ->
            sets.map { (w, reps, daysAgo) ->
                SetLogEntity(exerciseId = id, weight = w, reps = reps, dateMs = now - daysAgo * DAY_MS)
            }
        }
        db.logDao().insertAll(logs)

        val targets = Curated.SEED_TARGETS.map { t ->
            TargetEntity(
                id = t.id, exId = t.exId, target = t.target, start = t.start,
                startDMs = now - t.startedDaysAgo * DAY_MS, weeks = t.weeks,
            )
        }
        db.targetDao().insertAll(targets)
    }

    /**
     * Wipe all user data (proto clearAll, L816-822): logs, history and targets become empty —
     * real zeros + empty states, NOT re-seeded demo data. The exercise library is kept.
     * Plan/settings reset is handled by the DataStore layer.
     */
    suspend fun clearAll(db: GetFitDatabase) {
        db.logDao().clear()
        db.sessionDao().clearSets()
        db.sessionDao().clearSessions()
        db.targetDao().clear()
    }
}
