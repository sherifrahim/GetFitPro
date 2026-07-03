package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TargetMathTest {
    @Test fun progress_clamps_0_1() {
        assertThat(targetPct(cur = 105.0, start = 100.0, target = 110.0)).isWithin(0.001).of(0.5)
        assertThat(targetPct(cur = 130.0, start = 100.0, target = 110.0)).isEqualTo(1.0)
        assertThat(targetPct(cur = 90.0, start = 100.0, target = 110.0)).isEqualTo(0.0)
    }

    @Test fun days_left_counts_forward() {
        val now = 10 * DAY_MS
        // started 3 days ago, 1-week target -> 7 - 3 = 4 days left
        assertThat(targetDaysLeft(startDMs = now - 3 * DAY_MS, weeks = 1, now = now)).isEqualTo(4)
    }
}
