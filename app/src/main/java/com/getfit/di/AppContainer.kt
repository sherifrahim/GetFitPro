package com.getfit.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.getfit.data.db.GetFitDatabase
import com.getfit.data.backup.BackupRepo
import com.getfit.data.db.Seeder
import com.getfit.data.prefs.PlanStore
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
    ).build()

    val settingsStore = SettingsStore(appContext.dataStore)
    val planStore = PlanStore(appContext.dataStore)
    val sessionStore = com.getfit.data.prefs.SessionStore(appContext.dataStore)
    val secureKeyStore = SecureKeyStore(appContext.dataStore)
    val syncStore = SyncStore(appContext.dataStore)

    val backupRepo = BackupRepo(
        db.exerciseDao(), db.logDao(), db.sessionDao(), db.targetDao(),
        settingsStore, planStore, appContext.filesDir,
    )
    // Sync uploads the backup document, so it depends on backupRepo; the data repos below depend
    // on sync (to flag changes), hence this ordering.
    val syncRepo = SyncRepo(syncStore, secureKeyStore, backupRepo, appScope)

    val exerciseRepo = ExerciseRepo(db.exerciseDao(), db.logDao())
    val workoutRepo = WorkoutRepo(planStore, db.exerciseDao(), db.logDao(), db.sessionDao(), syncRepo)
    val progressRepo = ProgressRepo(db.sessionDao(), db.targetDao(), syncRepo)
    val importExportRepo = ImportExportRepo(db.exerciseDao(), db.logDao(), db.sessionDao())
    val phoneWearSync = PhoneWearSync(appContext)

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
