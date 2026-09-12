package com.getfit.data.wear

import com.getfit.domain.Phase
import com.getfit.domain.SessionItem
import com.getfit.domain.doneSet
import com.getfit.domain.pauseSession
import com.getfit.domain.startSession
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Covers [toWearSnapshot], the phone -> watch mapping. Everything downstream of it needs a paired
 * device to exercise, so this is the part of the wear feature that can be pinned on the JVM — and
 * it is worth pinning, because a dropped field here produces a watch that displays confidently
 * wrong numbers rather than an error.
 *
 * States are built with the real SessionEngine functions rather than hand-assembled, so the test
 * also catches a SessionState field being renamed out from under the mapping.
 */
class WearSnapshotTest {

    private val bench = SessionItem("bench", "Bench Press", "Chest", sets = 3, reps = "8", bw = false, suggestW = 100.0)
    private val pullup = SessionItem("pullup", "Pull Up", "Back", sets = 2, reps = "10", bw = true, suggestW = 0.0)
    private val t0 = 1_700_000_000_000L

    private fun session(now: Long = t0) =
        startSession(listOf(bench, pullup), restDefault = 60, preBest = emptyMap(), now = now)

    @Test fun no_session_is_inactive() {
        val snap = toWearSnapshot(null, "kg")
        assertThat(snap.active).isFalse()
    }

    @Test fun active_session_maps_the_current_exercise() {
        val snap = toWearSnapshot(session(), "kg")
        assertThat(snap.active).isTrue()
        assertThat(snap.exerciseName).isEqualTo("Bench Press")
        assertThat(snap.setNum).isEqualTo(1)
        assertThat(snap.totalSetsForExercise).isEqualTo(3)
        assertThat(snap.curWeight).isEqualTo(100.0)
        assertThat(snap.curReps).isEqualTo(8)
        assertThat(snap.bodyweight).isFalse()
        assertThat(snap.phase).isEqualTo(WearPhase.WORK)
        assertThat(snap.totalSets).isEqualTo(5) // 3 bench + 2 pull-up
    }

    @Test fun units_pass_through() {
        assertThat(toWearSnapshot(session(), "lb").units).isEqualTo("lb")
        assertThat(toWearSnapshot(session(), "kg").units).isEqualTo("kg")
    }

    /**
     * The watch runs its own 1Hz countdown off these anchors instead of the phone messaging it every
     * second (docs/wear-companion-design.md section 2). If they stop being carried, the watch's rest
     * timer silently shows the wrong time rather than failing.
     */
    @Test fun wall_clock_anchors_are_carried_to_the_watch() {
        val resting = doneSet(session(), units = "kg", now = t0 + 30_000).state
        val snap = toWearSnapshot(resting, "kg")

        assertThat(snap.phase).isEqualTo(WearPhase.REST)
        assertThat(snap.restEndAtMs).isEqualTo(t0 + 30_000 + 60_000)
        assertThat(snap.restTotal).isEqualTo(60)
        assertThat(snap.workStartedAtMs).isEqualTo(t0)
        assertThat(snap.restEndAtMs).isGreaterThan(0L)
    }

    @Test fun paused_state_is_carried() {
        val paused = pauseSession(session(), now = t0 + 5_000)
        assertThat(toWearSnapshot(paused, "kg").paused).isTrue()
        assertThat(toWearSnapshot(session(), "kg").paused).isFalse()
    }

    @Test fun completed_sets_advance() {
        val after = doneSet(session(), units = "kg", now = t0 + 30_000).state
        val snap = toWearSnapshot(after, "kg")
        assertThat(snap.completedSets).isEqualTo(1)
        assertThat(snap.totalSets).isEqualTo(5)
    }

    /** All three phases must map; DONE in particular is what drives the watch's summary screen. */
    @Test fun every_phase_maps() {
        val work = session()
        assertThat(toWearSnapshot(work, "kg").phase).isEqualTo(WearPhase.WORK)

        val rest = doneSet(work, units = "kg", now = t0 + 30_000).state
        assertThat(rest.phase).isEqualTo(Phase.REST)
        assertThat(toWearSnapshot(rest, "kg").phase).isEqualTo(WearPhase.REST)

        // Log every set of both exercises to reach DONE.
        var s = work
        repeat(s.totalSets) { s = doneSet(s, units = "kg", now = t0).state.let { n ->
            if (n.phase == Phase.REST) com.getfit.domain.advanceFromRest(n, fromTick = true, now = t0) else n
        } }
        assertThat(s.phase).isEqualTo(Phase.DONE)
        assertThat(toWearSnapshot(s, "kg").phase).isEqualTo(WearPhase.DONE)
    }

    /**
     * DONE is still `active`. The watch distinguishes "session finished, show the summary" from
     * "no session, show Idle" — and Idle is only the null case, because the phone keeps the state
     * around until the user dismisses the summary.
     */
    @Test fun finished_session_is_still_active() {
        var s = session()
        repeat(s.totalSets) { s = doneSet(s, units = "kg", now = t0).state.let { n ->
            if (n.phase == Phase.REST) com.getfit.domain.advanceFromRest(n, fromTick = true, now = t0) else n
        } }
        val snap = toWearSnapshot(s, "kg")
        assertThat(snap.phase).isEqualTo(WearPhase.DONE)
        assertThat(snap.active).isTrue()
    }

