package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionEngineTest {

    private fun items() = listOf(
        SessionItem("bench", "Bench", "Chest", sets = 2, reps = "8", bw = false, suggestW = 60.0),
        SessionItem("dip", "Dip", "Arms", sets = 1, reps = "12", bw = true, suggestW = 0.0),
    )

    private fun start() = startSession(items(), restDefault = 60, preBest = mapOf("bench" to (80.0 to 5), "dip" to (0.0 to 10)))

    @Test fun start_sets_first_item_weight_and_reps() {
        val s = start()
        assertThat(s.curW).isEqualTo(60.0)
        assertThat(s.curR).isEqualTo(8)
        assertThat(s.totalSets).isEqualTo(3)
        assertThat(s.phase).isEqualTo(Phase.WORK)
    }

    @Test fun done_nonlast_goes_to_rest_and_logs() {
        val r = doneSet(start(), "kg")
        assertThat(r.state.phase).isEqualTo(Phase.REST)
        assertThat(r.state.restLeft).isEqualTo(60)
        assertThat(r.state.log).hasSize(1)
        assertThat(r.state.completedSets).isEqualTo(1)
        assertThat(r.state.volume).isEqualTo(60.0 * 8)
    }

    @Test fun weighted_pr_when_above_prebest() {
        val s = start().copy(curW = 90.0) // > preBest 80
        val r = doneSet(s, "kg")
        assertThat(r.pr).isTrue()
        assertThat(r.state.newPRs.map { it.id }).contains("bench")
    }

    @Test fun no_pr_when_below_prebest() {
        val r = doneSet(start().copy(curW = 60.0), "kg") // < 80
        assertThat(r.pr).isFalse()
    }

    @Test fun bodyweight_pr_on_reps() {
        // advance to the dip (bodyweight) item: finish both bench sets then rest-advance
        var s = doneSet(start(), "kg").state          // bench set1 -> rest
        s = advanceFromRest(s, false)                 // bench set2 (work)
        s = doneSet(s, "kg").state                    // bench set2 -> rest
        s = advanceFromRest(s, false)                 // dip set1 (work)
        assertThat(s.current.id).isEqualTo("dip")
        val r = doneSet(s.copy(curR = 15), "kg")      // 15 > preBest 10
        assertThat(r.pr).isTrue()
        assertThat(r.state.phase).isEqualTo(Phase.DONE) // last set of last exercise
        assertThat(r.state.volume).isEqualTo(60.0 * 8 * 2) // bodyweight adds no volume
    }

    @Test fun advance_moves_set_then_exercise() {
        val s0 = start()
        val afterSet1 = advanceFromRest(s0.copy(phase = Phase.REST), false)
        assertThat(afterSet1.setNum).isEqualTo(2)
        val afterSet2 = advanceFromRest(afterSet1.copy(phase = Phase.REST), false)
        assertThat(afterSet2.idx).isEqualTo(1)
        assertThat(afterSet2.setNum).isEqualTo(1)
        assertThat(afterSet2.curR).isEqualTo(12) // dip initReps
    }

    @Test fun weight_and_rep_steps() {
        assertThat(adjustW(start(), 1, "kg").curW).isEqualTo(62.5)
        assertThat(adjustW(start(), 1, "lb").curW).isEqualTo(65.0)
        assertThat(adjustR(start(), -1).curR).isEqualTo(7)
    }

    @Test fun add_rest_clamps_to_five() {
        val now = 1_000_000L
        val resting = start().copy(phase = Phase.REST, restEndAtMs = now + 10_000, restLeft = 10)
        assertThat(addRest(resting, -15, now).restLeft).isEqualTo(5)
    }

    @Test fun tick_counts_down_rest() {
        val now = 1_000_000L
        val resting = start().copy(phase = Phase.REST, restEndAtMs = now + 5_000, restLeft = 5)
        assertThat(tick(resting, now + 1_000).restLeft).isEqualTo(4)
    }

    // --- wall-clock anchoring: the point of this file's timing rework ---

    @Test fun tick_self_corrects_after_a_missed_interval() {
        // Simulates the 1s ticker having been throttled (Doze, backgrounded coroutine, a
        // process restart) for 12 real seconds instead of firing every second: restLeft should
        // drop by the true elapsed time, not by 1.
        val now = 1_000_000L
        val resting = start().copy(phase = Phase.REST, restEndAtMs = now + 30_000, restLeft = 30)
        assertThat(tick(resting, now + 12_000).restLeft).isEqualTo(18)
    }

    @Test fun tick_ends_rest_when_the_anchor_has_already_elapsed() {
        // If the app was backgrounded for longer than the remaining rest, the very next tick
        // should advance straight out of REST rather than reporting a negative restLeft.
        val now = 1_000_000L
        val resting = start().copy(phase = Phase.REST, restEndAtMs = now + 10_000, restLeft = 10, setNum = 1)
        val r = tick(resting, now + 25_000)
        assertThat(r.phase).isEqualTo(Phase.WORK)
        assertThat(r.setNum).isEqualTo(2)
    }

    @Test fun elapsed_derives_from_wall_clock_and_pause_freezes_it() {
        val now = 1_000_000L
        var s = startSession(items(), restDefault = 60, preBest = emptyMap(), now = now)
        s = tick(s, now + 10_000)
        assertThat(s.elapsed).isEqualTo(10)

        s = togglePause(s, now + 10_000)
        s = tick(s, now + 40_000) // ticking while paused is a no-op
        assertThat(s.elapsed).isEqualTo(10)

        s = togglePause(s, now + 40_000) // resume 30s after pausing
        s = tick(s, now + 45_000)
        assertThat(s.elapsed).isEqualTo(15) // 10s before pause + 5s after resume; the 30s paused span is excluded
    }

    @Test fun pause_freezes_rest_countdown_across_a_background_gap() {
        val now = 1_000_000L
        var s = start().copy(phase = Phase.REST, restEndAtMs = now + 30_000, restLeft = 30)
        s = togglePause(s, now)
        s = tick(s, now + 20_000) // no-op while paused
        assertThat(s.restLeft).isEqualTo(30)

        s = togglePause(s, now + 20_000) // resume 20s later -> restEndAtMs shifts forward by the paused span
        s = tick(s, now + 20_000)
        assertThat(s.restLeft).isEqualTo(30)
    }
}
