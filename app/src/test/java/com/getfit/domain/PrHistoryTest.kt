package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrHistoryTest {
    private val d = 86_400_000L

    @Test fun emits_an_event_each_time_the_best_moves_newest_first() {
        val logs = listOf(
            LoggedSet("bench", 80.0, 8, 1 * d),   // first ever: not a PR
            LoggedSet("bench", 80.0, 8, 2 * d),   // equal: not a PR
            LoggedSet("bench", 80.0, 10, 3 * d),  // same weight, more reps: PR
            LoggedSet("bench", 85.0, 5, 4 * d),   // heavier: PR
            LoggedSet("bench", 82.5, 12, 5 * d),  // lighter: not a PR (weight rules)
        )
        val h = prHistory(logs) { false }
        assertThat(h.map { it.dateMs }).containsExactly(4 * d, 3 * d).inOrder()
        assertThat(h[0].e1rm).isEqualTo(99)   // 85 * (1 + 5/30) = 99.17
    }

    @Test fun bodyweight_tracks_reps_only() {
        val logs = listOf(LoggedSet("pullup", 0.0, 6, 1 * d), LoggedSet("pullup", 0.0, 8, 2 * d), LoggedSet("pullup", 0.0, 8, 3 * d))
        val h = prHistory(logs) { true }
        assertThat(h).hasSize(1)
        assertThat(h[0].reps).isEqualTo(8)
        assertThat(h[0].bodyweight).isTrue()
        assertThat(h[0].e1rm).isEqualTo(0)
    }

    @Test fun exercises_are_independent_and_interleave_by_date() {
        val logs = listOf(
            LoggedSet("a", 50.0, 5, 1 * d), LoggedSet("b", 20.0, 5, 2 * d),
            LoggedSet("b", 25.0, 5, 3 * d), LoggedSet("a", 55.0, 5, 4 * d),
        )
        assertThat(prHistory(logs) { false }.map { it.exerciseId }).containsExactly("a", "b").inOrder()
    }

    @Test fun progress_series_is_top_set_per_day_oldest_first() {
        val logs = listOf(
            LoggedSet("bench", 80.0, 8, 1 * d), LoggedSet("bench", 85.0, 3, 1 * d + 1000),
            LoggedSet("bench", 82.5, 8, 3 * d),
        )
        val s = progressSeries(logs, bodyweight = false)
        assertThat(s.map { it.value }).containsExactly(85.0, 82.5).inOrder()
        assertThat(s[0].e1rm).isEqualTo(94)   // 85 * 1.1
        assertThat(progressSeries(logs, bodyweight = true).map { it.value }).containsExactly(8.0, 8.0).inOrder()
    }
}
