package com.getfit.wear.data

import android.content.Context
import com.getfit.domain.SessionState
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The watch's own little persistence, for the standalone case: the last routine list the phone
 * sent (so workouts can start with no phone in range), an in-progress local session (survives
 * process death like the phone's does), and finished workouts waiting to reach the phone.
 *
 * SharedPreferences + JSON on purpose — a few KB, three keys, no Room/DataStore dependency added
 * to the watch module for it.
 */
class WatchStore(context: Context) {
    private val prefs = context.getSharedPreferences("forge_watch", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** What the phone last told us about routines; enough to run one without the phone. */
    @Serializable
    data class RoutineCache(
        val routines: List<WearRoutine> = emptyList(),
        val currentRoutineId: String = "",
        val units: String = "kg",
        val restDefault: Int = 60,
    )

    fun routines(): RoutineCache =
        prefs.getString(KEY_ROUTINES, null)?.let { runCatching { json.decodeFromString<RoutineCache>(it) }.getOrNull() } ?: RoutineCache()

    /** Only snapshots that carry routines with items overwrite the cache; an older phone's empty list must not wipe it. */
    fun rememberRoutines(snapshot: SessionSnapshot) {
        if (snapshot.routines.none { it.items.isNotEmpty() }) return
        val cache = RoutineCache(snapshot.routines, snapshot.currentRoutineId, snapshot.units, snapshot.restDefault)
        prefs.edit().putString(KEY_ROUTINES, json.encodeToString(cache)).apply()
    }

    fun localSession(): SessionState? =
        prefs.getString(KEY_SESSION, null)?.let { runCatching { json.decodeFromString<SessionState>(it) }.getOrNull() }

    fun saveLocalSession(state: SessionState?) {
        if (state == null) prefs.edit().remove(KEY_SESSION).apply()
        else prefs.edit().putString(KEY_SESSION, json.encodeToString(state)).apply()
    }

    fun pendingUploads(): List<WearSessionUpload> =
        prefs.getString(KEY_PENDING, null)?.let { runCatching { json.decodeFromString<List<WearSessionUpload>>(it) }.getOrNull() } ?: emptyList()

    fun enqueueUpload(upload: WearSessionUpload) {
        val list = pendingUploads().filterNot { it.id == upload.id } + upload
        prefs.edit().putString(KEY_PENDING, json.encodeToString(list)).apply()
    }

    fun removeUpload(id: String) {
        val list = pendingUploads().filterNot { it.id == id }
        prefs.edit().putString(KEY_PENDING, json.encodeToString(list)).apply()
    }

    private companion object {
        const val KEY_ROUTINES = "routines"
        const val KEY_SESSION = "local_session"
        const val KEY_PENDING = "pending_uploads"
    }
}
