package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecoveryTest {
    private val h = 3_600_000L
    private val now = 100 * DAY_MS
    private val muscleOf: (String) -> String? = { when (it) { "bench" -> "Chest"; "row" -> "Back"; "squat" -> "Legs"; else -> null } }

    @Test fun readiness_grows_with_time_and_never_trained_is_ready() {
        val sessions = mapOf("s1" to now - 12 * h, "s2" to now - 5 * DAY_MS)
        val sets = listOf("s1" to "bench", "s1" to "bench", "s2" to "row")
        val r = recoveryByMuscle(sessions, sets, muscleOf, now).associateBy { it.muscle }
        assertThat(r.getValue("Chest").readiness).isWithin(0.01).of(12.0 / 60.0 * (1 - 0.2 * 2 / 25.0))
        assertThat(r.getValue("Back").readiness).isWithin(0.01).of(1.0)   // 5 days ago, 1 set this week
        assertThat(r.getValue("Legs").readiness).isEqualTo(1.0)
        assertThat(r.getValue("Legs").lastTrainedMs).isNull()
        assertThat(r.getValue("Chest").setsLast7d).isEqualTo(2)
        assertThat(r.getValue("Back").daysSince(now)).isEqualTo(5)
    }

    @Test fun heavy_week_lowers_readiness() {
        val sessions = mapOf("s1" to now - 10 * DAY_MS, "s2" to now - 3 * DAY_MS)
        val light = recoveryByMuscle(sessions, listOf("s2" to "bench"), muscleOf, now).first { it.muscle == "Chest" }
        val heavy = recoveryByMuscle(sessions, List(30) { "s2" to "bench" }, muscleOf, now).first { it.muscle == "Chest" }
        assertThat(heavy.readiness).isLessThan(light.readiness)
        assertThat(heavy.readiness).isWithin(0.001).of(0.8)
    }

    @Test fun suggests_the_routine_whose_muscles_are_freshest() {
        val sessions = mapOf("s1" to now - 6 * h)
        val rec = recoveryByMuscle(sessions, listOf("s1" to "bench", "s1" to "bench"), muscleOf, now)
        val s = suggestRoutine(listOf("push" to listOf("bench"), "pull" to listOf("row"), "legs" to listOf("squat")), rec, muscleOf, now)!!
        assertThat(s.routineId).isEqualTo("pull")            // first of the two fully-ready routines
        assertThat(s.reason).isEqualTo("Back hasn't been trained yet")
    }

    @Test fun reason_names_days_since() {
        val sessions = mapOf("s1" to now - 3 * DAY_MS - h)
        val rec = recoveryByMuscle(sessions, listOf("s1" to "row"), muscleOf, now)
        val s = suggestRoutine(listOf("pull" to listOf("row")), rec, muscleOf, now)!!
        assertThat(s.reason).isEqualTo("Back was last trained 3 days ago")
        assertThat(suggestRoutine(emptyList(), rec, muscleOf, now)).isNull()
        assertThat(suggestRoutine(listOf("empty" to emptyList()), rec, muscleOf, now)).isNull()
    }
}
