package com.getfit.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.getfit.data.db.GetFitDatabase
import com.getfit.data.db.Seeder
import com.getfit.data.prefs.PlanStore
import com.getfit.data.prefs.SettingsStore
import com.getfit.data.repo.ExerciseRepo
import com.getfit.data.repo.ProgressRepo
import com.getfit.data.repo.WorkoutRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "getfit")

/** Manual DI root — built once in GetFitApp, holds the db, stores and repos. */
class AppContainer(private val appContext: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val db: GetFitDatabase = Room.databaseBuilder(
        appContext, GetFitDatabase::class.java, "getfit.db",
    ).build()

    val settingsStore = SettingsStore(appContext.dataStore)
    val planStore = PlanStore(appContext.dataStore)

    val exerciseRepo = ExerciseRepo(db.exerciseDao(), db.logDao())
    val workoutRepo = WorkoutRepo(planStore, db.exerciseDao(), db.logDao(), db.sessionDao())
    val progressRepo = ProgressRepo(db.sessionDao(), db.targetDao())

    /** Seed the library + demo data on first launch (idempotent). */
    fun seedOnFirstLaunch() {
        appScope.launch {
            Seeder.seedIfEmpty(appContext, db)
            settingsStore.setSeeded(true)
        }
    }

    /** "Clear all data": wipe user tables + reset plan and preferences. */
    suspend fun clearAllData() {
        Seeder.clearAll(db)
        planStore.resetToDefault()
        settingsStore.resetToDefaults()
    }
}
