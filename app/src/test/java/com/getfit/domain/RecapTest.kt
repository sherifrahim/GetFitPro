package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecapTest {
    private val d = DAY_MS
    private val floor: (Long) -> Long = { it - it % DAY_MS }

    @Test fun aggregates_only_sessions_inside_the_period() {
        val sessions = listOf(
            RecapSession("s1", 10 * d, 3600, 1000, 300), RecapSession("s2", 11 * d, 1800, 500, 200),
            RecapSession("s3", 12 * d, 1800, 500, 200), RecapSession("old", 2 * d, 9999, 9999, 999),
        )
        val sets = listOf("s1" to "bench", "s1" to "bench", "s2" to "squat", "s3" to "bench", "old" to "squat")
        val logs = listOf(
            LoggedSet("bench", 80.0, 8, 2 * d), LoggedSet("bench", 85.0, 8, 10 * d), LoggedSet("bench", 85.0, 8, 12 * d),
            LoggedSet("squat", 100.0, 5, 11 * d),
        )
        val r = recap(sessions, sets, logs, 9 * d, 13 * d, { if (it == "bench") "Chest" else "Legs" }, { it.uppercase() }, { false }, floor)
        assertThat(r.workouts).isEqualTo(3)
        assertThat(r.durationSec).isEqualTo(7200)
        assertThat(r.volumeKg).isEqualTo(2000.0)
        assertThat(r.sets).isEqualTo(4)
        assertThat(r.bestStreak).isEqualTo(3)
        assertThat(r.activeDays).isEqualTo(3)
        assertThat(r.topMuscle).isEqualTo("Chest"); assertThat(r.topMuscleSets).isEqualTo(3)
        assertThat(r.topExercise).isEqualTo("BENCH")
        assertThat(r.calories).isEqualTo(700)
        // bench 80 -> 85 on day 10 is a PR inside the range; squat's first set is not a PR.
        assertThat(r.prs).isEqualTo(1)
    }

    @Test fun empty_period() {
        val r = recap(emptyList(), emptyList(), emptyList(), 0, 30 * d, { null }, { null }, { false }, floor)
        assertThat(r.workouts).isEqualTo(0)
        assertThat(r.bestStreak).isEqualTo(0)
        assertThat(r.topMuscle).isNull()
        assertThat(r.avgPerWeek).isEqualTo(0.0)
    }

    @Test fun streak_breaks_on_a_gap() {
        val sessions = listOf(RecapSession("a", 1 * d, 0, 0, 0), RecapSession("b", 2 * d, 0, 0, 0), RecapSession("c", 4 * d, 0, 0, 0), RecapSession("d", 4 * d + 1000, 0, 0, 0))
        val r = recap(sessions, emptyList(), emptyList(), 0, 10 * d, { null }, { null }, { false }, floor)
        assertThat(r.bestStreak).isEqualTo(2)
        assertThat(r.activeDays).isEqualTo(3)
    }
}
