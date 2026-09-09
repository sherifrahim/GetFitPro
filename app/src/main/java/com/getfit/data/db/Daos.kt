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

    @Query("SELECT * FROM set_logs WHERE exerciseId = :id ORDER BY dateMs ASC")
    suspend fun forExercise(id: String): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :id ORDER BY dateMs DESC LIMIT 1")
    suspend fun latest(id: String): SetLogEntity?

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

    @Query("DELETE FROM targets WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM targets")
    suspend fun clear()
}
