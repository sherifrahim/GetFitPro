package com.getfit.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.getfit.data.db.Curated
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Settings mirror of the prototype state (units/sound/haptics/autorest/restDefault/intensity). */
data class Settings(
    val units: String = "kg",
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val autorest: Boolean = true,
    val restDefault: Int = 60,
    val intensity: String = "Moderate",
    val onboarded: Boolean = false,
    val seeded: Boolean = false,
    // AI review: the API key itself lives in SecureKeyStore (encrypted), not here — this is just
    // which model to call, a non-secret preference like any other.
    val aiModel: String = "claude-opus-5",
    // Which backend the AI features talk to: "anthropic" (Messages API) or "compat" (any
    // OpenAI-compatible Chat Completions endpoint — OpenAI, Groq, DeepSeek, OpenRouter, a self-hosted
    // Ollama...). The compat key lives in SecureKeyStore under its own slot.
    val aiProvider: String = "anthropic",
    val compatBaseUrl: String = "",
    val compatModel: String = "",
    // Profile (Hevy: Edit Profile + Private data). Only what the app can actually use: the name
    // for the greeting and initials, body weight/age/sex for the calorie estimate.
    val name: String = "",
    val bodyWeightKg: Double = 0.0,
    val heightCm: Int = 0,
    val birthYear: Int = 0,
    val sex: String = "",          // "male", "female" or "" (unset)
    // Workout preferences (Hevy: Workout Settings).
    val weeklyGoal: Int = 5,       // sessions per week the streak strip and Home aim for
    val keepAwake: Boolean = false,
    val prNotify: Boolean = true,  // celebrate a PR the moment a set is logged
    val weekStartsMonday: Boolean = true,
) {
    val initials: String
        get() = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }

    fun ageYears(now: java.util.Calendar = java.util.Calendar.getInstance()): Int =
        if (birthYear in 1900..now.get(java.util.Calendar.YEAR)) now.get(java.util.Calendar.YEAR) - birthYear else 0

    fun isMale(): Boolean? = when (sex) { "male" -> true; "female" -> false; else -> null }
}

private object Keys {
    val UNITS = stringPreferencesKey("units")
    val SOUND = booleanPreferencesKey("sound")
    val HAPTICS = booleanPreferencesKey("haptics")
    val AUTOREST = booleanPreferencesKey("autorest")
    val REST_DEFAULT = intPreferencesKey("restDefault")
    val INTENSITY = stringPreferencesKey("intensity")
    val ONBOARDED = booleanPreferencesKey("onboarded")
    val SEEDED = booleanPreferencesKey("seeded")
    val AI_MODEL = stringPreferencesKey("aiModel")
    val AI_PROVIDER = stringPreferencesKey("aiProvider")
    val COMPAT_BASE_URL = stringPreferencesKey("compatBaseUrl")
    val COMPAT_MODEL = stringPreferencesKey("compatModel")
    val NAME = stringPreferencesKey("profileName")
    val BODY_WEIGHT = doublePreferencesKey("bodyWeightKg")
    val HEIGHT = intPreferencesKey("heightCm")
    val BIRTH_YEAR = intPreferencesKey("birthYear")
    val SEX = stringPreferencesKey("sex")
    val WEEKLY_GOAL = intPreferencesKey("weeklyGoal")
    val KEEP_AWAKE = booleanPreferencesKey("keepAwake")
    val PR_NOTIFY = booleanPreferencesKey("prNotify")
    val WEEK_MONDAY = booleanPreferencesKey("weekStartsMonday")
    val PLAN = stringPreferencesKey("plan") // legacy single plan: read once by RoutinesStore, then removed
    val ROUTINES = stringPreferencesKey("routines")
    val SESSION = stringPreferencesKey("session")
}

