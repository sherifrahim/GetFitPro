package com.getfit.wear.data

import com.getfit.data.wear.WearPhase
import com.getfit.data.wear.SessionSnapshot
import com.getfit.domain.Phase
import com.getfit.domain.SessionState

/**
 * A standalone session rendered through the same screens as a phone-mirrored one: map the engine's
 * SessionState to the wire snapshot shape the Active/Rest/Done screens already consume. Mirrors the
 * phone's toWearSnapshot on purpose, so the two modes look identical on the wrist.
 */
fun localSnapshot(state: SessionState, units: String): SessionSnapshot {
    val item = state.current
    return SessionSnapshot(
        active = true,
        exerciseName = item.name,
        setNum = state.setNum,
        totalSetsForExercise = item.sets,
        curWeight = state.curW,
        curReps = state.curR,
        units = units,
        bodyweight = item.bw,
        phase = when (state.phase) {
            Phase.WORK -> WearPhase.WORK
            Phase.REST -> WearPhase.REST
            Phase.DONE -> WearPhase.DONE
        },
        restEndAtMs = state.restEndAtMs,
        restTotal = state.restTotal,
        paused = state.paused,
        workStartedAtMs = state.workStartedAtMs,
        pausedAccumMs = state.pausedAccumMs,
        completedSets = state.completedSets,
        totalSets = state.totalSets,
        sessionName = state.name,
    )
}
