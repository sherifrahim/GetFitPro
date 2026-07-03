package com.getfit.domain

import kotlin.math.roundToInt

/**
 * Weight is stored canonically in **kg** everywhere (logs, bests, targets, defaults).
 * The UI converts to the user's display unit only at render/input time, so toggling kg<->lb
 * shows the correct value instead of relabelling the same number.
 */
object Units {
    private const val LB_PER_KG = 2.2046226218

    fun toDisplay(kg: Double, units: String): Double =
        if (units == "lb") kg * LB_PER_KG else kg

    fun fromDisplay(value: Double, units: String): Double =
        if (units == "lb") value / LB_PER_KG else value

    /** Stepper increment in the display unit (2.5 kg / 5 lb). */
    fun step(units: String): Double = if (units == "lb") 5.0 else 2.5

    fun roundDisplay(v: Double): Double = (v * 100).roundToInt() / 100.0

    /** Convert a stored-kg weight to a formatted string in the display unit. */
    fun fmtDisplay(kg: Double, units: String): String = fmtW(toDisplay(kg, units))

    /** Convert a stored-kg volume to the display unit, rounded to a whole number. */
    fun volDisplay(kgVolume: Int, units: String): Int = toDisplay(kgVolume.toDouble(), units).roundToInt()
}
