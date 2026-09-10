package com.getfit.data.importexport

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The CSV reader is hand-rolled specifically because real workout-app exports quote fields that
 * contain commas (exercise and workout names routinely do), so a `.split(",")` would corrupt them.
 * These tests pin the quoting rules that justify keeping it hand-rolled.
 */
class CsvTest {

    @Test fun parses_plain_rows() {
        val t = Csv.parse("a,b,c\n1,2,3\n")
        assertThat(t).containsExactly(listOf("a", "b", "c"), listOf("1", "2", "3")).inOrder()
    }

    /** The whole reason this isn't split(","). */
    @Test fun quoted_field_keeps_its_commas() {
        val t = Csv.parse("name,reps\n\"Squat, Barbell\",5\n")
        assertThat(t[1]).containsExactly("Squat, Barbell", "5").inOrder()
    }

    @Test fun doubled_quotes_become_one_literal_quote() {
        val t = Csv.parse("name\n\"He said \"\"go\"\"\"\n")
        assertThat(t[1]).containsExactly("He said \"go\"")
    }

    @Test fun quoted_field_may_contain_a_newline() {
        val t = Csv.parse("name,note\n\"Row\",\"line1\nline2\"\n")
        assertThat(t).hasSize(2)
        assertThat(t[1]).containsExactly("Row", "line1\nline2").inOrder()
    }

    @Test fun handles_crlf_line_endings() {
        val t = Csv.parse("a,b\r\n1,2\r\n")
        assertThat(t).containsExactly(listOf("a", "b"), listOf("1", "2")).inOrder()
    }

    /** A file that doesn't end in a newline must not lose its last row. */
    @Test fun final_row_without_trailing_newline_is_kept() {
        val t = Csv.parse("a,b\n1,2")
        assertThat(t).hasSize(2)
        assertThat(t[1]).containsExactly("1", "2").inOrder()
    }

    @Test fun blank_lines_are_dropped() {
        val t = Csv.parse("a,b\n\n1,2\n\n\n")
        assertThat(t).containsExactly(listOf("a", "b"), listOf("1", "2")).inOrder()
    }

    @Test fun empty_input_is_empty() {
        assertThat(Csv.parse("")).isEmpty()
    }

    @Test fun empty_fields_are_preserved_within_a_row() {
        val t = Csv.parse("a,b,c\n1,,3\n")
        assertThat(t[1]).containsExactly("1", "", "3").inOrder()
    }

    // --- writing ---

    @Test fun writeField_only_quotes_when_it_has_to() {
        assertThat(Csv.writeField("plain")).isEqualTo("plain")
        assertThat(Csv.writeField("has,comma")).isEqualTo("\"has,comma\"")
        assertThat(Csv.writeField("has\"quote")).isEqualTo("\"has\"\"quote\"")
        assertThat(Csv.writeField("has\nnewline")).isEqualTo("\"has\nnewline\"")
    }

    /** Export then re-import must survive the values that forced quoting in the first place. */
    @Test fun write_then_parse_round_trips_awkward_values() {
        val rows = listOf(
            listOf("Exercise", "Note"),
            listOf("Squat, Barbell", "He said \"deep\""),
            listOf("Row", "line1\nline2"),
        )
        val text = rows.joinToString("\n") { Csv.writeRow(it) } + "\n"
        assertThat(Csv.parse(text)).isEqualTo(rows)
    }
}
