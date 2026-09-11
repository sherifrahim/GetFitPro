package com.getfit.data.repo

import com.getfit.data.db.ExerciseDao
import com.getfit.data.db.ExerciseEntity
import com.getfit.data.db.HeartRateDao
import com.getfit.data.db.HeartRateSampleEntity
import com.getfit.data.db.LogDao
import com.getfit.data.db.MeasurementDao
import com.getfit.data.db.MeasurementEntity
import com.getfit.data.db.SessionDao
import com.getfit.data.db.SessionEntity
import com.getfit.data.db.SessionSetEntity
import com.getfit.data.db.SetLogEntity
import com.getfit.data.db.TargetDao
import com.getfit.data.db.TargetEntity
import com.getfit.data.prefs.PlanItemData
import com.getfit.data.prefs.Routine
import com.getfit.data.prefs.RoutinesData
import com.getfit.data.prefs.RoutinesStore
import com.getfit.data.sync.SyncRepo
import com.getfit.domain.Best
import com.getfit.domain.HrPoint
import com.getfit.domain.LoggedSet
import com.getfit.domain.bestFor
import com.getfit.domain.isBW
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Library + logged-set access, with best/PR derivation. */
class ExerciseRepo(
    private val exerciseDao: ExerciseDao,
    private val logDao: LogDao,
) {
    val exercises: Flow<List<ExerciseEntity>> = exerciseDao.observeAll()
    val logs: Flow<List<SetLogEntity>> = logDao.observeAll()

    suspend fun byId(id: String): ExerciseEntity? = exerciseDao.byId(id)
    suspend fun byIds(ids: List<String>): List<ExerciseEntity> = exerciseDao.byIds(ids)
    suspend fun lastWeight(id: String): Double? = logDao.latest(id)?.weight

    fun isBodyweight(ex: ExerciseEntity): Boolean = isBW(ex.equipment, ex.reps)

    /** Best for one exercise given the full log list (avoids per-exercise DB round-trips). */
    fun best(ex: ExerciseEntity, allLogs: List<SetLogEntity>): Best? =
        bestFor(
            allLogs.filter { it.exerciseId == ex.id }
                .map { LoggedSet(it.exerciseId, it.weight, it.reps, it.dateMs) },
            bodyweight = isBodyweight(ex),
        )
}

