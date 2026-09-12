package com.getfit.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionEditTest {
    private fun item(id: String, sets: Int = 2, superset: Boolean = false) =
        SessionItem(id, id.uppercase(), "Chest", sets = sets, reps = "8", bw = false, suggestW = 50.0, superset = superset)
    private fun start(vararg items: SessionItem) = startSession(items.toList(), 60, emptyMap(), now = 0L)

    @Test fun add_appends_and_grows_totals_without_moving() {
        val s0 = start(item("a"), item("b"))
        val s1 = addItem(s0, item("c", sets = 3))
        assertThat(s1.items.map { it.id }).containsExactly("a", "b", "c").inOrder()
        assertThat(s1.totalSets).isEqualTo(7)
        assertThat(s1.idx).isEqualTo(0); assertThat(s1.setNum).isEqualTo(1)
        assertThat(addItem(s1, item("a"))).isEqualTo(s1)          // duplicates ignored
    }

    @Test fun replace_current_keeps_logged_sets_and_restarts_at_set_one() {
        var s = start(item("a", sets = 3), item("b"))
        s = doneSet(s, "kg", now = 1).state                         // a set 1 logged -> rest
        s = replaceItem(s, 0, item("x", sets = 2), now = 2)
        assertThat(s.current.id).isEqualTo("x"); assertThat(s.setNum).isEqualTo(1)
        assertThat(s.phase).isEqualTo(Phase.WORK)
        assertThat(s.log.map { it.id }).containsExactly("a")
        assertThat(s.totalSets).isEqualTo(2 + 2 + 1)                // x + b + the orphaned a set
        assertThat(s.completedSets).isEqualTo(1)
    }

    @Test fun replace_keeps_the_superset_link_of_the_slot() {
        val s = start(item("a", superset = true), item("b"))
        val r = replaceItem(s, 0, item("x"))
        assertThat(r.items[0].superset).isTrue()
        assertThat(replaceItem(s, 5, item("y"))).isEqualTo(s)
    }

    @Test fun remove_unstarted_item_and_repair_links() {
        val s = start(item("a", superset = true), item("b", superset = true), item("c"))
        val r = removeItem(s, 1)
        assertThat(r.items.map { it.id }).containsExactly("a", "c").inOrder()
        assertThat(r.items[0].superset).isTrue()                     // b linked onward, so a->c stays linked
        assertThat(r.items[1].superset).isFalse()
        val r2 = removeItem(start(item("a", superset = true), item("b")), 1)
        assertThat(r2.items.single().superset).isFalse()
    }

    @Test fun remove_refuses_started_items_and_the_last_item() {
        var s = start(item("a"), item("b"))
        s = doneSet(s, "kg", now = 1).state
        assertThat(canRemoveItem(s, 0)).isFalse()
        assertThat(removeItem(s, 0)).isEqualTo(s)
        assertThat(canRemoveItem(s, 1)).isTrue()
        val only = start(item("a"))
        assertThat(canRemoveItem(only, 0)).isFalse()
    }

    @Test fun remove_current_jumps_to_what_followed() {
        val s = start(item("a"), item("b", sets = 3))
        val r = removeItem(s, 0)
        assertThat(r.current.id).isEqualTo("b"); assertThat(r.setNum).isEqualTo(1)
        assertThat(r.totalSets).isEqualTo(3)
    }

    @Test fun skip_exercise_trims_sets_and_advances() {
        var s = start(item("a", sets = 4), item("b"))
        s = doneSet(s, "kg", now = 1).state                          // 1 of 4 logged, resting
        s = skipExercise(s, now = 2)
        assertThat(s.current.id).isEqualTo("b"); assertThat(s.phase).isEqualTo(Phase.WORK)
        assertThat(s.items[0].sets).isEqualTo(1)
        assertThat(s.totalSets).isEqualTo(1 + 2)
        assertThat(s.completedSets).isEqualTo(1)
    }

    @Test fun skip_last_exercise_ends_the_session() {
        val s = start(item("a"))
        val r = skipExercise(s, now = 5)
        assertThat(r.phase).isEqualTo(Phase.DONE)
        assertThat(r.items[0].sets).isEqualTo(1)
    }
}

class SessionUnitsTest {
    @Test fun kg_to_lb_converts_every_weight_and_back_exactly() {
        var s = startSession(
            listOf(SessionItem("bench", "Bench", "Chest", sets = 2, reps = "8", bw = false, suggestW = 60.0)),
            60, mapOf("bench" to (80.0 to 5)), now = 0L,
        )
        s = doneSet(s.copy(curW = 100.0), "kg", now = 1).state          // PR at 100 kg
        val lb = convertSessionUnits(s, "kg", "lb")
        assertThat(lb.curW).isWithin(0.01).of(220.46)
        assertThat(lb.log.single().weight).isWithin(0.01).of(220.46)
        assertThat(lb.preBest.getValue("bench").first).isWithin(0.01).of(220.46)
        assertThat(lb.items.single().suggestW).isWithin(0.01).of(132.28)
        assertThat(lb.volume).isWithin(0.1).of(100.0 * 8 * 2.2046226218)
        assertThat(lb.newPRs.single().value).isEqualTo("220.5 lb × 8")
        val back = convertSessionUnits(lb, "lb", "kg")
        assertThat(back.curW).isWithin(0.01).of(100.0)
        assertThat(back.log.single().weight).isWithin(0.01).of(100.0)
        assertThat(convertSessionUnits(s, "kg", "kg")).isEqualTo(s)
    }
}

class PerExerciseRestTest {
    @Test fun exercise_override_wins_then_default_returns() {
        val items = listOf(
            SessionItem("a", "A", "Chest", sets = 1, reps = "8", bw = false, suggestW = 50.0, restSec = 120),
            SessionItem("b", "B", "Back", sets = 2, reps = "8", bw = false, suggestW = 50.0),
        )
        var s = startSession(items, restDefault = 60, preBest = emptyMap(), now = 0L)
        s = doneSet(s, "kg", now = 1000).state                  // A: 120 s override
        assertThat(s.restTotal).isEqualTo(120); assertThat(s.restLeft).isEqualTo(120)
        assertThat(s.restEndAtMs).isEqualTo(1000 + 120_000)
        s = advanceFromRest(s, false, now = 2000)
        s = doneSet(s, "kg", now = 3000).state                  // B: session default
        assertThat(s.restTotal).isEqualTo(60); assertThat(s.restEndAtMs).isEqualTo(3000 + 60_000)
    }

    /** A state persisted before restDefault existed (0) keeps using restTotal as the default. */
    @Test fun legacy_state_without_default_still_rests() {
        val items = listOf(SessionItem("a", "A", "Chest", sets = 2, reps = "8", bw = false, suggestW = 50.0))
        val legacy = startSession(items, 90, emptyMap(), now = 0L).copy(restDefault = 0)
        assertThat(doneSet(legacy, "kg", now = 1).state.restTotal).isEqualTo(90)
    }
}
