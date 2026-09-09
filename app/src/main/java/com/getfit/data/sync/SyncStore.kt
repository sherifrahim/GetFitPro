package com.getfit.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val SYNC_STATE_KEY = stringPreferencesKey("syncState")

/**
 * DataStore-backed sync outbox + config — the client-side groundwork for Oracle server sync.
 * Deliberately NOT a new Room table: this app's database has no migration path set up yet
 * (version 1, no Migration objects), so adding a table here without one would either crash on the
 * next launch or require a destructive-migration wipe of the user's real workout data — neither is
 * acceptable for what's still just scaffolding. A DataStore blob (same pattern as Settings/Plan/
 * Session in Prefs.kt) needs no schema migration at all.
 */
class SyncStore(private val ds: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }

    val flow: Flow<SyncState> = ds.data.map { p ->
        p[SYNC_STATE_KEY]?.let { runCatching { json.decodeFromString<SyncState>(it) }.getOrNull() } ?: SyncState()
    }

    private suspend fun current(): SyncState = flow.first()
    private suspend fun save(state: SyncState) = ds.edit { it[SYNC_STATE_KEY] = json.encodeToString(state) }

    /** Stable per-install id, generated once and reused for every sync call. */
    suspend fun deviceId(): String {
        val s = current()
        if (s.deviceId.isNotBlank()) return s.deviceId
        val id = UUID.randomUUID().toString()
        save(s.copy(deviceId = id))
        return id
    }

    suspend fun setServerUrl(url: String) = save(current().copy(serverUrl = url.trim()))

    suspend fun enqueue(entityType: String, entityId: String, op: String, payload: String) {
        val s = current()
        val next = SyncOp(
            id = "op${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
            entityType = entityType, entityId = entityId, op = op, payload = payload,
            enqueuedAtMs = System.currentTimeMillis(),
        )
        // Cap the outbox so a long offline stretch can't grow it unboundedly.
        save(s.copy(queue = (s.queue + next).takeLast(500)))
    }

    suspend fun dequeue(ids: Set<String>) {
        val s = current()
        save(s.copy(queue = s.queue.filterNot { it.id in ids }))
    }

    suspend fun markSynced(atMs: Long) = save(current().copy(lastSyncAtMs = atMs, lastError = null))
    suspend fun markError(message: String) = save(current().copy(lastError = message))
}