    @Test fun bodyweight_exercise_is_flagged() {
        var s = session()
        // Finish all 3 bench sets so the current exercise becomes the bodyweight pull-up.
        repeat(3) { s = doneSet(s, units = "kg", now = t0).state.let { n ->
            if (n.phase == Phase.REST) com.getfit.domain.advanceFromRest(n, fromTick = true, now = t0) else n
        } }
        val snap = toWearSnapshot(s, "kg")
        assertThat(snap.exerciseName).isEqualTo("Pull Up")
        assertThat(snap.bodyweight).isTrue()
    }

    /**
     * The snapshot crosses the wire as JSON, and PhoneWearSync's de-dup compares encoded strings,
     * so it has to serialize and come back identical.
     */
    @Test fun snapshot_survives_a_json_round_trip() {
        val json = Json { ignoreUnknownKeys = true }
        val snap = toWearSnapshot(doneSet(session(), "kg", t0 + 30_000).state, "kg")

        val decoded = json.decodeFromString<SessionSnapshot>(json.encodeToString(snap))

        assertThat(decoded).isEqualTo(snap)
    }

    /** An older watch build missing a newer field must not fail the whole decode. */
    @Test fun unknown_fields_are_ignored_on_decode() {
        val json = Json { ignoreUnknownKeys = true }
        val withExtra = """{"active":true,"exerciseName":"Bench Press","reps":5,"futureField":42}"""

        val decoded = json.decodeFromString<SessionSnapshot>(withExtra)

        assertThat(decoded.active).isTrue()
        assertThat(decoded.exerciseName).isEqualTo("Bench Press")
    }

    @Test fun watch_action_round_trips() {
        val json = Json { ignoreUnknownKeys = true }
        val action = WatchAction(ActionKind.ADJUST_REST, restDeltaSec = 15)

        assertThat(json.decodeFromString<WatchAction>(json.encodeToString(action))).isEqualTo(action)
    }

    @Test fun heart_rate_batch_round_trips() {
        val json = Json { ignoreUnknownKeys = true }
        val batch = HeartRateBatch(listOf(HeartRateSample(142.0, t0), HeartRateSample(147.5, t0 + 8_000)))

        assertThat(json.decodeFromString<HeartRateBatch>(json.encodeToString(batch))).isEqualTo(batch)
    }
}

class WearRoutinesTest {
    private val bench = com.getfit.data.db.ExerciseEntity("bench", "Bench", "Chest", "Barbell", "Intermediate", "Chest", "", "8", null, "")
    private val dip = com.getfit.data.db.ExerciseEntity("dip", "Dip", "Arms", "Bodyweight", "Intermediate", "Triceps", "", "12", null, "")

    @Test fun items_carry_display_prefill_bests_and_last_sets() {
        val routines = listOf(
            com.getfit.data.prefs.Routine("r1", "Push", listOf(
                com.getfit.data.prefs.PlanItemData("bench", 3, "8", superset = true),
                com.getfit.data.prefs.PlanItemData("dip", 2, "12"),
                com.getfit.data.prefs.PlanItemData("ghost", 1, "5"),      // not in the library: dropped
            )),
        )
        val best = mapOf("bench" to com.getfit.domain.Best(false, 100.0, 5, 117, 0L))
        val out = toWearRoutines(routines, mapOf("bench" to bench, "dip" to dip), mapOf("bench" to 80.0), best, "lb", mapOf("bench" to "176.4×8"))
        val r = out.single()
        assertThat(r.exercises).isEqualTo(3); assertThat(r.sets).isEqualTo(6)   // counts follow the routine as written
        assertThat(r.items.map { it.id }).containsExactly("bench", "dip").inOrder()
        val b = r.items[0]
        assertThat(b.suggestW).isWithin(0.01).of(176.37)                      // 80 kg in lb
        assertThat(b.bestW).isWithin(0.01).of(220.46)
        assertThat(b.bestReps).isEqualTo(5)
        assertThat(b.superset).isTrue()
        assertThat(b.equipment).isEqualTo("Barbell")
        assertThat(b.last).isEqualTo("176.4×8")
        val d = r.items[1]
        assertThat(d.bw).isTrue()
        assertThat(d.suggestW).isEqualTo(0.0)                                   // DEFAULT_WEIGHT["dip"] = 0
        assertThat(d.bestW).isEqualTo(0.0)
    }

    /** Snapshot round-trips the routine list, so a watch with an older protocol still decodes it. */
    @Test fun snapshot_carries_routines_and_rest_default() {
        val wr = listOf(WearRoutine("r1", "Push", 1, 3, listOf(WearItem("bench", "Bench", "Chest", 3, "8", false, 60.0))))
        val snap = toWearSnapshot(null, "kg", wr, "r1", restDefault = 90)
        val json = Json { ignoreUnknownKeys = true }
        val back = json.decodeFromString<SessionSnapshot>(json.encodeToString(snap))
        assertThat(back.active).isFalse()
        assertThat(back.currentRoutineId).isEqualTo("r1")
        assertThat(back.restDefault).isEqualTo(90)
        assertThat(back.routines.single().items.single().suggestW).isEqualTo(60.0)
    }
}
