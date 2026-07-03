package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormattersTest {
    @Test fun vol() {
        assertThat(fmtVol(0)).isEqualTo("0")
        assertThat(fmtVol(9999)).isEqualTo("9,999")
        assertThat(fmtVol(12000)).isEqualTo("12k")
        assertThat(fmtVol(1_500_000)).isEqualTo("1.5M")
        assertThat(fmtVol(2_000_000)).isEqualTo("2M")
    }

    @Test fun dur() {
        assertThat(fmtDur(0)).isEqualTo("0m")
        assertThat(fmtDur(1800)).isEqualTo("30m")
        assertThat(fmtDur(7200)).isEqualTo("2h")
        assertThat(fmtDur(5400)).isEqualTo("1.5h")
    }

    @Test fun clock() {
        assertThat(fmtClock(65)).isEqualTo("1:05")
        assertThat(fmtClock(600)).isEqualTo("10:00")
    }

    @Test fun weight() {
        assertThat(fmtW(60.0)).isEqualTo("60")
        assertThat(fmtW(92.5)).isEqualTo("92.5")
    }
}
