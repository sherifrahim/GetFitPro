package com.getfit.data.sync

import com.getfit.data.backup.BackupRepo
import com.getfit.data.backup.RestoreOutcome
import com.getfit.data.security.KeySlot
import com.getfit.data.security.SecureKeyStore
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class SyncOutcome {
    data class Uploaded(val version: Long, val unchanged: Boolean) : SyncOutcome()
    data class Restored(val version: Long, val records: Int) : SyncOutcome()
    data class Failure(val message: String) : SyncOutcome()
    object NotConfigured : SyncOutcome()
}

/**
 * Snapshot sync. Every write path in the app calls [noteChange]; that marks the state dirty and,
 * if auto-sync is on and a server is configured, schedules an upload a short while later — so a
 * burst of edits (logging a session's sets, reordering the plan) becomes one upload, not ten.
 *
 * The upload is the app's own backup document, produced by [BackupRepo]. Restoring from the cloud
 * is the same replace-everything operation as restoring a local backup, with the same confirm gate
 * in the UI. Nothing here can run without a server URL AND a token; before both exist every call
 * is a no-op that reports [SyncOutcome.NotConfigured].
 */
class SyncRepo(
    private val store: SyncStore,
    private val keys: SecureKeyStore,
    private val backupRepo: BackupRepo,
    private val scope: CoroutineScope,
) {
    val state: Flow<SyncState> = store.flow
    val hasToken: Flow<Boolean> = keys.hasSyncKey

    // One upload at a time; a second syncNow() during an upload waits rather than double-posting.
    private val uploadLock = Mutex()
    private var pending: Job? = null

    suspend fun setServerUrl(url: String) = store.setServerUrl(url)
    suspend fun setToken(token: String) = keys.setApiKey(token, KeySlot.SYNC)
    suspend fun clearToken() = keys.clear(KeySlot.SYNC)
    suspend fun setAutoSync(on: Boolean) = store.setAutoSync(on)

    private suspend fun configured(): Boolean =
        store.flow.first().serverUrl.isNotBlank() && !keys.getApiKey(KeySlot.SYNC).isNullOrBlank()

    /**
     * Something the user owns changed. Never throws, never blocks the caller's write path — the
     * DataStore flag write is wrapped, and the upload itself is scheduled, not awaited.
     */
    fun noteChange() {
        scope.launch {
            runCatching { store.markDirty() }
            val s = store.flow.first()
            if (!s.autoSync || !configured()) return@launch
            pending?.cancel()
            pending = launch {
                delay(DEBOUNCE_MS)
                syncNow()
            }
        }
    }

    /** Uploads the current snapshot if it differs from the last one uploaded. */
    suspend fun syncNow(): SyncOutcome = uploadLock.withLock {
        if (!configured()) return SyncOutcome.NotConfigured
        val s = store.flow.first()
        val token = keys.getApiKey(KeySlot.SYNC).orEmpty()
        val body = runCatching { backupRepo.exportText() }.getOrElse {
            return fail("Couldn't build the snapshot: ${it.message}")
        }
        val sha = sha256(body)
        // Same bytes as last time and nothing marked dirty: nothing to send.
        if (!s.dirty && sha == s.lastUploadedSha && s.remoteVersion > 0) {
            return SyncOutcome.Uploaded(s.remoteVersion, unchanged = true)
        }
        return SyncClient.putSnapshot(s.serverUrl, token, store.deviceId(), body).fold(
            onSuccess = { r ->
                store.markSynced(r.version, sha, System.currentTimeMillis())
                SyncOutcome.Uploaded(r.version, r.unchanged)
            },
            onFailure = { fail(it.message ?: "Upload failed.") },
        )
    }

    /** Pulls the newest snapshot and REPLACES local data with it. The UI confirms before calling. */
    suspend fun restoreFromCloud(): SyncOutcome {
        if (!configured()) return SyncOutcome.NotConfigured
        val s = store.flow.first()
        val token = keys.getApiKey(KeySlot.SYNC).orEmpty()
        val remote = SyncClient.getLatest(s.serverUrl, token).getOrElse { return fail(it.message ?: "Download failed.") }
        return when (val r = backupRepo.restoreFromText(remote.body)) {
            is RestoreOutcome.Success -> {
                // What's on the device now IS the server's latest — record that so the next
                // auto-upload is a no-op instead of re-posting identical bytes.
                store.markSynced(remote.version, sha256(remote.body), System.currentTimeMillis())
                SyncOutcome.Restored(remote.version, r.info.records)
            }
            is RestoreOutcome.Failure -> fail(r.message)
        }
    }

    /** Health check only — no token needed, no data moves. For the Settings "Test" button. */
    suspend fun testConnection(url: String): Result<Unit> = SyncClient.health(url)

    private suspend fun fail(message: String): SyncOutcome {
        runCatching { store.markError(message) }
        return SyncOutcome.Failure(message)
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    companion object {
        /** Long enough to coalesce a whole logged session; short enough that a phone put down after training still syncs. */
        const val DEBOUNCE_MS = 20_000L
    }
}
