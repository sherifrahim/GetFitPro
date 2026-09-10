package com.getfit.wear.data

import android.content.Context
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate

/**
 * Live heart rate during an active session, via Health Services' ExerciseClient
 * (ExerciseType.STRENGTH_TRAINING — no GPS needed, unlike a run/ride exercise type). Requires the
 * BODY_SENSORS runtime permission (declared in the manifest; requesting it at runtime is still
 * needed from the UI layer, not yet wired here).
 */
class HeartRateMonitor(context: Context) {
    private val exerciseClient = HealthServices.getClient(context).exerciseClient

    private var onBpm: ((Double) -> Unit)? = null

    private val callback = object : ExerciseUpdateCallback {
        override fun onRegistered() {}
        override fun onRegistrationFailed(throwable: Throwable) {}

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            update.latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.let { sample ->
                onBpm?.invoke(sample.value)
            }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}
        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {}
    }

    fun start(onBpmUpdate: (Double) -> Unit) {
        onBpm = onBpmUpdate
        exerciseClient.setUpdateCallback(callback)
        val config = ExerciseConfig.builder(ExerciseType.STRENGTH_TRAINING)
            .setDataTypes(setOf(DataType.HEART_RATE_BPM))
            .build()
        exerciseClient.startExerciseAsync(config)
    }

    fun stop() {
        onBpm = null
        exerciseClient.endExerciseAsync()
    }
}
