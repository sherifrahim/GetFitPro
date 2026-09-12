package com.getfit.domain

import java.util.Locale
import kotlin.math.roundToInt

/** Volume: "0", "12k" (>=10k), "1.5M" (>=1M), else grouped (proto fmtVol, L782). */
fun fmtVol(v: Int): String = when {
    v <= 0 -> "0"
    v >= 1_000_000 -> String.format(Locale.US, "%.1f", v / 1_000_000.0).removeSuffix(".0") + "M"
    v >= 10_000 -> (v / 1000.0).roundToInt().toString() + "k"
    else -> String.format(Locale.US, "%,d", v)
}

/** Duration: "0m", "30m", "2h" / "1.5h" (proto fmtDur, L781). */
fun fmtDur(sec: Int): String {
    val m = (sec / 60.0).roundToInt()
    return when {
        m <= 0 -> "0m"
        m < 60 -> "${m}m"
        else -> {
            val h = m / 60.0
            (if (h % 1.0 == 0.0) h.toInt().toString() else String.format(Locale.US, "%.1f", h)) + "h"
        }
    }
}

/** m:ss clock (proto fmt, L766). */
fun fmtClock(sec: Int): String = "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"

/** Weight: whole numbers drop the decimal, else one decimal (proto fmtW, L767). */
fun fmtW(w: Double): String =
    if (w % 1.0 == 0.0) w.toInt().toString() else String.format(Locale.US, "%.1f", w)
