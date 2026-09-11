package com.getfit.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        SetLogEntity::class,
        SessionEntity::class,
        SessionSetEntity::class,
        TargetEntity::class,
        HeartRateSampleEntity::class,
        MeasurementEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class GetFitDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun logDao(): LogDao
    abstract fun sessionDao(): SessionDao
    abstract fun targetDao(): TargetDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun measurementDao(): MeasurementDao

    companion object {
        /**
         * v1 -> v2: routines, watch heart rate and body measurements. Additive only — every new
         * column has a default, so the user's existing history (including a freshly imported Hevy
         * export) survives untouched. Never replace this with fallbackToDestructiveMigration: that
         * would wipe that history on the first launch after an update.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN routineId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sessions ADD COLUMN avgBpm INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN maxBpm INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN calories INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS heart_rate_samples (" +
                        "rowId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "sessionId TEXT NOT NULL, atMs INTEGER NOT NULL, bpm INTEGER NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_heart_rate_samples_sessionId ON heart_rate_samples (sessionId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS measurements (" +
                        "id TEXT PRIMARY KEY NOT NULL, dateMs INTEGER NOT NULL, weightKg REAL NOT NULL, " +
                        "bodyFatPct REAL, note TEXT NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_measurements_dateMs ON measurements (dateMs)")
            }
        }
    }
}
