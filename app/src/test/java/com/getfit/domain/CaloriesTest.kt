package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CaloriesTest {

    /** Keytel, male, 75 kg, 30 y, 120 bpm: (-55.0969 + 75.708 + 14.91 + 6.051) / 4.184 ≈ 9.93 kcal/min. */
    @Test fun heart_rate_path_matches_keytel_male() {
        assertThat(estimateCalories(durationSec = 3600, avgBpm = 120, weightKg = 75.0, ageYears = 30, male = true)).isEqualTo(596)
    }

    @Test fun heart_rate_path_matches_keytel_female() {
        // (-20.4022 + 53.664 - 9.4725 + 2.22) / 4.184 ≈ 6.22 kcal/min
        assertThat(estimateCalories(3600, 120, 75.0, 30, male = false)).isEqualTo(373)
    }

    @Test fun unknown_sex_averages_the_two_equations() {
        val m = estimateCalories(3600, 120, 75.0, 30, true)
        val f = estimateCalories(3600, 120, 75.0, 30, false)
        assertThat(Math.abs(estimateCalories(3600, 120, 75.0, 30, null) - (m + f) / 2)).isAtMost(1)
    }

    /** No watch → 6 MET: 6 × 3.5 × 75 / 200 = 7.875 kcal/min. */
    @Test fun no_heart_rate_falls_back_to_met() {
        assertThat(estimateCalories(durationSec = 3600, avgBpm = 0)).isEqualTo(473)
        assertThat(estimateCalories(durationSec = 1800, avgBpm = 0, weightKg = 100.0)).isEqualTo(315)
    }

    @Test fun zero_duration_is_zero_and_bad_inputs_use_defaults() {
        assertThat(estimateCalories(0, 150)).isEqualTo(0)
        assertThat(estimateCalories(3600, 0, weightKg = -5.0)).isEqualTo(estimateCalories(3600, 0))
    }

    @Test fun trace_summaries() {
        val hr = listOf(HrPoint(0, 100), HrPoint(5000, 120), HrPoint(10000, 131))
        assertThat(avgBpm(hr)).isEqualTo(117)
        assertThat(maxBpm(hr)).isEqualTo(131)
        assertThat(avgBpm(emptyList())).isEqualTo(0)
        assertThat(maxBpm(emptyList())).isEqualTo(0)
    }

    @Test fun add_heart_rate_keeps_last_per_bucket_and_tolerates_overlap() {
        val s0 = SessionState(items = emptyList())
        val s1 = addHeartRate(s0, listOf(HrPoint(1000, 90), HrPoint(3000, 95), HrPoint(6000, 100)))
        assertThat(s1.hr).containsExactly(HrPoint(3000, 95), HrPoint(6000, 100)).inOrder()
        // Re-delivering the same batch plus one new sample changes nothing but the new point.
        val s2 = addHeartRate(s1, listOf(HrPoint(3000, 95), HrPoint(6000, 100), HrPoint(12000, 0), HrPoint(13000, 110)))
        assertThat(s2.hr).containsExactly(HrPoint(3000, 95), HrPoint(6000, 100), HrPoint(13000, 110)).inOrder()
    }
}
