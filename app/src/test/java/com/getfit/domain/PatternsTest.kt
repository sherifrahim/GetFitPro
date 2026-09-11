package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import org.junit.Test

class PatternsTest {

    /** Local-time timestamp builder, DST-safe (same approach as TrendTest). */
    private fun at(month: Int, day: Int, hour: Int = 18, year: Int = 2026): Long {
        val c = Calendar.getInstance()
        c.set(year, month, day, hour, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    // Sunday 8 Mar 2026, 20:00 — so "this week" is Mon 2 Mar .. Sun 8 Mar, and the 8-week window
    // opens on Mon 12 Jan.
    private val now = at(Calendar.MARCH, 8, 20)

    private fun session(ts: Long, durationSec: Int = 3600, sets: Int = 16, volume: Int = 4000) =
        SessionRecord("s$ts", ts, "Push", durationSec, sets, volume, 0)

    // --- window and weekly counts ---

    @Test fun empty_history_is_reported_as_such() {
        val r = analyzePatterns(emptyList(), now)
        assertThat(r.sessionsInWindow).isEqualTo(0)
        assertThat(describePatterns(r)).contains("No sessions")
    }

    @Test fun sessions_bucket_into_monday_aligned_weeks_oldest_first() {
        val r = analyzePatterns(
            listOf(
                session(at(Calendar.MARCH, 2)),     // Mon this week
                session(at(Calendar.MARCH, 4)),     // Wed this week
                session(at(Calendar.FEBRUARY, 23)), // Mon last week
            ),
            now,
        )
        assertThat(r.weeklyCounts).hasSize(8)
        assertThat(r.weeklyCounts.last()).isEqualTo(2)
        assertThat(r.weeklyCounts[6]).isEqualTo(1)
        assertThat(r.weeklyCounts.take(6)).containsExactly(0, 0, 0, 0, 0, 0)
    }

    /** The current week is in progress and must not count as skipped on a Tuesday. */
    @Test fun skipped_weeks_only_counts_completed_empty_weeks() {
        val r = analyzePatterns(listOf(session(at(Calendar.FEBRUARY, 23))), now)
        // 7 completed weeks in the window, one of them trained -> 6 skipped; current week excluded.
        assertThat(r.skippedWeeks).isEqualTo(6)
    }

    @Test fun sessions_outside_the_window_are_ignored() {
        val r = analyzePatterns(listOf(session(at(Calendar.JANUARY, 1))), now) // 10 weeks back
        assertThat(r.sessionsInWindow).isEqualTo(0)
    }

    // --- gaps and cadence ---

    @Test fun gaps_and_spacing_are_computed_between_trained_days() {
        val r = analyzePatterns(
            listOf(session(at(Calendar.MARCH, 2)), session(at(Calendar.MARCH, 3)), session(at(Calendar.MARCH, 6))),
            now,
        )
        assertThat(r.longestGapDays).isEqualTo(3)
        assertThat(r.avgDaysBetween).isWithin(1e-9).of(2.0) // gaps 1 and 3
        assertThat(r.backToBackDays).isEqualTo(1)
        assertThat(r.daysSinceLast).isEqualTo(2)             // Mar 6 -> Mar 8
    }

    /** Two sessions on one day are one trained day, not a zero-day gap. */
    @Test fun same_day_sessions_collapse_to_one_trained_day() {
        val r = analyzePatterns(
            listOf(session(at(Calendar.MARCH, 2, 7)), session(at(Calendar.MARCH, 2, 19)), session(at(Calendar.MARCH, 4))),
            now,
        )
        assertThat(r.avgDaysBetween).isWithin(1e-9).of(2.0)
        assertThat(r.backToBackDays).isEqualTo(0)
    }

    // --- day-of-week habits ---

    @Test fun favourite_and_never_trained_days_are_identified() {
        val r = analyzePatterns(
            listOf(
                session(at(Calendar.MARCH, 2)),     // Mon
                session(at(Calendar.MARCH, 4)),     // Wed
                session(at(Calendar.MARCH, 6)),     // Fri
                session(at(Calendar.FEBRUARY, 23)), // Mon
                session(at(Calendar.FEBRUARY, 25)), // Wed
            ),
            now,
        )
        assertThat(r.dayOfWeekCounts).containsExactly(2, 0, 2, 0, 1, 0, 0).inOrder()
        assertThat(r.favouriteDays).containsExactly("Mon", "Wed")
        assertThat(r.neverTrainedDays).containsExactly("Tue", "Thu", "Sat", "Sun")
    }

    /** Too little data to call a day "never trained" — three sessions can't establish a habit. */
    @Test fun never_trained_needs_enough_sessions() {
        val r = analyzePatterns(listOf(session(at(Calendar.MARCH, 2)), session(at(Calendar.MARCH, 4))), now)
        assertThat(r.neverTrainedDays).isEmpty()
    }

    // --- session shape ---

    @Test fun session_shape_averages() {
        val r = analyzePatterns(
            listOf(session(at(Calendar.MARCH, 2), 3600, 20), session(at(Calendar.MARCH, 4), 1800, 10)),
            now,
        )
        assertThat(r.avgDurationSec).isEqualTo(2700)
        assertThat(r.avgSetsPerSession).isWithin(1e-9).of(15.0)
        assertThat(r.avgMinutesPerSet).isWithin(1e-9).of(3.0) // both sessions pace at 3 min/set
    }

    @Test fun time_of_day_buckets() {
        val r = analyzePatterns(
            listOf(session(at(Calendar.MARCH, 2, 7)), session(at(Calendar.MARCH, 4, 19)), session(at(Calendar.MARCH, 6, 19))),
            now,
        )
        assertThat(r.timeOfDay["morning"]).isEqualTo(1)
        assertThat(r.timeOfDay["evening"]).isEqualTo(2)
        assertThat(describePatterns(r)).contains("evening")
    }

    // --- volume direction ---

    private fun fourCompletedWeeks(v3: Int, v4: Int, v5: Int, v6: Int): List<SessionRecord> = listOf(
        // completed-week indices 3..6 = weeks starting Mon 2 Feb, 9 Feb, 16 Feb, 23 Feb
        session(at(Calendar.FEBRUARY, 3), volume = v3),
        session(at(Calendar.FEBRUARY, 10), volume = v4),
        session(at(Calendar.FEBRUARY, 17), volume = v5),
        session(at(Calendar.FEBRUARY, 24), volume = v6),
    )

    @Test fun volume_rising_when_recent_two_weeks_beat_prior_two_by_ten_percent() {
        val r = analyzePatterns(fourCompletedWeeks(1000, 1000, 1200, 1200), now)
        assertThat(r.volumeDirection).isEqualTo(VolumeDirection.RISING)
    }

    @Test fun volume_falling() {
        val r = analyzePatterns(fourCompletedWeeks(1200, 1200, 1000, 1000), now)
        assertThat(r.volumeDirection).isEqualTo(VolumeDirection.FALLING)
    }

    @Test fun volume_flat_inside_ten_percent() {
        val r = analyzePatterns(fourCompletedWeeks(1000, 1000, 1050, 1050), now)
        assertThat(r.volumeDirection).isEqualTo(VolumeDirection.FLAT)
    }

    /** A rest week must read as "can't tell", not as a volume collapse. */
    @Test fun volume_direction_needs_four_consecutive_trained_weeks() {
        val r = analyzePatterns(fourCompletedWeeks(1000, 0, 1200, 1200).filter { it.volume > 0 }, now)
        assertThat(r.volumeDirection).isEqualTo(VolumeDirection.INSUFFICIENT_DATA)
    }

    // --- muscle balance ---

    private fun sets(muscle: String, n: Int, day: Int = 2) = List(n) { MuscleSet(at(Calendar.MARCH, day), muscle) }

    @Test fun neglected_groups_are_those_under_eight_percent_of_recent_sets() {
        val r = analyzePatterns(
            listOf(session(at(Calendar.MARCH, 2))), now,
            muscleSets = sets("Chest", 12) + sets("Back", 12) + sets("Legs", 6),
        )
        assertThat(r.muscleSets).containsExactly("Back", 12, "Chest", 12, "Legs", 6)
        // Legs is 20% — fine. Shoulders/Arms/Core have zero sets.
        assertThat(r.neglectedMuscles).containsExactly("Shoulders", "Arms", "Core")
        assertThat(describePatterns(r)).contains("Neglected")
    }

    @Test fun neglect_is_not_called_on_too_few_sets() {
        val r = analyzePatterns(listOf(session(at(Calendar.MARCH, 2))), now, muscleSets = sets("Chest", 10))
        assertThat(r.neglectedMuscles).isEmpty()
    }

    @Test fun muscle_balance_only_looks_at_the_last_four_weeks() {
        val r = analyzePatterns(
            listOf(session(at(Calendar.MARCH, 2))), now,
            muscleSets = List(30) { MuscleSet(at(Calendar.JANUARY, 20), "Legs") }, // 7 weeks ago
        )
        assertThat(r.muscleSets).isEmpty()
    }

    // --- prompt text ---

    @Test fun description_states_the_facts_the_model_should_not_recompute() {
        val r = analyzePatterns(
            listOf(session(at(Calendar.MARCH, 2)), session(at(Calendar.MARCH, 4)), session(at(Calendar.FEBRUARY, 23))),
            now,
        )
        val text = describePatterns(r)
        assertThat(text).contains("3 sessions")
        assertThat(text).contains("weekly goal is 5")
        assertThat(text).contains("Sessions per week, oldest to newest: 0, 0, 0, 0, 0, 0, 1, 2")
        assertThat(text).contains("6 completed weeks with no training")
        assertThat(text).contains("Last trained 4 days ago")
    }
}
