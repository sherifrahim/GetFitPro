package com.getfit.data.importexport

import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import java.util.TimeZone
import org.junit.Test

/**
 * Import parsing matches column NAMES rather than fixed positions, because real exports vary in
 * column order and gain columns over time. That flexibility is the point, but it is also easy to
 * break silently — a keyword that starts matching the wrong column produces a plausible-looking
 * import with wrong numbers in it. These tests pin the header shapes of each supported app.
 */
class ImportParserTest {

    // Realistic header rows for each supported export.
    private val hevyHeader =
        "title,start_time,end_time,description,exercise_title,superset_id,exercise_notes," +
            "set_index,set_type,weight_kg,reps,distance_km,duration_seconds,rpe"
    private val strongHeader =
        "Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes"
    private val fitNotesHeader =
        "Date,Exercise,Category,Weight,Weight Unit,Reps,Distance,Distance Unit,Time,Comment"
    private val forgeHeader =
        "Date,Workout Name,Exercise Name,Weight,Weight Unit,Reps"

    private fun localMs(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Long {
        val c = Calendar.getInstance()
        c.set(y, m, d, h, min, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    // --- format detection ---

    @Test fun detects_hevy() {
        val csv = "$hevyHeader\nPush,2024-03-02 10:00:00,,,Bench Press,,,0,normal,100,5,,,\n"
        assertThat(parseImportCsv(csv).format).isEqualTo(SourceFormat.HEVY)
    }

    @Test fun detects_strong() {
        val csv = "$strongHeader\n2024-03-02 10:00,Push,60m,Bench Press,1,100,5,,,\n"
        assertThat(parseImportCsv(csv).format).isEqualTo(SourceFormat.STRONG)
    }

    @Test fun detects_fitnotes() {
        val csv = "$fitNotesHeader\n2024-03-02,Bench Press,Chest,100,kgs,5,,,,\n"
        assertThat(parseImportCsv(csv).format).isEqualTo(SourceFormat.FITNOTES)
    }

    @Test fun detects_forge() {
        val csv = "$forgeHeader\n2024-03-02,Push,Bench Press,100,kg,5\n"
        assertThat(parseImportCsv(csv).format).isEqualTo(SourceFormat.FORGE)
    }

    @Test fun unknown_header_falls_back_to_generic_but_still_parses() {
        val csv = "when,what,load,count\n2024-03-02,Bench Press,100,5\n"
        val p = parseImportCsv(csv)
        assertThat(p.format).isEqualTo(SourceFormat.GENERIC)
        // "what" isn't an exercise keyword, so nothing is importable — but it must not crash.
        assertThat(p.rows).isEmpty()
    }

    // --- column matching ---

    @Test fun hevy_row_maps_to_the_right_columns() {
        val csv = "$hevyHeader\nPush Day,2024-03-02 10:00:00,,,Bench Press,,,0,normal,100,5,,,\n"
        val row = parseImportCsv(csv).rows.single()
        assertThat(row.exerciseName).isEqualTo("Bench Press")
        assertThat(row.weightKg).isWithin(1e-9).of(100.0)
        assertThat(row.reps).isEqualTo(5)
        assertThat(row.sessionName).isEqualTo("Push Day")
    }

    /**
     * Hevy has both `title` and `exercise_title`. The workout-name lookup must land on `title` and
     * not be captured by the substring in `exercise_title`.
     */
    @Test fun exercise_title_does_not_steal_the_workout_name_column() {
        val csv = "$hevyHeader\nLeg Day,2024-03-02 10:00:00,,,Back Squat,,,0,normal,140,3,,,\n"
        val row = parseImportCsv(csv).rows.single()
        assertThat(row.sessionName).isEqualTo("Leg Day")
        assertThat(row.exerciseName).isEqualTo("Back Squat")
    }

    /** Column order is not guaranteed; only names are. */
    @Test fun column_order_does_not_matter() {
        val a = parseImportCsv("Exercise Name,Reps,Weight,Weight Unit\nBench Press,5,100,kg\n")
        val b = parseImportCsv("Weight Unit,Weight,Reps,Exercise Name\nkg,100,5,Bench Press\n")
        assertThat(b.rows.single().exerciseName).isEqualTo(a.rows.single().exerciseName)
        assertThat(b.rows.single().weightKg).isEqualTo(a.rows.single().weightKg)
        assertThat(b.rows.single().reps).isEqualTo(a.rows.single().reps)
    }

    @Test fun unknown_extra_columns_are_ignored() {
        val csv = "Exercise Name,Reps,Weight,Weight Unit,Mood,Playlist\nBench Press,5,100,kg,good,rock\n"
        assertThat(parseImportCsv(csv).rows.single().reps).isEqualTo(5)
    }

    @Test fun without_an_exercise_or_reps_column_nothing_is_imported() {
        assertThat(parseImportCsv("Date,Weight\n2024-03-02,100\n").rows).isEmpty()
    }

    @Test fun header_only_file_is_empty() {
        assertThat(parseImportCsv("$forgeHeader\n").rows).isEmpty()
    }

    // --- units ---

    @Test fun pounds_are_converted_to_kilos() {
        val csv = "$fitNotesHeader\n2024-03-02,Bench Press,Chest,225,lbs,5,,,,\n"
        assertThat(parseImportCsv(csv).rows.single().weightKg).isWithin(1e-6).of(225 * 0.45359237)
    }

    @Test fun explicit_kg_unit_is_left_alone() {
        val csv = "$fitNotesHeader\n2024-03-02,Bench Press,Chest,100,kg,5,,,,\n"
        assertThat(parseImportCsv(csv).rows.single().weightKg).isWithin(1e-9).of(100.0)
    }

    /** Hevy has no unit column; the `weight_kg` header name is the only hint that it's metric. */
    @Test fun kg_in_the_column_name_is_treated_as_metric() {
        val csv = "$hevyHeader\nPush,2024-03-02 10:00:00,,,Bench Press,,,0,normal,100,5,,,\n"
        assertThat(parseImportCsv(csv).rows.single().weightKg).isWithin(1e-9).of(100.0)
    }

    @Test fun bodyweight_rows_with_no_weight_are_zero_not_skipped() {
        val csv = "$forgeHeader\n2024-03-02,Push,Pull Up,,,12\n"
        val row = parseImportCsv(csv).rows.single()
        assertThat(row.weightKg).isEqualTo(0.0)
        assertThat(row.reps).isEqualTo(12)
    }

    // --- skipping ---

    @Test fun rows_without_an_exercise_name_are_skipped_not_fatal() {
        val csv = "$forgeHeader\n2024-03-02,Push,,100,kg,5\n2024-03-02,Push,Bench Press,100,kg,5\n"
        val p = parseImportCsv(csv)
        assertThat(p.rows).hasSize(1)
        assertThat(p.skippedRows).isEqualTo(1)
    }

    @Test fun rows_with_unusable_reps_are_skipped() {
        val csv = "$forgeHeader\n" +
            "2024-03-02,Push,Bench Press,100,kg,0\n" +   // zero reps
            "2024-03-02,Push,Bench Press,100,kg,abc\n" + // non-numeric
            "2024-03-02,Push,Bench Press,100,kg,5\n"     // good
        val p = parseImportCsv(csv)
        assertThat(p.rows).hasSize(1)
        assertThat(p.skippedRows).isEqualTo(2)
    }

    @Test fun decimal_reps_are_rounded() {
        val csv = "$forgeHeader\n2024-03-02,Push,Bench Press,100,kg,5.0\n"
        assertThat(parseImportCsv(csv).rows.single().reps).isEqualTo(5)
    }

    // --- session grouping ---

    @Test fun rows_sharing_name_and_date_group_into_one_session() {
        val csv = "$forgeHeader\n" +
            "2024-03-02,Push,Bench Press,100,kg,5\n" +
            "2024-03-02,Push,Overhead Press,60,kg,8\n" +
            "2024-03-04,Pull,Barbell Row,80,kg,8\n"
        val keys = parseImportCsv(csv).rows.map { it.sessionKey }
        assertThat(keys[0]).isEqualTo(keys[1])
        assertThat(keys[2]).isNotEqualTo(keys[0])
    }

    /** FitNotes has no workout-name column, so the date alone has to carry the grouping. */
    @Test fun fitnotes_groups_by_date_alone() {
        val csv = "$fitNotesHeader\n" +
            "2024-03-02,Bench Press,Chest,100,kg,5,,,,\n" +
            "2024-03-02,Overhead Press,Shoulders,60,kg,8,,,,\n" +
            "2024-03-04,Barbell Row,Back,80,kg,8,,,,\n"
        val rows = parseImportCsv(csv).rows
        assertThat(rows[0].sessionKey).isEqualTo(rows[1].sessionKey)
        assertThat(rows[2].sessionKey).isNotEqualTo(rows[0].sessionKey)
        assertThat(rows[0].sessionName).isEqualTo("Imported workout")
    }

    // --- dates ---

    @Test fun parses_common_export_date_formats() {
        assertThat(parseFlexibleDate("2024-03-02 10:30:00", 0)).isEqualTo(localMs(2024, Calendar.MARCH, 2, 10, 30))
        assertThat(parseFlexibleDate("2024-03-02", 0)).isEqualTo(localMs(2024, Calendar.MARCH, 2))
        assertThat(parseFlexibleDate("02 Mar 2024, 10:30", 0)).isEqualTo(localMs(2024, Calendar.MARCH, 2, 10, 30))
    }

    @Test fun an_unparseable_date_falls_back_rather_than_failing_the_import() {
        assertThat(parseFlexibleDate("not a date", 12345L)).isEqualTo(12345L)
        assertThat(parseFlexibleDate("", 12345L)).isEqualTo(12345L)
    }

    /**
     * A trailing Z means UTC. Parsing it as local time silently shifts a workout by the offset,
     * which can move it to the wrong calendar day — and day boundaries drive streaks and the weekly
     * chart, so this is not cosmetic.
     */
    @Test fun iso_utc_timestamps_are_treated_as_utc() {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2024, Calendar.MARCH, 2, 10, 30, 0)
        }.timeInMillis
        assertThat(parseFlexibleDate("2024-03-02T10:30:00Z", 0)).isEqualTo(utc)
    }

    @Test fun a_bad_date_does_not_discard_the_row() {
        val csv = "$forgeHeader\nnot-a-date,Push,Bench Press,100,kg,5\n"
        val p = parseImportCsv(csv)
        assertThat(p.rows).hasSize(1)
        assertThat(p.rows.single().dateMs).isGreaterThan(0L)
    }
}
