package com.getfit.data.db

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Seeds the library from assets/exercises.json (1,342) plus the 18 curated exercises.
 *
 * The prototype also seeded demo history and two demo targets on first run. That was dropped on
 * request once the app ran on real hardware: a fresh install now starts with an EMPTY log — real
 * zeros and the empty states — so nothing in Progress or the PR list is ever a placeholder. The
 * demo values remain in Curated.SEED for the PR unit tests, which pin the prototype's maths.
 */
object Seeder {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun seedIfEmpty(context: Context, db: GetFitDatabase, now: Long = System.currentTimeMillis()) {
        if (db.exerciseDao().count() > 0) return
        seed(context, db, now)
    }

    /** Library seed. Used on first launch; "Clear all data" keeps the library and only wipes user rows. */
    suspend fun seed(context: Context, db: GetFitDatabase, now: Long = System.currentTimeMillis()) {
        val raw = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val dtos = json.decodeFromString<List<ExerciseDto>>(raw)

        val entities = dtos.map { it.toEntity() } + Curated.EXERCISES.map { it.toEntity() }
        db.exerciseDao().insertAll(entities)
    }

    /**
     * One-time cleanup for installs that predate the no-demo-data change: removes the seeded demo
     * logs and targets, and nothing else. Demo logs are recognised as orphans (no session shares
     * their timestamp) whose exercise/weight/reps match a SEED entry; demo targets by their fixed
     * ids AND values, so a user target that happened to reuse an id survives. Returns rows removed.
     */
    suspend fun purgeDemoData(db: GetFitDatabase): Int {
        val demo = Curated.SEED.flatMap { (id, sets) -> sets.map { (w, reps, _) -> Triple(id, w, reps) } }.toSet()
        val orphanDemo = db.logDao().orphans().filter { Triple(it.exerciseId, it.weight, it.reps) in demo }
        if (orphanDemo.isNotEmpty()) db.logDao().deleteRows(orphanDemo.map { it.rowId })
        var removed = orphanDemo.size
        val targets = db.targetDao().allOnce()
        Curated.SEED_TARGETS.forEach { t ->
            val hit = targets.firstOrNull { it.id == t.id && it.exId == t.exId && it.target == t.target && it.start == t.start }
            if (hit != null) { db.targetDao().delete(hit.id); removed++ }
        }
        return removed
    }

    /**
     * One-shot for installs whose CSV imports predate [inferMuscle]: every invented `imp_*` exercise
     * was filed under "Core". Re-derive from the name; returns how many rows changed.
     */
    suspend fun reinferImportedMuscles(db: GetFitDatabase): Int {
        var changed = 0
        db.exerciseDao().imported().forEach { e ->
            val m = inferMuscle(e.name)
            if (m != e.muscle) { db.exerciseDao().setMuscle(e.id, m); changed++ }
        }
        return changed
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
        db.heartRateDao().clear()
        db.measurementDao().clear()
    }
}
