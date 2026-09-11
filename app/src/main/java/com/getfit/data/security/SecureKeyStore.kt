package com.getfit.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "forge_ai_key_wrap"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_BITS = 128

/** Which provider a stored key belongs to. Each slot is its own ciphertext + IV pair. */
enum class KeySlot(internal val prefix: String) {
    /** Anthropic key — the original slot, so its preference names are unchanged for existing installs. */
    ANTHROPIC("ai_api_key"),
    /** Key for the OpenAI-compatible provider (OpenAI, Groq, DeepSeek, OpenRouter, self-hosted, ...). */
    COMPAT("ai_compat_key"),
}

private fun ctKey(slot: KeySlot) = stringPreferencesKey(slot.prefix + "_ct")
private fun ivKey(slot: KeySlot) = stringPreferencesKey(slot.prefix + "_iv")

/**
 * Stores the user's own AI API key encrypted at rest. The AES key itself lives in the Android
 * Keystore (hardware-backed on most devices) and never leaves it — only the ciphertext + IV are
 * persisted, in the same DataStore the rest of the app's preferences use. Deliberately a separate
 * class from SettingsStore: this holds secret material, not a UI preference, and the encrypt/
 * decrypt path is worth keeping in one obvious place.
 */
class SecureKeyStore(private val ds: DataStore<Preferences>) {

    val hasKey: Flow<Boolean> = ds.data.map { !it[ctKey(KeySlot.ANTHROPIC)].isNullOrEmpty() }
    val hasCompatKey: Flow<Boolean> = ds.data.map { !it[ctKey(KeySlot.COMPAT)].isNullOrEmpty() }

    suspend fun setApiKey(plaintext: String, slot: KeySlot = KeySlot.ANTHROPIC) {
        val trimmed = plaintext.trim()
        if (trimmed.isEmpty()) { clear(slot); return }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        val ciphertext = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
        ds.edit {
            it[ctKey(slot)] = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            it[ivKey(slot)] = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        }
    }

    suspend fun getApiKey(slot: KeySlot = KeySlot.ANTHROPIC): String? {
        val prefs = ds.data.first()
        val ciphertextB64 = prefs[ctKey(slot)] ?: return null
        val ivB64 = prefs[ivKey(slot)] ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_BITS, Base64.decode(ivB64, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            String(cipher.doFinal(Base64.decode(ciphertextB64, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    suspend fun clear(slot: KeySlot = KeySlot.ANTHROPIC) = ds.edit {
        it.remove(ctKey(slot))
        it.remove(ivKey(slot))
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
