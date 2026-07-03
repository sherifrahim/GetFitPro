package com.getfit.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        SetLogEntity::class,
        SessionEntity::class,
        SessionSetEntity::class,
        TargetEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GetFitDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun logDao(): LogDao
    abstract fun sessionDao(): SessionDao
    abstract fun targetDao(): TargetDao
}
