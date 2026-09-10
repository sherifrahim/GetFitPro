package com.getfit.data.backup

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
import com.getfit.data.prefs.SettingsStore
import java.io.File
import kotlinx.coroutines.flow.first

data class BackupInfo(val createdAtMs: Long, val sizeBytes: Long, val records: Int)

sealed class RestoreOutcome {
    data class Success(val info: BackupInfo) : RestoreOutcome()
    data class Failure(val message: String) : RestoreOutcome()
}

/**
 * On-device backup and restore.
 *
 * "On device" means app-private storage (`filesDir/backups/`), which needs no permission and is
 * removed when the app is uninstalled. That last part matters and is why the Settings screen also
 * offers Save-a-copy/Restore-from-file through the storage picker: the local slot protects against
 * a bad restore, a botched import or accidental "Clear all data", but it is NOT protection against
 * losing the phone. Both paths write the identical format.
 */
class BackupRepo(
    private val exerciseDao: ExerciseDao,
    private val logDao: LogDao,
    private val sessionDao: SessionDao,
    private val targetDao: TargetDao,
    private val settingsStore: SettingsStore,
    private val planStore: PlanStore,
    private val filesDir: File,
) {
    private val dir: File get() = File(filesDir, "backups")
    private val slot: File get() = File(dir, "forge-backup.json")

    // --- build ---

    suspend fun buildBackup(now: Long = System.currentTimeMillis()): BackupFile {
        val settings = settingsStore.flow.first()
        val plan = planStore.flow.first()
        val sessions = sessionDao.allOnce()
        val sessionSets = sessionDao.allSetsOnce()
        val logs = logDao.allOnce()
        val targets = targetDao.allOnce()

        // Carry only the exercises this history actually points at — see BackupModels' class doc.
        // Includes the plan and targets, or a restore could leave either referencing a missing row.
        val referenced = buildSet {
            sessionSets.forEach { add(it.exerciseId) }
            logs.forEach { add(it.exerciseId) }
            targets.forEach { add(it.exId) }
            plan.forEach { add(it.id) }
        }
        val exercises = if (referenced.isEmpty()) emptyList() else exerciseDao.byIds(referenced.toList())

        return BackupFile(
            createdAtMs = now,
            settings = BackupSettings(
                units = settings.units,
                sound = settings.sound,
                haptics = settings.haptics,
                autorest = settings.autorest,
                restDefault = settings.restDefault,
                intensity = settings.intensity,
                onboarded = settings.onboarded,
                aiModel = settings.aiModel,
            ),
            plan = plan.map { BackupPlanItem(it.id, it.sets, it.reps) },
            exercises = exercises.map {
                BackupExercise(
                    id = it.id, name = it.name, muscle = it.muscle, equipment = it.equipment,
                    level = it.level, target = it.target, secondaryMuscles = it.secondaryMuscles,
                    reps = it.reps, mediaId = it.mediaId, cues = it.cues, curated = it.curated,
                )
            },
            sessions = sessions.map {
                BackupSession(it.id, it.dateMs, it.name, it.durationSec, it.totalSets, it.volume, it.prs)
            },
            sessionSets = sessionSets.map {
                BackupSessionSet(it.sessionId, it.exerciseId, it.name, it.weight, it.reps)
            },
            logs = logs.map { BackupLog(it.exerciseId, it.weight, it.reps, it.dateMs) },
            targets = targets.map {
                BackupTarget(it.id, it.exId, it.target, it.start, it.startDMs, it.weeks)
            },
        )
    }

    suspend fun exportText(): String = encodeBackup(buildBackup())

    // --- local slot ---

    /**
     * Writes to a temp file and renames over the slot, so an interrupted write can't leave the
     * previous backup truncated. A half-written backup is worse than a stale one — the user only
     * finds out either way at the moment they need it.
     */
    suspend fun backupToDevice(): Result<BackupInfo> = runCatching {
        val text = exportText()
        dir.mkdirs()
        val tmp = File(dir, "forge-backup.json.tmp")
        tmp.writeText(text)
        if (slot.exists()) slot.delete()
        check(tmp.renameTo(slot)) { "Couldn't finalise the backup file." }
        val parsed = parseBackup(text) as BackupParseResult.Success
        BackupInfo(parsed.backup.createdAtMs, slot.length(), parsed.backup.totalRecords)
    }

    /** Metadata for the stored backup, or null if there isn't one. */
    fun localBackupInfo(): BackupInfo? {
        if (!slot.isFile) return null
        val text = runCatching { slot.readText() }.getOrNull() ?: return null
        val parsed = parseBackup(text) as? BackupParseResult.Success ?: return null
        return BackupInfo(parsed.backup.createdAtMs, slot.length(), parsed.backup.totalRecords)
    }

    suspend fun restoreFromDevice(): RestoreOutcome {
        if (!slot.isFile) return RestoreOutcome.Failure("There's no backup on this device yet.")
        val text = runCatching { slot.readText() }.getOrNull()
            ?: return RestoreOutcome.Failure("Couldn't read the stored backup.")
        return restoreFromText(text)
    }

    fun deleteLocalBackup(): Boolean = slot.delete()

    // --- restore ---

    /**
     * Replaces the user's data with the backup's. Deliberately replace, not merge: workout history
     * has no dependable identity across installs, so merging would either duplicate every session
     * or silently drop edits. "Restore" should mean "put it back how it was", and the UI confirms
     * before calling this.
     */
    suspend fun restoreFromText(text: String): RestoreOutcome {
        val backup = when (val r = parseBackup(text)) {
            is BackupParseResult.Failure -> return RestoreOutcome.Failure(r.message)
            is BackupParseResult.Success -> r.backup
        }

        return runCatching {
            // Exercises first — sets and targets restored below reference them.
            if (backup.exercises.isNotEmpty()) {
                exerciseDao.insertAll(
                    backup.exercises.map {
                        ExerciseEntity(
                            id = it.id, name = it.name, muscle = it.muscle, equipment = it.equipment,
                            level = it.level, target = it.target, secondaryMuscles = it.secondaryMuscles,
                            reps = it.reps, mediaId = it.mediaId, cues = it.cues, curated = it.curated,
                        )
                    },
                )
            }

            sessionDao.clearSets()
            sessionDao.clearSessions()
            logDao.clear()
            targetDao.clear()

            backup.sessions.forEach {
                sessionDao.insertSession(
                    SessionEntity(it.id, it.dateMs, it.name, it.durationSec, it.totalSets, it.volume, it.prs),
                )
            }
            if (backup.sessionSets.isNotEmpty()) {
                sessionDao.insertSets(
                    backup.sessionSets.map { SessionSetEntity(0, it.sessionId, it.exerciseId, it.name, it.weight, it.reps) },
                )
            }
            if (backup.logs.isNotEmpty()) {
                logDao.insertAll(backup.logs.map { SetLogEntity(0, it.exerciseId, it.weight, it.reps, it.dateMs) })
            }
            if (backup.targets.isNotEmpty()) {
                targetDao.insertAll(
                    backup.targets.map { TargetEntity(it.id, it.exId, it.target, it.start, it.startDMs, it.weeks) },
                )
            }

            planStore.set(backup.plan.map { PlanItemData(it.id, it.sets, it.reps) })

            with(backup.settings) {
                settingsStore.setUnits(units)
                settingsStore.setSound(sound)
                settingsStore.setHaptics(haptics)
                settingsStore.setAutorest(autorest)
                settingsStore.setIntensity(intensity)
                // setIntensity overwrites restDefault with the preset's value, so the user's own
                // rest length has to be reapplied after it, not before.
                settingsStore.setRestDefault(restDefault)
                settingsStore.setOnboarded(onboarded)
                settingsStore.setAiModel(aiModel)
            }
            // Never restore `seeded` from the file. The library on THIS install is already seeded,
            // and a false value would make the next launch re-seed demo history on top of the data
            // we just restored.
            settingsStore.setSeeded(true)

            BackupInfo(backup.createdAtMs, text.length.toLong(), backup.totalRecords)
        }.fold(
            onSuccess = { RestoreOutcome.Success(it) },
            onFailure = { RestoreOutcome.Failure(it.message ?: "Restore failed.") },
        )
    }
}
