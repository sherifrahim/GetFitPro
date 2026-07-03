package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeekAggTest {
    private val DAY = DAY_MS

    @Test fun buckets_volume_by_day_from_monday() {
        val weekStart = 0L
        val sessions = listOf(
            SessionRecord("a", weekStart + 0 * DAY + 3_600_000, "Push", 1800, 4, 1000, 0), // Mon
            SessionRecord("b", weekStart + 2 * DAY + 3_600_000, "Push", 1800, 4, 500, 0),  // Wed
        )
        val agg = weekAgg(sessions, weekStart)
        assertThat(agg.dayVolume[0]).isEqualTo(1000)
        assertThat(agg.dayVolume[2]).isEqualTo(500)
        assertThat(agg.dayHit[0]).isTrue()
        assertThat(agg.dayHit[1]).isFalse()
        assertThat(agg.workouts).isEqualTo(2)
        assertThat(agg.totalVolume).isEqualTo(1500)
    }

    @Test fun ignores_sessions_before_week_start() {
        val weekStart = 10 * DAY
        val sessions = listOf(
            SessionRecord("old", weekStart - DAY, "Push", 1800, 4, 999, 0),
            SessionRecord("new", weekStart + DAY, "Push", 1800, 4, 200, 0),
        )
        val agg = weekAgg(sessions, weekStart)
        assertThat(agg.workouts).isEqualTo(1)
        assertThat(agg.totalVolume).isEqualTo(200)
    }
}
