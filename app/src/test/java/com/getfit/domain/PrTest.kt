package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrTest {
    // proto SEED.bench: [[85,8],[92.5,6],[100,5]]
    private val benchSets = listOf(
        LoggedSet("bench", 85.0, 8, 0),
        LoggedSet("bench", 92.5, 6, 0),
        LoggedSet("bench", 100.0, 5, 0),
    )

    @Test fun isBW_true_for_bodyweight_equipment() {
        assertThat(isBW(equipment = "Bodyweight", reps = "12")).isTrue()
    }

    @Test fun isBW_true_for_time_based_reps() {
        assertThat(isBW(equipment = "Barbell", reps = "45s")).isTrue()
    }

    @Test fun isBW_false_for_weighted() {
        assertThat(isBW(equipment = "Barbell", reps = "8")).isFalse()
    }

    @Test fun best_weighted_picks_max_weight_and_e1rm() {
        val b = bestFor(benchSets, bodyweight = false)!!
        assertThat(b.weight).isEqualTo(100.0)
        assertThat(b.reps).isEqualTo(5)
        assertThat(b.e1rm).isEqualTo(117) // round(100*(1+5/30)) = 117
    }

    @Test fun best_bodyweight_picks_max_reps() {
        val sets = listOf(LoggedSet("dip", 0.0, 12, 1), LoggedSet("dip", 0.0, 15, 2))
        val b = bestFor(sets, bodyweight = true)!!
        assertThat(b.reps).isEqualTo(15)
        assertThat(b.weight).isEqualTo(0.0)
        assertThat(b.e1rm).isEqualTo(0)
    }

    @Test fun best_empty_is_null() {
        assertThat(bestFor(emptyList(), false)).isNull()
    }
}
