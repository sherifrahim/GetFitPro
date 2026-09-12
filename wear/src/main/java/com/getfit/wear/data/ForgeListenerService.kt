package com.getfit.wear.data

import com.getfit.data.wear.WearPaths
import com.getfit.data.wear.UploadAck
import com.getfit.data.wear.SessionSnapshot
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
 *
 * [acked] carries upload ids the phone has confirmed, so the activity can refresh its pending count.
 */
object SnapshotBus {
    val snapshot = MutableStateFlow<SessionSnapshot?>(null)
    val acked = MutableStateFlow<String?>(null)
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
 * Every snapshot also refreshes the routine cache that standalone workouts run from, and upload
 * acks retire pending sessions. Registered in the manifest with path filters, so Play Services
 * only wakes us for our own paths.
 */
class ForgeListenerService : WearableListenerService() {
    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            WearPaths.SESSION_SNAPSHOT -> {
                val snap = runCatching { json.decodeFromString<SessionSnapshot>(String(event.data)) }.getOrNull() ?: return
                WatchStore(this).rememberRoutines(snap)
                SnapshotBus.snapshot.value = snap
                if (snap.active && !MainActivity.inForeground) {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    )
                }
            }
            WearPaths.UPLOAD_ACK -> {
                val ack = runCatching { json.decodeFromString<UploadAck>(String(event.data)) }.getOrNull() ?: return
                WatchStore(this).removeUpload(ack.id)
                SnapshotBus.acked.value = ack.id
            }
        }
    }
}
