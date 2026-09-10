package com.getfit.data.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The backup file is the user's only copy of their history, so the format's failure modes matter as
 * much as its happy path: a wrong file picked from a storage browser has to say so, and a truncated
 * or unrelated file must never restore *partially*.
 */
class BackupFormatTest {

    private fun sample() = BackupFile(
        createdAtMs = 1_700_000_000_000,
        settings = BackupSettings(units = "lb", restDefault = 90, intensity = "Hard", sound = false),
        plan = listOf(BackupPlanItem("bench", 4, "8")),
        exercises = listOf(
            BackupExercise(
                id = "imp_zercher_squat", name = "Zercher Squat", muscle = "Legs", equipment = "Barbell",
                level = "Advanced", target = "Quads", secondaryMuscles = "Glutes", reps = "5",
                mediaId = null, cues = "brace||stand", curated = false,
            ),
        ),
        sessions = listOf(BackupSession("s1", 1_699_000_000_000, "Push Day", 3600, 12, 5400, 2)),
        sessionSets = listOf(BackupSessionSet("s1", "bench", "Bench Press", 100.0, 5)),
        logs = listOf(BackupLog("bench", 100.0, 5, 1_699_000_000_000)),
        targets = listOf(BackupTarget("t1", "bench", 120.0, 100.0, 1_699_000_000_000, 8)),
    )

    @Test fun round_trips_every_section() {
        val decoded = parseBackup(encodeBackup(sample()))

        assertThat(decoded).isInstanceOf(BackupParseResult.Success::class.java)
        assertThat((decoded as BackupParseResult.Success).backup).isEqualTo(sample())
    }

    /** Settings are easy to drop silently, and a restored app with the wrong units looks broken. */
    @Test fun settings_survive_the_round_trip() {
        val s = ((parseBackup(encodeBackup(sample())) as BackupParseResult.Success)).backup.settings
        assertThat(s.units).isEqualTo("lb")
        assertThat(s.restDefault).isEqualTo(90)
        assertThat(s.intensity).isEqualTo("Hard")
        assertThat(s.sound).isFalse()
    }

    @Test fun total_records_counts_everything_a_restore_writes() {
        // 1 session + 1 set + 1 log + 1 target + 1 plan item + 1 exercise
        assertThat(sample().totalRecords).isEqualTo(6)
    }

    // --- rejection ---

    @Test fun empty_file_is_rejected() {
        val r = parseBackup("") as BackupParseResult.Failure
        assertThat(r.message).contains("empty")
    }

    @Test fun non_json_is_rejected_with_a_useful_message() {
        val r = parseBackup("Date,Exercise,Reps\n2024-01-01,Bench,5\n") as BackupParseResult.Failure
        // Picking a CSV export here is the most likely mistake, so the message names Import.
        assertThat(r.message).contains("Import")
    }

    @Test fun unrelated_json_is_rejected() {
        val r = parseBackup("""{"format":"something.else","version":1}""")
        assertThat(r).isInstanceOf(BackupParseResult.Failure::class.java)
    }

    /** A half-written file must fail outright rather than restore the records it managed to hold. */
    @Test fun truncated_file_is_rejected_not_partially_restored() {
        val truncated = encodeBackup(sample()).let { it.substring(0, it.length / 2) }
        assertThat(parseBackup(truncated)).isInstanceOf(BackupParseResult.Failure::class.java)
    }

    @Test fun a_newer_format_version_is_refused_rather_than_misread() {
        val newer = encodeBackup(sample()).replace("\"version\": 1", "\"version\": 99")
        val r = parseBackup(newer) as BackupParseResult.Failure
        assertThat(r.message).contains("newer version")
    }

    /**
     * Adding a field must not invalidate existing backups — every field has a default and decoding
     * ignores unknown keys, which is why [BackupFile.VERSION] only moves for breaking changes.
     */
    @Test fun older_and_unknown_fields_are_tolerated() {
        val minimal = """{"format":"forge.backup","version":1,"created_at_ms":123}"""
        val withFuture = """{"format":"forge.backup","version":1,"created_at_ms":123,"habits":[1,2]}"""

        val a = parseBackup(minimal) as BackupParseResult.Success
        val b = parseBackup(withFuture) as BackupParseResult.Success

        assertThat(a.backup.sessions).isEmpty()
        assertThat(a.backup.totalRecords).isEqualTo(0)
        assertThat(b.backup.createdAtMs).isEqualTo(123)
    }

    /** Snake_case is the wire contract; renaming a Kotlin property must not silently change it. */
    @Test fun wire_keys_are_stable() {
        val text = encodeBackup(sample())
        listOf("created_at_ms", "session_sets", "exercise_id", "date_ms", "rest_default", "start_d_ms")
            .forEach { assertThat(text).contains("\"$it\"") }
    }
}
