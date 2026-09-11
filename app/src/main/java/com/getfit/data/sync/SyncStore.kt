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
 * DataStore-backed sync state. A single JSON blob under one preference key (same pattern as
 * Settings/Plan/Session in Prefs.kt) — no Room table, no schema migration. The old op-based
 * outbox that used to live in this same key decodes with `ignoreUnknownKeys`, so an install that
 * still has one simply drops it.
 */
class SyncStore(private val ds: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }

    val flow: Flow<SyncState> = ds.data.map { p ->
        p[SYNC_STATE_KEY]?.let { runCatching { json.decodeFromString<SyncState>(it) }.getOrNull() } ?: SyncState()
    }

    private suspend fun current(): SyncState = flow.first()
    private suspend fun save(state: SyncState) = ds.edit { it[SYNC_STATE_KEY] = json.encodeToString(state) }

    /** Stable per-install id, generated once. Sent as X-Device-Id so the server can label versions. */
    suspend fun deviceId(): String {
        val s = current()
        if (s.deviceId.isNotBlank()) return s.deviceId
        val id = UUID.randomUUID().toString()
        save(s.copy(deviceId = id))
        return id
    }

    suspend fun setServerUrl(url: String) = save(current().copy(serverUrl = url.trim().trimEnd('/'), lastError = null))
    suspend fun setAutoSync(on: Boolean) = save(current().copy(autoSync = on))
    suspend fun markDirty() = save(current().copy(dirty = true))
    suspend fun markSynced(version: Long, sha: String, atMs: Long) =
        save(current().copy(dirty = false, remoteVersion = version, lastUploadedSha = sha, lastSyncAtMs = atMs, lastError = null))
    suspend fun markError(message: String) = save(current().copy(lastError = message))
}
