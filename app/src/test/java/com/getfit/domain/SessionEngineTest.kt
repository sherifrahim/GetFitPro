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
        val resting = start().copy(phase = Phase.REST, restLeft = 10)
        assertThat(addRest(resting, -15).restLeft).isEqualTo(5)
    }

    @Test fun tick_counts_down_rest() {
        val resting = start().copy(phase = Phase.REST, restLeft = 5)
        assertThat(tick(resting).restLeft).isEqualTo(4)
    }
}
