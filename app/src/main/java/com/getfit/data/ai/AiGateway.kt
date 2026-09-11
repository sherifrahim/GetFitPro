package com.getfit.data.ai

import com.getfit.data.prefs.Settings
import com.getfit.data.security.KeySlot
import com.getfit.data.security.SecureKeyStore

const val PROVIDER_ANTHROPIC = "anthropic"
const val PROVIDER_COMPAT = "compat"

/**
 * The one entry point the app uses for an AI request. Reads which provider is configured and routes
 * to [AnthropicClient] or [OpenAiCompatClient]; callers build content with [textBlock]/[imageBlock]
 * and never know which backend answered. Keeps "which key, which endpoint, which model" out of the
 * ViewModel entirely.
 */
class AiGateway(private val keys: SecureKeyStore) {

    /** True when the configured provider has everything it needs to make a call. */
    suspend fun isConfigured(settings: Settings): Boolean = when (settings.aiProvider) {
        PROVIDER_COMPAT -> settings.compatBaseUrl.isNotBlank() && settings.compatModel.isNotBlank()
        else -> !keys.getApiKey(KeySlot.ANTHROPIC).isNullOrBlank()
    }

    /** What's missing, phrased for the user, or null if nothing is. */
    suspend fun missingConfig(settings: Settings): String? = when (settings.aiProvider) {
        PROVIDER_COMPAT -> when {
            settings.compatBaseUrl.isBlank() -> "Pick a provider or enter an endpoint in Settings first."
            settings.compatModel.isBlank() -> "Enter a model name in Settings first."
            else -> null
        }
        else -> if (keys.getApiKey(KeySlot.ANTHROPIC).isNullOrBlank()) "Add your API key in Settings first." else null
    }

    suspend fun send(settings: Settings, system: String, userContent: List<ContentBlock>, maxTokens: Int): AiResult =
        when (settings.aiProvider) {
            PROVIDER_COMPAT -> OpenAiCompatClient.send(
                baseUrl = settings.compatBaseUrl,
                // A self-hosted endpoint may legitimately have no key; the client sends no header then.
                apiKey = keys.getApiKey(KeySlot.COMPAT).orEmpty(),
                model = settings.compatModel,
                system = system, userContent = userContent, maxTokens = maxTokens,
            )
            else -> AnthropicClient.send(
                apiKey = keys.getApiKey(KeySlot.ANTHROPIC).orEmpty(),
                model = settings.aiModel,
                system = system, userContent = userContent, maxTokens = maxTokens,
            )
        }
}
