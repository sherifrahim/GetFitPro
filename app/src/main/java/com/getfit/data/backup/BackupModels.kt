package com.getfit.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Full-fidelity local backup of everything the user has actually created.
 *
 * Distinct from the CSV import/export in `data/importexport`, which is for moving data between
 * *apps* and is deliberately lossy: it carries only date/exercise/weight/reps, so it drops targets,
 * the plan, settings, session durations and PR counts, and on the way back in it re-resolves
 * exercises by name (creating `imp_*` duplicates). This format restores the app to exactly where it
 * was.
 *
 * These DTOs are intentionally NOT the Room entities with `@Serializable` bolted on. The file format
 * is a compatibility contract that outlives any one schema; binding it to the entities would mean a
 * routine DB change silently altering every backup file ever written. The mapping in
 * [com.getfit.data.backup.BackupRepo] is the seam where a schema change gets noticed and handled.
 *
 * The exercise library is not backed up wholesale — it is 1,342 seeded rows that any install can
 * regenerate. Only exercises the user's own history actually *references* are carried, which keeps
 * the file small while guaranteeing no restored set points at a missing exercise (including
 * `imp_*` ones invented by a CSV import, which exist on no other install).
 */
@Serializable
data class BackupFile(
    /** Identifies the file as ours, so restoring some other JSON fails with a clear message. */
    val format: String = FORMAT,
    val version: Int = VERSION,
    @SerialName("created_at_ms") val createdAtMs: Long = 0,
    val settings: BackupSettings = BackupSettings(),
    val plan: List<BackupPlanItem> = emptyList(),
    val exercises: List<BackupExercise> = emptyList(),
    val sessions: List<BackupSession> = emptyList(),
    @SerialName("session_sets") val sessionSets: List<BackupSessionSet> = emptyList(),
    val logs: List<BackupLog> = emptyList(),
    val targets: List<BackupTarget> = emptyList(),
) {
    /** Everything a restore would write — what the confirmation prompt should quote. */
    val totalRecords: Int
        get() = sessions.size + sessionSets.size + logs.size + targets.size + plan.size + exercises.size

    companion object {
        const val FORMAT = "forge.backup"

        /**
         * Bump only for a *breaking* change. Adding a field with a default is not breaking —
         * [BackupRepo] decodes with `ignoreUnknownKeys`, and every field here has a default, so an
         * older file simply restores without the newer data.
         */
        const val VERSION = 1
    }
}

@Serializable
data class BackupSettings(
    val units: String = "kg",
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val autorest: Boolean = true,
    @SerialName("rest_default") val restDefault: Int = 60,
    val intensity: String = "Moderate",
    val onboarded: Boolean = true,
    @SerialName("ai_model") val aiModel: String = "claude-opus-5",
)

@Serializable
data class BackupPlanItem(val id: String, val sets: Int, val reps: String)

@Serializable
data class BackupExercise(
    val id: String,
    val name: String,
    val muscle: String,
    val equipment: String,
    val level: String,
    val target: String,
    @SerialName("secondary_muscles") val secondaryMuscles: String,
    val reps: String,
    @SerialName("media_id") val mediaId: String? = null,
    val cues: String = "",
    val curated: Boolean = false,
)

@Serializable
data class BackupSession(
    val id: String,
    @SerialName("date_ms") val dateMs: Long,
    val name: String,
    @SerialName("duration_sec") val durationSec: Int,
    @SerialName("total_sets") val totalSets: Int,
    val volume: Int,
    val prs: Int,
)

@Serializable
data class BackupSessionSet(
    @SerialName("session_id") val sessionId: String,
    @SerialName("exercise_id") val exerciseId: String,
    val name: String,
    val weight: Double,
    val reps: Int,
)

@Serializable
data class BackupLog(
    @SerialName("exercise_id") val exerciseId: String,
    val weight: Double,
    val reps: Int,
    @SerialName("date_ms") val dateMs: Long,
)

@Serializable
data class BackupTarget(
    val id: String,
    @SerialName("ex_id") val exId: String,
    val target: Double,
    val start: Double,
    @SerialName("start_d_ms") val startDMs: Long,
    val weeks: Int,
)

sealed class BackupParseResult {
    data class Success(val backup: BackupFile) : BackupParseResult()
    data class Failure(val message: String) : BackupParseResult()
}

/**
 * `isLenient = false` on purpose: a backup is the user's only copy, so silently accepting a
 * malformed file and restoring a partial one is worse than refusing it.
 */
internal val backupJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

fun encodeBackup(backup: BackupFile): String = backupJson.encodeToString(BackupFile.serializer(), backup)

/**
 * Parses and validates a backup file. Never throws — a corrupted or unrelated file has to surface
 * as a message the user can act on, not a crash on the restore path.
 */
fun parseBackup(text: String): BackupParseResult {
    if (text.isBlank()) return BackupParseResult.Failure("That file is empty.")

    val decoded = runCatching { backupJson.decodeFromString(BackupFile.serializer(), text) }.getOrNull()
        ?: return BackupParseResult.Failure(
            "That doesn't look like a Forge backup. Pick a .json file created by \"Back up\" — " +
                "to bring in workouts from another app, use Import instead.",
        )

    if (decoded.format != BackupFile.FORMAT) {
        return BackupParseResult.Failure(
            "That JSON isn't a Forge backup (it says \"${decoded.format.ifBlank { "nothing" }}\").",
        )
    }
    if (decoded.version > BackupFile.VERSION) {
        return BackupParseResult.Failure(
            "That backup was made by a newer version of Forge (format ${decoded.version}, this build " +
                "reads ${BackupFile.VERSION}). Update the app and try again.",
        )
    }
    return BackupParseResult.Success(decoded)
}
