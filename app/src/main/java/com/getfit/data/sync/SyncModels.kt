package com.getfit.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One locally-made change waiting to be pushed to the sync server. Enqueued the moment the user's
 *  own data changes (a saved session, a new/removed target) — enqueueing itself never blocks or
 *  fails the local write it's attached to. */
@Serializable
data class SyncOp(
    val id: String,
    val entityType: String, // "session" | "target"
    val entityId: String,
    val op: String,         // "upsert" | "delete"
    val payload: String,    // JSON snapshot of the entity at enqueue time (blank for deletes)
    val enqueuedAtMs: Long,
)

/** Persisted sync configuration + outbox. serverUrl stays blank — and sync stays fully inert —
 *  until the user sets their Oracle endpoint in Settings. */
@Serializable
data class SyncState(
    val serverUrl: String = "",
    val deviceId: String = "",
    val queue: List<SyncOp> = emptyList(),
    val lastSyncAtMs: Long = 0,
    val lastError: String? = null,
)

@Serializable
data class SyncSessionSnapshot(
    val id: String,
    val dateMs: Long,
    val name: String,
    val durationSec: Int,
    val totalSets: Int,
    val volume: Int,
    val prs: Int,
)

@Serializable
data class SyncTargetSnapshot(
    val id: String,
    val exId: String,
    val target: Double,
    val start: Double,
    val startDMs: Long,
    val weeks: Int,
)

@Serializable
data class SyncPushRequest(
    @SerialName("device_id") val deviceId: String,
    val ops: List<SyncOp>,
)

@Serializable
data class SyncPushResponse(
    val accepted: Int = 0,
    val error: String? = null,
)
