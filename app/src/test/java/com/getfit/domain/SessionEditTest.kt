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