class SettingsStore(private val ds: DataStore<Preferences>) {
    val flow: Flow<Settings> = ds.data.map { p ->
        Settings(
            units = p[Keys.UNITS] ?: "kg",
            sound = p[Keys.SOUND] ?: true,
            haptics = p[Keys.HAPTICS] ?: true,
            autorest = p[Keys.AUTOREST] ?: true,
            restDefault = p[Keys.REST_DEFAULT] ?: 60,
            intensity = p[Keys.INTENSITY] ?: "Moderate",
            onboarded = p[Keys.ONBOARDED] ?: false,
            seeded = p[Keys.SEEDED] ?: false,
            aiModel = p[Keys.AI_MODEL] ?: "claude-opus-5",
            aiProvider = p[Keys.AI_PROVIDER] ?: "anthropic",
            compatBaseUrl = p[Keys.COMPAT_BASE_URL] ?: "",
            compatModel = p[Keys.COMPAT_MODEL] ?: "",
            name = p[Keys.NAME] ?: "",
            bodyWeightKg = p[Keys.BODY_WEIGHT] ?: 0.0,
            heightCm = p[Keys.HEIGHT] ?: 0,
            birthYear = p[Keys.BIRTH_YEAR] ?: 0,
            sex = p[Keys.SEX] ?: "",
            weeklyGoal = p[Keys.WEEKLY_GOAL] ?: 5,
            keepAwake = p[Keys.KEEP_AWAKE] ?: false,
            prNotify = p[Keys.PR_NOTIFY] ?: true,
            weekStartsMonday = p[Keys.WEEK_MONDAY] ?: true,
        )
    }

    suspend fun setUnits(v: String) = ds.edit { it[Keys.UNITS] = v }
    suspend fun setSound(v: Boolean) = ds.edit { it[Keys.SOUND] = v }
    suspend fun setHaptics(v: Boolean) = ds.edit { it[Keys.HAPTICS] = v }
    suspend fun setAutorest(v: Boolean) = ds.edit { it[Keys.AUTOREST] = v }
    suspend fun setRestDefault(v: Int) = ds.edit { it[Keys.REST_DEFAULT] = v }
    suspend fun setIntensity(v: String) = ds.edit { it[Keys.INTENSITY] = v; it[Keys.REST_DEFAULT] = Curated.INTENSITY[v]?.rest ?: 60 }
    suspend fun setOnboarded(v: Boolean) = ds.edit { it[Keys.ONBOARDED] = v }
    suspend fun setSeeded(v: Boolean) = ds.edit { it[Keys.SEEDED] = v }
    suspend fun setAiModel(v: String) = ds.edit { it[Keys.AI_MODEL] = v }
    suspend fun setAiProvider(v: String) = ds.edit { it[Keys.AI_PROVIDER] = v }
    suspend fun setCompatBaseUrl(v: String) = ds.edit { it[Keys.COMPAT_BASE_URL] = v.trim().trimEnd('/') }
    suspend fun setCompatModel(v: String) = ds.edit { it[Keys.COMPAT_MODEL] = v.trim() }
    suspend fun setName(v: String) = ds.edit { it[Keys.NAME] = v.trim() }
    suspend fun setBodyWeightKg(v: Double) = ds.edit { it[Keys.BODY_WEIGHT] = v.coerceIn(0.0, 400.0) }
    suspend fun setHeightCm(v: Int) = ds.edit { it[Keys.HEIGHT] = v.coerceIn(0, 272) }
    suspend fun setBirthYear(v: Int) = ds.edit { it[Keys.BIRTH_YEAR] = v }
    suspend fun setSex(v: String) = ds.edit { it[Keys.SEX] = v }
    suspend fun setWeeklyGoal(v: Int) = ds.edit { it[Keys.WEEKLY_GOAL] = v.coerceIn(1, 7) }
    suspend fun setKeepAwake(v: Boolean) = ds.edit { it[Keys.KEEP_AWAKE] = v }
    suspend fun setPrNotify(v: Boolean) = ds.edit { it[Keys.PR_NOTIFY] = v }
    suspend fun setWeekStartsMonday(v: Boolean) = ds.edit { it[Keys.WEEK_MONDAY] = v }

    /** Reset preferences to defaults (proto clearAll). */
    suspend fun resetToDefaults() = ds.edit {
        it[Keys.UNITS] = "kg"; it[Keys.SOUND] = true; it[Keys.HAPTICS] = true
        it[Keys.AUTOREST] = true; it[Keys.REST_DEFAULT] = 60; it[Keys.INTENSITY] = "Moderate"
        // onboarded + seeded are intentionally preserved. The AI key (SecureKeyStore) and model
        // choice are left untouched too — "clear all data" means workout data, not app config.
    }
}

