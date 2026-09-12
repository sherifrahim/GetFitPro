package com.getfit.wear.data

import com.getfit.data.wear.WearSessionUpload
import com.getfit.data.wear.WearPaths
import com.getfit.data.wear.WatchAction
import com.getfit.data.wear.SessionSnapshot
import com.getfit.data.wear.HeartRateBatch
import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Thin wrapper over the Data Layer API's MessageClient — one-shot fire-and-forget messages, not a
 * continuous stream (see docs/wear-companion-design.md for MessageClient vs DataClient here).
 * Deliberately callback-based (Task.addOnSuccessListener) rather than kotlinx-coroutines-play-
 * services' .await(), to avoid pulling in an extra unverified dependency for one call site — same
 * reasoning as this project's HttpURLConnection-over-Retrofit choice elsewhere.
 */
class WatchMessenger(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    fun sendAction(action: WatchAction) = send(WearPaths.WATCH_ACTION, json.encodeToString(action))
    fun sendHeartRateBatch(batch: HeartRateBatch) = send(WearPaths.HEART_RATE, json.encodeToString(batch))
    /** A workout finished on the watch alone. Sent again on every reconnect until the phone acks it. */
    fun sendUpload(upload: WearSessionUpload) = send(WearPaths.SESSION_UPLOAD, json.encodeToString(upload))

    /** Whether any phone is currently reachable over the Data Layer (Bluetooth or same Wi-Fi). */
    fun hasConnectedNode(onResult: (Boolean) -> Unit) {
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes -> onResult(nodes.isNotEmpty()) }
            .addOnFailureListener { onResult(false) }
    }

    private fun send(path: String, jsonBody: String) {
        val payload = jsonBody.toByteArray()
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node -> messageClient.sendMessage(node.id, path, payload) }
        }
    }

    fun decodeSnapshot(bytes: ByteArray): SessionSnapshot? =
        runCatching { json.decodeFromString<SessionSnapshot>(String(bytes)) }.getOrNull()

    /** Registers a listener that calls [onSnapshot] whenever the phone sends a new session
     *  snapshot. Returns the listener so the caller can unregister it via [stopListening] when its
     *  own lifecycle ends (see the DisposableEffect in MainActivity.kt). */
    fun listenForSnapshots(onSnapshot: (SessionSnapshot) -> Unit): MessageClient.OnMessageReceivedListener {
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == WearPaths.SESSION_SNAPSHOT) decodeSnapshot(event.data)?.let(onSnapshot)
        }
        messageClient.addListener(listener)
        return listener
    }

    fun stopListening(listener: MessageClient.OnMessageReceivedListener) {
        messageClient.removeListener(listener)
    }
}
