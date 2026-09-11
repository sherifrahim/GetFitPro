package com.getfit.wear.data

import android.content.Intent
import com.getfit.wear.MainActivity
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

/**
 * The latest session snapshot the phone sent, shared between [ForgeListenerService] (which
 * receives it, app open or not) and the activity (which renders it). A process-wide StateFlow is
 * enough: the service and the activity always share a process, and the phone re-sends the full
 * snapshot on request, so nothing here needs to survive process death.
 */
object SnapshotBus {
    val snapshot = MutableStateFlow<SessionSnapshot?>(null)
}

/**
 * Receives the phone's session snapshots even when the watch app is not running — which is the
 * normal case: a workout starts on the phone while the watch is on the wrist showing the watch face.
 *
 * Before this existed the only listener lived in the activity, so a snapshot sent while the app was
 * closed went to nobody, and opening the app afterwards showed Idle until the phone's NEXT state
 * change. On a real Galaxy Watch that read as "the watch just says open Forge on your phone".
 *
 * On an active snapshot this launches the activity so the workout appears on the wrist by itself.
 * Registered in the manifest with a path filter, so Play Services only wakes us for our own path.
 */
class ForgeListenerService : WearableListenerService() {
    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WearPaths.SESSION_SNAPSHOT) return
        val snap = runCatching { json.decodeFromString<SessionSnapshot>(String(event.data)) }.getOrNull() ?: return
        SnapshotBus.snapshot.value = snap
        if (snap.active && !MainActivity.inForeground) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
    }
}