@Serializable
data class PlanItemData(val id: String, val sets: Int, val reps: String)

/**
 * A named, ordered workout: what Hevy calls a routine. The single "Push Day" plan the prototype
 * shipped is now just the first of these. [items] is the same shape the builder and the session
 * engine always consumed, so a routine's items feed straight into SessionController.start().
 */
@Serializable
data class Routine(
    val id: String,
    val name: String,
    val items: List<PlanItemData>,
    val note: String = "",
) {
    val setsTotal: Int get() = items.sumOf { it.sets }
}

/**
 * Every routine plus which one is "up next". [currentId] rotates forward through [routines] each
 * time a session for the current routine is saved, so the Home card and the watch's Idle screen
 * always name the workout the user is expected to do next — and either device can override it.
 */
@Serializable
data class RoutinesData(
    val routines: List<Routine> = emptyList(),
    val currentId: String = "",
) {
    val current: Routine? get() = routines.firstOrNull { it.id == currentId } ?: routines.firstOrNull()

    /** Routines after the current one, in rotation order, wrapping around — the "up next" list. */
    val upcoming: List<Routine>
        get() {
            val cur = current ?: return emptyList()
            val i = routines.indexOfFirst { it.id == cur.id }
            if (i < 0 || routines.size < 2) return emptyList()
            return (1 until routines.size).map { routines[(i + it) % routines.size] }
        }

    /** The routine that follows [afterId] in rotation order (itself if it's the only one). */
    fun nextAfter(afterId: String): Routine? {
        val i = routines.indexOfFirst { it.id == afterId }
        if (i < 0) return current
        return routines[(i + 1) % routines.size]
    }
}

class RoutinesStore(private val ds: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }

    val flow: Flow<RoutinesData> = ds.data.map { p -> read(p) }

    /**
     * A missing `routines` key means either a fresh install or an install from before routines
     * existed. In the second case the legacy single `plan` key is still there, possibly edited by
     * the user, so it becomes the Push Day routine's items rather than being thrown away.
     */
    private fun read(p: Preferences): RoutinesData {
        p[Keys.ROUTINES]?.let { raw ->
            runCatching { json.decodeFromString<RoutinesData>(raw) }.getOrNull()?.let { d ->
                if (d.routines.isNotEmpty()) return d
            }
        }
        val legacyPlan = p[Keys.PLAN]?.let { runCatching { json.decodeFromString<List<PlanItemData>>(it) }.getOrNull() }
        return defaults(legacyPlan)
    }

    suspend fun set(data: RoutinesData) = ds.edit {
        it[Keys.ROUTINES] = json.encodeToString(data)
        it.remove(Keys.PLAN)
    }

    /** Read-modify-write on the current value. Every routine mutation goes through here. */
    suspend fun update(fn: (RoutinesData) -> RoutinesData) = ds.edit {
        it[Keys.ROUTINES] = json.encodeToString(fn(read(it)))
        it.remove(Keys.PLAN)
    }

    suspend fun resetToDefault() = set(defaults(null))

    companion object {
        fun defaults(legacyPushItems: List<PlanItemData>?): RoutinesData {
            val routines = Curated.DEFAULT_ROUTINES.map { r ->
                val items = if (r.id == Curated.DEFAULT_ROUTINE_ID && !legacyPushItems.isNullOrEmpty()) legacyPushItems
                else r.items.map { PlanItemData(it.id, it.sets, it.reps) }
                Routine(r.id, r.name, items)
            }
            return RoutinesData(routines, Curated.DEFAULT_ROUTINE_ID)
        }
    }
}

/** Persists the in-progress guided session so it survives process death. */
class SessionStore(private val ds: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(state: com.getfit.domain.SessionState) =
        ds.edit { it[Keys.SESSION] = json.encodeToString(state) }

    suspend fun clear() = ds.edit { it.remove(Keys.SESSION) }

    suspend fun load(): com.getfit.domain.SessionState? =
        ds.data.first()[Keys.SESSION]?.let {
            runCatching { json.decodeFromString<com.getfit.domain.SessionState>(it) }.getOrNull()
        }
}
