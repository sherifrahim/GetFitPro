package com.getfit.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Exercise library row. The 1,342 dataset records are mapped into the app's muscle-group model;
 * the 18 prototype-curated exercises are also inserted (curated = true) to power the default plan,
 * demo history and PR seed exactly like the prototype.
 */
@Entity(tableName = "exercises", indices = [Index("muscle")])
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscle: String,       // app group: Chest/Back/Legs/Shoulders/Arms/Core/Cardio/Glutes
    val equipment: String,    // normalized ("Bodyweight", "Barbell", ...)
    val level: String,        // Beginner/Intermediate/Advanced
    val target: String,       // dataset target muscle (display)
    val secondaryMuscles: String,
    val reps: String,         // default rep target ("10", "12", "45s")
    val mediaId: String?,
    val cues: String,         // cue steps joined by "||"
    val curated: Boolean = false,
)

@Entity(
    tableName = "set_logs",
    indices = [Index("exerciseId"), Index("dateMs")],
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val exerciseId: String,
    val weight: Double,
    val reps: Int,
    val dateMs: Long,
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val dateMs: Long,
    val name: String,
    val durationSec: Int,
    val totalSets: Int,
    val volume: Int,
    val prs: Int,
    // v2. Which routine the session was started from ("" for ad-hoc / imported), and the
    // watch-derived heart-rate summary + calorie estimate Hevy shows per workout. 0 = not recorded.
    @ColumnInfo(defaultValue = "") val routineId: String = "",
    @ColumnInfo(defaultValue = "0") val avgBpm: Int = 0,
    @ColumnInfo(defaultValue = "0") val maxBpm: Int = 0,
    @ColumnInfo(defaultValue = "0") val calories: Int = 0,
)

/** One heart-rate reading from the watch during a session (v2) — the workout detail's HR graph. */
@Entity(tableName = "heart_rate_samples", indices = [Index("sessionId")])
data class HeartRateSampleEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val sessionId: String,
    val atMs: Long,
    val bpm: Int,
)

/** A body measurement entry (v2): weight and optionally body-fat, one row per logging. */
@Entity(tableName = "measurements", indices = [Index("dateMs")])
data class MeasurementEntity(
    @PrimaryKey val id: String,
    val dateMs: Long,
    val weightKg: Double,
    val bodyFatPct: Double? = null,
    val note: String = "",
)

@Entity(tableName = "session_sets", indices = [Index("sessionId")])
data class SessionSetEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val sessionId: String,
    val exerciseId: String,
    val name: String,
    val weight: Double,
    val reps: Int,
)

@Entity(tableName = "targets")
data class TargetEntity(
    @PrimaryKey val id: String,
    val exId: String,
    val target: Double,
    val start: Double,
    val startDMs: Long,
    val weeks: Int,
)
