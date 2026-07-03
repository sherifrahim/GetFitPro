package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StreakTest {
    private val DAY = DAY_MS

    @Test fun empty_is_zero() {
        assertThat(streakCount(emptyList(), now = 10 * DAY)).isEqualTo(0)
    }

    @Test fun today_and_yesterday_is_two() {
        val now = 10 * DAY
        assertThat(streakCount(listOf(now, now - DAY), now)).isEqualTo(2)
    }

    @Test fun missing_today_but_yesterday_counts() {
        val now = 10 * DAY
        assertThat(streakCount(listOf(now - DAY, now - 2 * DAY), now)).isEqualTo(2)
    }

    @Test fun gap_breaks_streak() {
        val now = 10 * DAY
        assertThat(streakCount(listOf(now, now - 2 * DAY), now)).isEqualTo(1)
    }
}