/** Routines (the workout plans) + session persistence. */
class WorkoutRepo(
    private val routinesStore: RoutinesStore,
    private val exerciseDao: ExerciseDao,
    private val logDao: LogDao,
    private val sessionDao: SessionDao,
    private val heartRateDao: HeartRateDao,
    private val syncRepo: SyncRepo,
) {
    val routines: Flow<RoutinesData> = routinesStore.flow

    /** The current ("up next") routine's items — what the old single plan was. */
    val plan: Flow<List<PlanItemData>> = routines.map { it.current?.items.orEmpty() }

    // ---- routine list ----

    suspend fun createRoutine(name: String, items: List<PlanItemData> = emptyList(), makeCurrent: Boolean = false): Routine {
        val r = Routine(newId(), name.trim().ifBlank { "New routine" }, items)
        routinesStore.update { d ->
            d.copy(routines = d.routines + r, currentId = if (makeCurrent || d.routines.isEmpty()) r.id else d.currentId)
        }
        syncRepo.noteChange()
        return r
    }

    suspend fun renameRoutine(id: String, name: String) = edit(id) { it.copy(name = name.trim().ifBlank { it.name }) }

    suspend fun duplicateRoutine(id: String) {
        val src = current().routines.firstOrNull { it.id == id } ?: return
        routinesStore.update { d ->
            val i = d.routines.indexOfFirst { it.id == id }
            val copy = src.copy(id = newId(), name = src.name + " copy")
            d.copy(routines = d.routines.toMutableList().apply { add(i + 1, copy) })
        }
        syncRepo.noteChange()
    }

    /** The last routine can't be deleted: Home and the watch always need something to offer. */
    suspend fun deleteRoutine(id: String): Boolean {
        val d = current()
        if (d.routines.size <= 1 || d.routines.none { it.id == id }) return false
        routinesStore.update { cur ->
            val rest = cur.routines.filterNot { it.id == id }
            val nextCurrent = if (cur.currentId == id) (cur.nextAfter(id)?.id ?: rest.first().id) else cur.currentId
            cur.copy(routines = rest, currentId = nextCurrent)
        }
        syncRepo.noteChange()
        return true
    }

    suspend fun moveRoutine(index: Int, dir: Int) {
        routinesStore.update { d ->
            val list = d.routines.toMutableList()
            val j = index + dir
            if (index !in list.indices || j !in list.indices) return@update d
            val t = list[index]; list[index] = list[j]; list[j] = t
            d.copy(routines = list)
        }
        syncRepo.noteChange()
    }

    /** "Do this one next" — from the phone or the watch. */
    suspend fun setCurrentRoutine(id: String) {
        if (current().routines.none { it.id == id }) return
        routinesStore.update { it.copy(currentId = id) }
        syncRepo.noteChange()
    }

    /** Called when a session for [finishedRoutineId] is saved: rotate to the one after it. */
    suspend fun advanceAfter(finishedRoutineId: String) {
        if (finishedRoutineId.isBlank()) return
        routinesStore.update { d -> d.nextAfter(finishedRoutineId)?.let { d.copy(currentId = it.id) } ?: d }
        syncRepo.noteChange()
    }

    // ---- items within one routine (null id = the current routine) ----

    /** Returns false if already present. */
    suspend fun addToPlan(id: String, reps: String, sets: Int = 3, routineId: String? = null): Boolean {
        val r = resolve(routineId) ?: return false
        if (r.items.any { it.id == id }) return false
        edit(r.id) { it.copy(items = it.items + PlanItemData(id, sets, reps)) }
        return true
    }

    suspend fun removeFromPlan(id: String, routineId: String? = null) {
        val r = resolve(routineId) ?: return
        edit(r.id) { it.copy(items = it.items.filterNot { p -> p.id == id }) }
    }

    suspend fun movePlan(index: Int, dir: Int, routineId: String? = null) {
        val r = resolve(routineId) ?: return
        val p = r.items.toMutableList()
        val j = index + dir
        if (index !in p.indices || j !in p.indices) return
        val t = p[index]; p[index] = p[j]; p[j] = t
        edit(r.id) { it.copy(items = p) }
    }

    suspend fun setSets(id: String, delta: Int, routineId: String? = null) {
        val r = resolve(routineId) ?: return
        edit(r.id) { it.copy(items = it.items.map { p -> if (p.id == id) p.copy(sets = (p.sets + delta).coerceIn(1, 8)) else p }) }
    }

    suspend fun setReps(id: String, reps: String, routineId: String? = null) {
        val r = resolve(routineId) ?: return
        val clean = reps.trim()
        if (clean.isBlank()) return
        edit(r.id) { it.copy(items = it.items.map { p -> if (p.id == id) p.copy(reps = clean) else p }) }
    }

    suspend fun resetPlan() {
        routinesStore.resetToDefault()
        syncRepo.noteChange()
    }

    // ---- sessions ----

    /** Persist a completed session: history record + session sets + one SetLog per set + HR trace. */
    suspend fun saveSession(
        now: Long,
        name: String,
        durationSec: Int,
        sets: List<SessionSaveSet>,
        volume: Int,
        prs: Int,
        routineId: String = "",
        avgBpm: Int = 0,
        maxBpm: Int = 0,
        calories: Int = 0,
        heartRate: List<HrPoint> = emptyList(),
    ) {
        if (sets.isEmpty()) return
        val id = "h$now"
        sessionDao.insertSession(
            SessionEntity(id, now, name, durationSec, sets.size, volume, prs, routineId, avgBpm, maxBpm, calories),
        )
        sessionDao.insertSets(sets.map { SessionSetEntity(0, id, it.exerciseId, it.name, it.weight, it.reps) })
        logDao.insertAll(sets.map { SetLogEntity(0, it.exerciseId, it.weight, it.reps, now) })
        if (heartRate.isNotEmpty()) heartRateDao.insertAll(heartRate.map { HeartRateSampleEntity(0, id, it.atMs, it.bpm) })
        syncRepo.noteChange()
    }

    /** Removes a history record and everything hanging off it, including its PR-history logs. */
    suspend fun deleteSession(id: String) {
        val s = sessionDao.byId(id) ?: return
        sessionDao.deleteSetsFor(id)
        heartRateDao.deleteForSession(id)
        logDao.deleteAt(s.dateMs)
        sessionDao.deleteSession(id)
        syncRepo.noteChange()
    }

    suspend fun sessionSets(id: String): List<SessionSetEntity> = sessionDao.setsFor(id)
    suspend fun sessionHeartRate(id: String): List<HeartRateSampleEntity> = heartRateDao.forSession(id)

    /**
     * Hevy's "Save as Routine" on a past workout: one item per distinct exercise, in first-logged
     * order, with that session's set count and its most common rep count. Returns the new routine.
     */
    suspend fun routineFromSession(id: String): Routine? {
        val s = sessionDao.byId(id) ?: return null
        val items = routineItemsFromSets(sessionDao.setsFor(id))
        if (items.isEmpty()) return null
        return createRoutine(s.name, items)
    }

    // ---- helpers ----

    private suspend fun current(): RoutinesData = routinesStore.flow.first()
    private suspend fun resolve(routineId: String?): Routine? {
        val d = current()
        return if (routineId == null) d.current else d.routines.firstOrNull { it.id == routineId }
    }
    private suspend fun edit(id: String, fn: (Routine) -> Routine) {
        routinesStore.update { d -> d.copy(routines = d.routines.map { if (it.id == id) fn(it) else it }) }
        syncRepo.noteChange()
    }
    private fun newId(): String = "r" + System.currentTimeMillis() + "_" + (Math.random() * 1000).toInt()

    companion object {
        /** Shared with the CSV importer, which builds routines from imported workouts the same way. */
        fun routineItemsFromSets(sets: List<SessionSetEntity>): List<PlanItemData> =
            sets.groupBy { it.exerciseId }.map { (exId, rows) ->
                val reps = rows.groupingBy { it.reps }.eachCount().maxByOrNull { it.value }?.key ?: 10
                PlanItemData(exId, rows.size.coerceIn(1, 8), reps.toString())
            }
    }
}

data class SessionSaveSet(val exerciseId: String, val name: String, val weight: Double, val reps: Int)

/** History + targets + body measurements. */
class ProgressRepo(
    private val sessionDao: SessionDao,
    private val targetDao: TargetDao,
    private val measurementDao: MeasurementDao,
    private val syncRepo: SyncRepo,
) {

    val sessions: Flow<List<SessionEntity>> = sessionDao.observeAll()
    val sessionSets: Flow<List<SessionSetEntity>> = sessionDao.observeAllSets()
    val targets: Flow<List<TargetEntity>> = targetDao.observeAll()
    val measurements: Flow<List<MeasurementEntity>> = measurementDao.observeAll()

    suspend fun addTarget(t: TargetEntity) {
        targetDao.insert(t)
        syncRepo.noteChange()
    }

    suspend fun deleteTarget(id: String) {
        targetDao.delete(id)
        syncRepo.noteChange()
    }

    suspend fun addMeasurement(m: MeasurementEntity) {
        measurementDao.insert(m)
        syncRepo.noteChange()
    }

    suspend fun deleteMeasurement(id: String) {
        measurementDao.delete(id)
        syncRepo.noteChange()
    }
}
