package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LoadToolsTest {

    @Test fun plates_greedy_largest_first_per_side() {
        val load = platesFor(100.0, Plates.BAR_KG, Plates.KG)!!
        assertThat(load.perSide).containsExactly(25.0, 15.0).inOrder()
        assertThat(load.exact).isTrue()
    }

    @Test fun plates_use_small_plates_and_report_remainder() {
        assertThat(platesFor(62.5, 20.0, Plates.KG)!!.perSide).containsExactly(20.0, 1.25).inOrder()
        val odd = platesFor(61.0, 20.0, Plates.KG)!!          // 20.5 per side: 20 + 0.5 unreachable
        assertThat(odd.perSide).containsExactly(20.0)
        assertThat(odd.remainder).isEqualTo(1.0)
        assertThat(odd.exact).isFalse()
    }

    @Test fun plates_empty_bar_and_below_bar() {
        assertThat(platesFor(20.0, 20.0, Plates.KG)!!.perSide).isEmpty()
        assertThat(platesFor(15.0, 20.0, Plates.KG)).isNull()
    }

    @Test fun plates_in_pounds() {
        val load = platesFor(225.0, Plates.BAR_LB, Plates.LB)!!
        assertThat(load.perSide).containsExactly(45.0, 45.0).inOrder()
        assertThat(load.exact).isTrue()
    }

    @Test fun warmup_ramp_rounds_down_to_increment() {
        val ramp = warmupRamp(100.0, 20.0, 2.5)
        assertThat(ramp.map { it.weight }).containsExactly(50.0, 70.0, 90.0).inOrder()
        assertThat(ramp.map { it.reps }).containsExactly(8, 5, 2).inOrder()
        val ramp2 = warmupRamp(87.5, 20.0, 2.5)                // 43.75 -> 42.5, 61.25 -> 60, 78.75 -> 77.5
        assertThat(ramp2.map { it.weight }).containsExactly(42.5, 60.0, 77.5).inOrder()
    }

    @Test fun warmup_ramp_shrinks_for_light_weights_and_vanishes_at_the_bar() {
        // 40 kg: 50% = 20 (bar) -> dropped as no-information; 70% = 27.5, 90% = 35
        assertThat(warmupRamp(40.0, 20.0, 2.5).map { it.weight }).containsExactly(27.5, 35.0).inOrder()
        assertThat(warmupRamp(20.0, 20.0, 2.5)).isEmpty()
        assertThat(warmupRamp(10.0, 20.0, 2.5)).isEmpty()
        // 25 kg: everything rounds to the bar (20) or above the working weight -> only the 90% (22.5) step
        assertThat(warmupRamp(25.0, 20.0, 2.5).map { it.weight }).containsExactly(22.5)
    }
}
