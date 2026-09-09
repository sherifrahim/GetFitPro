package com.getfit.data.repo

import com.getfit.data.db.ExerciseDao
import com.getfit.data.db.ExerciseEntity
import com.getfit.data.db.LogDao
import com.getfit.data.db.SessionDao
import com.getfit.data.db.SessionEntity
import com.getfit.data.db.SessionSetEntity
import com.getfit.data.db.SetLogEntity
import com.getfit.data.db.TargetDao
import com.getfit.data.db.TargetEntity
import com.getfit.data.prefs.PlanItemData
import com.getfit.data.prefs.PlanStore
import com.getfit.data.sync.SyncRepo
import com.getfit.data.sync.SyncSessionSnapshot
import com.getfit.data.sync.SyncTargetSnapshot
import com.getfit.domain.Best
import com.getfit.domain.LoggedSet
import com.getfit.domain.bestFor
import com.getfit.domain.isBW
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

/** Workout plan + session persistence. */
class WorkoutRepo(
    private val planStore: PlanStore,
    private val exerciseDao: ExerciseDao,
    private val logDao: LogDao,
    private val sessionDao: SessionDao,
    private val syncRepo: SyncRepo,
) {
    private val syncJson = Json { ignoreUnknownKeys = true }
    val plan: Flow<List<PlanItemData>> = planStore.flow

    /** Returns false if already present. */
    suspend fun addToPlan(id: String, reps: String, sets: Int = 3): Boolean {
        val cur = currentPlan()
        if (cur.any { it.id == id }) return false
        planStore.set(cur + PlanItemData(id, sets, reps))
        return true
    }

    suspend fun removeFromPlan(id: String) =
        planStore.set(currentPlan().filterNot { it.id == id })

    suspend fun movePlan(index: Int, dir: Int) {
        val p = currentPlan().toMutableList()
        val j = index + dir
        if (index !in p.indices || j !in p.indices) return
        val t = p[index]; p[index] = p[j]; p[j] = t
        planStore.set(p)
    }

    suspend fun setSets(id: String, delta: Int) = planStore.set(
        currentPlan().map { if (it.id == id) it.copy(sets = (it.sets + delta).coerceIn(1, 8)) else it },
    )

    suspend fun resetPlan() = planStore.resetToDefault()

    /** Persist a completed session: history record + session sets + one SetLog per set. */
    suspend fun saveSession(
        now: Long,
        name: String,
        durationSec: Int,
        sets: List<SessionSaveSet>,
        volume: Int,
        prs: Int,
    ) {
        if (sets.isEmpty()) return
        val id = "h$now"
        sessionDao.insertSession(
            SessionEntity(id, now, name, durationSec, sets.size, volume, prs),
        )
        sessionDao.insertSets(sets.map { SessionSetEntity(0, id, it.exerciseId, it.name, it.weight, it.reps) })
        logDao.insertAll(sets.map { SetLogEntity(0, it.exerciseId, it.weight, it.reps, now) })
        syncRepo.enqueue(
            "session", id, "upsert",
            syncJson.encodeToString(SyncSessionSnapshot(id, now, name, durationSec, sets.size, volume, prs)),
        )
    }

    private suspend fun currentPlan(): List<PlanItemData> = planStore.flow.first()
}

data class SessionSaveSet(val exerciseId: String, val name: String, val weight: Double, val reps: Int)

/** History + targets. */
class ProgressRepo(
    private val sessionDao: SessionDao,
    private val targetDao: TargetDao,
    private val syncRepo: SyncRepo,
) {
    private val syncJson = Json { ignoreUnknownKeys = true }

    val sessions: Flow<List<SessionEntity>> = sessionDao.observeAll()
    val targets: Flow<List<TargetEntity>> = targetDao.observeAll()

    suspend fun addTarget(t: TargetEntity) {
        targetDao.insert(t)
        syncRepo.enqueue(
            "target", t.id, "upsert",
            syncJson.encodeToString(SyncTargetSnapshot(t.id, t.exId, t.target, t.start, t.startDMs, t.weeks)),
        )
    }

    suspend fun deleteTarget(id: String) {
        targetDao.delete(id)
        syncRepo.enqueue("target", id, "delete", "")
    }
}
