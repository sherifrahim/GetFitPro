package com.getfit.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    val aiModel: String = "claude-sonnet-4-5",
)

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
    val PLAN = stringPreferencesKey("plan")
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
            aiModel = p[Keys.AI_MODEL] ?: "claude-sonnet-4-5",
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

class PlanStore(private val ds: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }
    private val default = Curated.DEFAULT_PLAN.map { PlanItemData(it.id, it.sets, it.reps) }

    val flow: Flow<List<PlanItemData>> = ds.data.map { p ->
        p[Keys.PLAN]?.let { runCatching { json.decodeFromString<List<PlanItemData>>(it) }.getOrNull() } ?: default
    }

    suspend fun set(list: List<PlanItemData>) =
        ds.edit { it[Keys.PLAN] = json.encodeToString(list) }

    suspend fun resetToDefault() = set(default)
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
