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

private object Keys {
    val CIPHERTEXT = stringPreferencesKey("ai_api_key_ct")
    val IV = stringPreferencesKey("ai_api_key_iv")
}

/**
 * Stores the user's own AI API key encrypted at rest. The AES key itself lives in the Android
 * Keystore (hardware-backed on most devices) and never leaves it — only the ciphertext + IV are
 * persisted, in the same DataStore the rest of the app's preferences use. Deliberately a separate
 * class from SettingsStore: this holds secret material, not a UI preference, and the encrypt/
 * decrypt path is worth keeping in one obvious place.
 */
class SecureKeyStore(private val ds: DataStore<Preferences>) {

    val hasKey: Flow<Boolean> = ds.data.map { !it[Keys.CIPHERTEXT].isNullOrEmpty() }

    suspend fun setApiKey(plaintext: String) {
        val trimmed = plaintext.trim()
        if (trimmed.isEmpty()) { clear(); return }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        val ciphertext = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
        ds.edit {
            it[Keys.CIPHERTEXT] = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            it[Keys.IV] = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        }
    }

    suspend fun getApiKey(): String? {
        val prefs = ds.data.first()
        val ciphertextB64 = prefs[Keys.CIPHERTEXT] ?: return null
        val ivB64 = prefs[Keys.IV] ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_BITS, Base64.decode(ivB64, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            String(cipher.doFinal(Base64.decode(ciphertextB64, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    suspend fun clear() = ds.edit {
        it.remove(Keys.CIPHERTEXT)
        it.remove(Keys.IV)
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
