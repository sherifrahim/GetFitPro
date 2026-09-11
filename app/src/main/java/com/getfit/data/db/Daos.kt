package com.getfit.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExerciseEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT * FROM exercises ORDER BY curated DESC, name ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun byId(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<ExerciseEntity>
}

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: SetLogEntity)

    @Insert
    suspend fun insertAll(logs: List<SetLogEntity>)

    @Query("SELECT * FROM set_logs ORDER BY dateMs ASC")
    fun observeAll(): Flow<List<SetLogEntity>>

    /** One-shot read for backup/export, matching SessionDao.allOnce(). */
    @Query("SELECT * FROM set_logs ORDER BY dateMs ASC")
    suspend fun allOnce(): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :id ORDER BY dateMs ASC")
    suspend fun forExercise(id: String): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :id ORDER BY dateMs DESC LIMIT 1")
    suspend fun latest(id: String): SetLogEntity?

    /** Logs are stamped with their session's exact dateMs (saveSession/import both do this), so
     *  this is how a deleted session takes its sets out of the PR history too. */
    @Query("DELETE FROM set_logs WHERE dateMs = :dateMs")
    suspend fun deleteAt(dateMs: Long)

    @Query("DELETE FROM set_logs")
    suspend fun clear()
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Insert
    suspend fun insertSets(sets: List<SessionSetEntity>)

    @Query("SELECT * FROM sessions ORDER BY dateMs DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY dateMs ASC")
    suspend fun allOnce(): List<SessionEntity>

    @Query("SELECT * FROM session_sets")
    suspend fun allSetsOnce(): List<SessionSetEntity>

    /** Live view of every session's sets, for the history feed's exercise previews. Small table: one
     *  row per logged set. */
    @Query("SELECT * FROM session_sets ORDER BY rowId ASC")
    fun observeAllSets(): Flow<List<SessionSetEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun byId(id: String): SessionEntity?

    @Query("SELECT * FROM session_sets WHERE sessionId = :sessionId ORDER BY rowId ASC")
    suspend fun setsFor(sessionId: String): List<SessionSetEntity>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("DELETE FROM session_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsFor(sessionId: String)

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM session_sets")
    suspend fun clearSets()
}

@Dao
interface TargetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: TargetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(t: List<TargetEntity>)

    @Query("SELECT * FROM targets")
    fun observeAll(): Flow<List<TargetEntity>>

    /** One-shot read for backup/export, matching SessionDao.allOnce(). */
    @Query("SELECT * FROM targets")
    suspend fun allOnce(): List<TargetEntity>

    @Query("DELETE FROM targets WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM targets")
    suspend fun clear()
}

@Dao
interface HeartRateDao {
    @Insert
    suspend fun insertAll(samples: List<HeartRateSampleEntity>)

    @Query("SELECT * FROM heart_rate_samples WHERE sessionId = :sessionId ORDER BY atMs ASC")
    suspend fun forSession(sessionId: String): List<HeartRateSampleEntity>

    @Query("SELECT * FROM heart_rate_samples ORDER BY atMs ASC")
    suspend fun allOnce(): List<HeartRateSampleEntity>

    @Query("DELETE FROM heart_rate_samples WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    @Query("DELETE FROM heart_rate_samples")
    suspend fun clear()
}

@Dao
interface MeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(m: MeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(m: List<MeasurementEntity>)

    @Query("SELECT * FROM measurements ORDER BY dateMs DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements ORDER BY dateMs ASC")
    suspend fun allOnce(): List<MeasurementEntity>

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM measurements")
    suspend fun clear()
}
