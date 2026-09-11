package com.getfit.data.sync

import kotlinx.serialization.Serializable

/**
 * Persisted cloud-sync state. The model is SNAPSHOT sync: whenever the user's data changes the app
 * uploads its full backup document (the same "forge.backup" JSON the on-device backup writes) to
 * the forge-sync server, which keeps the last N versions; restore pulls the newest one back. One
 * user, whole snapshots, last-writer-wins — see server/forge-sync/main.py for the other half.
 *
 * The bearer token is NOT here; it lives in SecureKeyStore under KeySlot.SYNC.
 */
@Serializable
data class SyncState(
    val serverUrl: String = "",
    val deviceId: String = "",
    /** Auto-upload after changes (debounced). Off means only "Sync now" uploads. */
    val autoSync: Boolean = true,
    /** Something changed locally since the last successful upload. */
    val dirty: Boolean = false,
    val lastSyncAtMs: Long = 0,
    /** Server-assigned version of the last snapshot we uploaded or restored. */
    val remoteVersion: Long = 0,
    /** sha256 of the last uploaded body — lets a no-op upload be skipped without a round trip. */
    val lastUploadedSha: String = "",
    val lastError: String? = null,
)

/** Server's reply to PUT /v1/snapshot. */
@Serializable
data class SnapshotPutResponse(
    val version: Long,
    val created_at: String = "",
    val sha256: String = "",
    val bytes: Int = 0,
    val unchanged: Boolean = false,
)

/** GET /v1/snapshot/latest: the body plus the metadata headers. */
data class RemoteSnapshot(val body: String, val version: Long, val createdAt: String, val sha256: String)
