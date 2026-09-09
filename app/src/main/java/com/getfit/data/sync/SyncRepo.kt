package com.getfit.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** The "repo layer" the other repos enqueue changes through, and the ViewModel drives to actually
 *  sync. Everything here is safe to call with no server configured — it just queues locally and
 *  [syncNow] reports back that there's nothing to sync to yet, rather than failing loudly. */
class SyncRepo(private val store: SyncStore) {
    val state: Flow<SyncState> = store.flow

    suspend fun setServerUrl(url: String) = store.setServerUrl(url)

    /** Fire-and-forget: never throws, so a queueing hiccup can never take down the write path it's
     *  attached to (session save, target add/remove, ...). */
    suspend fun enqueue(entityType: String, entityId: String, op: String, payload: String) {
        runCatching { store.enqueue(entityType, entityId, op, payload) }
    }

    /** Pushes whatever's queued. */
    suspend fun syncNow(): SyncClientResult {
        val s = state.first()
        if (s.serverUrl.isBlank()) {
            val msg = "No sync server set yet — add one above once your Oracle server is ready."
            store.markError(msg)
            return SyncClientResult.Failure(msg)
        }
        if (s.queue.isEmpty()) {
            store.markSynced(System.currentTimeMillis())
            return SyncClientResult.Success(0)
        }
        val deviceId = store.deviceId()
        return when (val result = SyncClient.push(s.serverUrl, deviceId, s.queue)) {
            is SyncClientResult.Success -> {
                store.dequeue(s.queue.map { it.id }.toSet())
                store.markSynced(System.currentTimeMillis())
                result
            }
            is SyncClientResult.Failure -> {
                store.markError(result.message)
                result
            }
        }
    }
}
