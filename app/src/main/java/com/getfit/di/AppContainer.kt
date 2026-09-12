package com.getfit.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.getfit.data.db.GetFitDatabase
import com.getfit.data.backup.BackupRepo
import com.getfit.data.db.Seeder
import com.getfit.data.prefs.RoutinesStore
import com.getfit.data.prefs.SettingsStore
import com.getfit.data.importexport.ImportExportRepo
import com.getfit.data.repo.ExerciseRepo
import com.getfit.data.repo.ProgressRepo
import com.getfit.data.repo.WorkoutRepo
import com.getfit.data.security.SecureKeyStore
import com.getfit.data.sync.SyncRepo
import com.getfit.data.sync.SyncStore
import com.getfit.data.wear.PhoneWearSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "getfit")

/** Manual DI root — built once in GetFitApp, holds the db, stores and repos. */
class AppContainer(val appContext: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val db: GetFitDatabase = Room.databaseBuilder(
        appContext, GetFitDatabase::class.java, "getfit.db",
    ).addMigrations(GetFitDatabase.MIGRATION_1_2).build()

    val settingsStore = SettingsStore(appContext.dataStore)
    val routinesStore = RoutinesStore(appContext.dataStore)
    val sessionStore = com.getfit.data.prefs.SessionStore(appContext.dataStore)
    val secureKeyStore = SecureKeyStore(appContext.dataStore)
    val syncStore = SyncStore(appContext.dataStore)

    val backupRepo = BackupRepo(
        db.exerciseDao(), db.logDao(), db.sessionDao(), db.targetDao(), db.heartRateDao(), db.measurementDao(),
        settingsStore, routinesStore, appContext.filesDir,
    )
    // Sync uploads the backup document, so it depends on backupRepo; the data repos below depend
    // on sync (to flag changes), hence this ordering.
    val syncRepo = SyncRepo(syncStore, secureKeyStore, backupRepo, appScope)

    val exerciseRepo = ExerciseRepo(db.exerciseDao(), db.logDao())
    val workoutRepo = WorkoutRepo(routinesStore, db.exerciseDao(), db.logDao(), db.sessionDao(), db.heartRateDao(), syncRepo)
    val progressRepo = ProgressRepo(db.sessionDao(), db.targetDao(), db.measurementDao(), syncRepo)
    val importExportRepo = ImportExportRepo(db.exerciseDao(), db.logDao(), db.sessionDao(), workoutRepo)
    val phoneWearSync = PhoneWearSync(appContext)

    /** Seed the library on first launch (idempotent); purge old demo rows once on upgraded installs. */
    fun seedOnFirstLaunch() {
        appScope.launch {
            Seeder.seedIfEmpty(appContext, db)
            settingsStore.setSeeded(true)
            if (!settingsStore.isDemoPurged()) {
                if (Seeder.purgeDemoData(db) > 0) syncRepo.noteChange()
                settingsStore.setDemoPurged()
            }
            if (!settingsStore.isImportMusclesFixed()) {
                if (Seeder.reinferImportedMuscles(db) > 0) syncRepo.noteChange()
                settingsStore.setImportMusclesFixed()
            }
        }
    }

    /** "Clear all data": wipe user tables + reset routines and preferences. */
    suspend fun clearAllData() {
        Seeder.clearAll(db)
        routinesStore.resetToDefault()
        settingsStore.resetToDefaults()
    }
}
