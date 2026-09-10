package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import org.junit.Test

class TrendTest {
    /**
     * analyzeTrend buckets by [floorDayLocal], which reads the JVM's default timezone, so tests
     * can't use the flat `n * DAY_MS` arithmetic that StreakTest uses. Building each timestamp at
     * noon local via Calendar.add keeps consecutive days on distinct local dates in any timezone,
     * DST transitions included (a ±1h shift still lands inside the same day).
     */
    private fun day(daysAgo: Int): Long {
        val c = Calendar.getInstance()
        c.set(2026, Calendar.MARCH, 2, 12, 0, 0) // Mon 2 Mar 2026, noon local
        c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.DAY_OF_MONTH, -daysAgo)
        return c.timeInMillis
    }

    /** One set per day, oldest first — `weights[0]` is the earliest day. */
    private fun weighted(vararg weights: Double): List<LoggedSet> =
        weights.mapIndexed { i, w -> LoggedSet("bench", w, 5, day(weights.size - 1 - i)) }

    /** One set per day, oldest first — `reps[0]` is the earliest day. */
    private fun bodyweight(vararg reps: Int): List<LoggedSet> =
        reps.mapIndexed { i, r -> LoggedSet("pullup", 0.0, r, day(reps.size - 1 - i)) }

    // --- insufficient data ---

    @Test fun empty_is_insufficient() {
        val t = analyzeTrend(emptyList(), bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.INSUFFICIENT_DATA)
        assertThat(t.changePct).isNull()
    }

    @Test fun two_days_is_insufficient() {
        val t = analyzeTrend(weighted(100.0, 120.0), bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.INSUFFICIENT_DATA)
    }

    /** Points are per-day, not per-set: many sets on one day are still a single data point. */
    @Test fun many_sets_on_one_day_is_still_one_point() {
        val d = day(0)
        val sets = listOf(
            LoggedSet("bench", 100.0, 5, d),
            LoggedSet("bench", 110.0, 5, d + 60_000),
            LoggedSet("bench", 120.0, 5, d + 120_000),
            LoggedSet("bench", 130.0, 5, d + 180_000),
        )
        assertThat(analyzeTrend(sets, bodyweight = false).verdict)
            .isEqualTo(TrendVerdict.INSUFFICIENT_DATA)
    }

    /** A zero first point can't produce a percentage — guarded rather than divided by zero. */
    @Test fun zero_first_point_is_insufficient() {
        val t = analyzeTrend(weighted(0.0, 50.0, 60.0), bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.INSUFFICIENT_DATA)
        assertThat(t.changePct).isNull()
    }

    // --- verdicts ---

    @Test fun weighted_gain_is_improving() {
        val t = analyzeTrend(weighted(100.0, 105.0, 110.0), bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.IMPROVING)
        assertThat(t.changePct!!).isWithin(1e-9).of(10.0)
        assertThat(t.message).contains("Up 10%")
        assertThat(t.message).contains("last 3 sessions")
    }

    @Test fun weighted_loss_is_declining() {
        val t = analyzeTrend(weighted(100.0, 95.0, 90.0), bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.DECLINING)
        assertThat(t.changePct!!).isWithin(1e-9).of(-10.0)
        assertThat(t.message).contains("Down 10%") // message reports the magnitude, unsigned
    }

    @Test fun small_change_is_plateaued() {
        val t = analyzeTrend(weighted(100.0, 101.0, 102.0), bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.PLATEAUED)
        assertThat(t.changePct!!).isWithin(1e-9).of(2.0)
    }

    @Test fun no_change_at_all_is_plateaued() {
        val t = analyzeTrend(weighted(100.0, 100.0, 100.0), bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.PLATEAUED)
        assertThat(t.changePct!!).isWithin(1e-9).of(0.0)
    }

    // --- threshold boundaries (±5% inclusive) ---

    @Test fun exactly_plus_five_percent_is_improving() {
        val t = analyzeTrend(weighted(100.0, 100.0, 105.0), bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.IMPROVING)
    }

    @Test fun exactly_minus_five_percent_is_declining() {
        val t = analyzeTrend(weighted(100.0, 100.0, 95.0), bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.DECLINING)
    }

    @Test fun just_under_plus_five_percent_is_plateaued() {
        val t = analyzeTrend(weighted(100.0, 100.0, 104.0), bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.PLATEAUED)
    }

    @Test fun just_under_minus_five_percent_is_plateaued() {
        val t = analyzeTrend(weighted(100.0, 100.0, 96.0), bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.PLATEAUED)
    }

    // --- per-day point selection ---

    /** Within a day the heaviest set wins, so a lighter back-off set doesn't drag the trend down. */
    @Test fun day_point_is_the_heaviest_set_not_the_last() {
        val sets = listOf(
            LoggedSet("bench", 100.0, 5, day(2)),
            LoggedSet("bench", 100.0, 5, day(1)),
            LoggedSet("bench", 120.0, 3, day(0)),
            LoggedSet("bench", 60.0, 12, day(0) + 60_000), // back-off set, logged later same day
        )
        val t = analyzeTrend(sets, bodyweight = false)
        assertThat(t.verdict).isEqualTo(TrendVerdict.IMPROVING)
        assertThat(t.changePct!!).isWithin(1e-9).of(20.0)
    }

    /** Bodyweight lifts have no weight to compare — the day's point is its best rep count. */
    @Test fun bodyweight_uses_max_reps() {
        val t = analyzeTrend(bodyweight(10, 12, 15), bodyweight = true)
        assertThat(t.verdict).isEqualTo(TrendVerdict.IMPROVING)
        assertThat(t.changePct!!).isWithin(1e-9).of(50.0)
    }

    @Test fun bodyweight_ignores_weight_field_entirely() {
        val sets = listOf(
            LoggedSet("pullup", 999.0, 10, day(2)),
            LoggedSet("pullup", 0.0, 10, day(1)),
            LoggedSet("pullup", 0.0, 8, day(0)),
        )
        val t = analyzeTrend(sets, bodyweight = true)
        assertThat(t.verdict).isEqualTo(TrendVerdict.DECLINING)
        assertThat(t.changePct!!).isWithin(1e-9).of(-20.0)
    }

    /** Plateau copy differs by lift type: you can't add weight to a pull-up. */
    @Test fun plateau_message_is_lift_type_specific() {
        assertThat(analyzeTrend(weighted(100.0, 100.0, 100.0), bodyweight = false).message)
            .contains("weight or a rep")
        assertThat(analyzeTrend(bodyweight(10, 10, 10), bodyweight = true).message)
            .contains("a rep")
    }

    // --- window ---

    /** Only the most recent maxPoints days count, so old history can't skew a current read. */
    @Test fun only_the_last_six_days_count_by_default() {
        // 8 days: two heavy days long ago, then a flat-then-up run of six.
        val t = analyzeTrend(
            weighted(200.0, 200.0, 100.0, 100.0, 100.0, 100.0, 100.0, 110.0),
            bodyweight = false,
        )
        assertThat(t.verdict).isEqualTo(TrendVerdict.IMPROVING) // not DECLINING off the 200s
        assertThat(t.changePct!!).isWithin(1e-9).of(10.0)
        assertThat(t.message).contains("last 6 sessions")
    }

    @Test fun max_points_window_is_configurable() {
        val t = analyzeTrend(weighted(100.0, 100.0, 100.0, 200.0), bodyweight = false, maxPoints = 3)
        assertThat(t.changePct!!).isWithin(1e-9).of(100.0) // window starts at the 2nd 100
        assertThat(t.message).contains("last 3 sessions")
    }

    /** Input order is not guaranteed by the repo layer; days are sorted before comparing. */
    @Test fun unordered_input_is_sorted_by_day() {
        val ordered = weighted(100.0, 110.0, 120.0)
        val shuffled = listOf(ordered[2], ordered[0], ordered[1])
        assertThat(analyzeTrend(shuffled, bodyweight = false))
            .isEqualTo(analyzeTrend(ordered, bodyweight = false))
    }
}
