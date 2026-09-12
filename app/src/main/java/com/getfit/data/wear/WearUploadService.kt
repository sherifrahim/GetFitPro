package com.getfit.data.wear

import com.getfit.GetFitApp
import com.getfit.data.repo.SessionSaveSet
import com.getfit.domain.HrPoint
import com.getfit.domain.Units
import com.getfit.domain.addHeartRate
import com.getfit.domain.SessionState
import com.getfit.domain.avgBpm
import com.getfit.domain.estimateCalories
import com.getfit.domain.maxBpm
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Receives workouts the watch ran on its own (see the watch's LocalSession) — whether or not the
 * phone app is open. This is a manifest-registered service, not the MessageClient listener in
 * AppViewModel, precisely so a workout done on a run without the phone lands in history the moment
 * the two devices reconnect, not the next time the user happens to open Forge.
 *
 * Saving is idempotent on the upload id (the watch retries until it gets the ack), and the ack is
 * sent whether the record was new or already there. Weights arrive in the watch's display units.
 */
class WearUploadService : WearableListenerService() {
    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WearPaths.SESSION_UPLOAD) return
        val upload = runCatching { json.decodeFromString<WearSessionUpload>(String(event.data)) }.getOrNull() ?: return
        val container = (application as? GetFitApp)?.container ?: return
        // WearableListenerService callbacks run on a background thread; the DAOs are suspend
        // functions, so block this thread — it is ours, and the work is one transaction.
        runBlocking { save(container, upload) }
        container.phoneWearSync.sendUploadAck(upload.id)
    }

    private suspend fun save(container: com.getfit.di.AppContainer, u: WearSessionUpload) {
        if (u.sets.isEmpty()) return
        if (container.db.sessionDao().byId(u.id) != null) return          // retry of something we have
        val setsKg = u.sets.map { SessionSaveSet(it.exerciseId, it.name, Units.fromDisplay(it.weight, u.units), it.reps) }
        val volumeKg = u.sets.filterNot { it.bw }.sumOf { Units.fromDisplay(it.weight, u.units) * it.reps }
        val hr = addHeartRate(SessionState(items = emptyList()), u.hr.map { HrPoint(it.atMs, Math.round(it.bpm).toInt()) }).hr
        val avg = avgBpm(hr)
        val profile = container.settingsStore.flow.first()
        container.workoutRepo.saveSession(
            now = u.startedAtMs,
            name = u.name,
            durationSec = u.durationSec,
            sets = setsKg,
            volume = Math.round(volumeKg).toInt(),
            prs = u.prs,
            routineId = u.routineId,
            avgBpm = avg,
            maxBpm = maxBpm(hr),
            calories = estimateCalories(u.durationSec, avg, profile.bodyWeightKg, profile.ageYears(), profile.isMale()),
            heartRate = hr,
            id = u.id,
        )
        container.workoutRepo.advanceAfter(u.routineId)
    }
}
