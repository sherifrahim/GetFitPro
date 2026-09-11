package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutDetailTest {
    private val day = 86_400_000L

    @Test fun weighted_delta_compares_top_weight_with_previous_day() {
        val sets = listOf(SessionSetRow("bench", "Bench", 80.0, 8), SessionSetRow("bench", "Bench", 85.0, 5))
        val logs = listOf(
            LoggedSet("bench", 82.5, 6, 10 * day), LoggedSet("bench", 80.0, 8, 10 * day), // previous session
            LoggedSet("bench", 70.0, 10, 3 * day),                                        // older, ignored
            LoggedSet("bench", 85.0, 5, 12 * day),                                        // this session
        )
        val d = exerciseDeltas(12 * day, sets, logs) { false }.getValue("bench")
        assertThat(d.topThis).isEqualTo(85.0)
        assertThat(d.topPrev).isEqualTo(82.5)
        assertThat(d.delta).isEqualTo(2.5)
    }

    @Test fun bodyweight_delta_compares_reps() {
        val sets = listOf(SessionSetRow("pullup", "Pull-Up", 0.0, 8))
        val logs = listOf(LoggedSet("pullup", 0.0, 6, 5 * day), LoggedSet("pullup", 0.0, 8, 9 * day))
        val d = exerciseDeltas(9 * day, sets, logs) { true }.getValue("pullup")
        assertThat(d.bodyweight).isTrue()
        assertThat(d.delta).isEqualTo(2.0)
    }

    @Test fun first_ever_session_has_no_previous() {
        val d = exerciseDeltas(1 * day, listOf(SessionSetRow("squat", "Squat", 100.0, 5)), emptyList()) { false }.getValue("squat")
        assertThat(d.topPrev).isNull()
        assertThat(d.delta).isNull()
    }

    @Test fun muscle_split_is_percent_of_sets_largest_first() {
        val sets = listOf(
            SessionSetRow("a", "", 0.0, 1), SessionSetRow("a", "", 0.0, 1), SessionSetRow("a", "", 0.0, 1),
            SessionSetRow("b", "", 0.0, 1), SessionSetRow("c", "", 0.0, 1),
        )
        val split = muscleSplit(sets) { when (it) { "a" -> "Chest"; "b" -> "Back"; else -> null } }
        assertThat(split).containsExactly("Chest" to 60, "Back" to 20, "Other" to 20).inOrder()
        assertThat(muscleSplit(emptyList()) { null }).isEmpty()
    }
}
