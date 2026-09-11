package com.getfit.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val ANTHROPIC_ENDPOINT = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION = "2023-06-01"

/** The default model. Current generation; thinks adaptively by default; supports vision. */
const val DEFAULT_AI_MODEL = "claude-opus-5"

/**
 * Server-side fallbacks: if the model's safety classifiers decline a request, the API re-runs it on
 * Anthropic's recommended substitute (routed by refusal category) instead of returning the refusal.
 * Only the `"default"` scalar form is used, which needs exactly this beta header. Gated to the
 * models that accept the parameter — sending it to an older model would 400.
 */
private const val FALLBACK_BETA = "server-side-fallback-2026-07-01"
private fun supportsDefaultFallbacks(model: String): Boolean =
    model.startsWith("claude-opus-5") || model.startsWith("claude-fable-5")

sealed class AiResult {
    data class Success(val text: String) : AiResult()
    data class Failure(val message: String) : AiResult()
}

/** A user-turn content block. Build with [textBlock] / [imageBlock]. */
typealias ContentBlock = JsonObject

fun textBlock(text: String): ContentBlock = buildJsonObject {
    put("type", "text")
    put("text", text)
}

/** [base64] must be the raw JPEG/PNG bytes base64-encoded with no line wrapping. */
fun imageBlock(base64: String, mediaType: String = "image/jpeg"): ContentBlock = buildJsonObject {
    put("type", "image")
    putJsonObject("source") {
        put("type", "base64")
        put("media_type", mediaType)
        put("data", base64)
    }
}

@Serializable
private data class ResponseBlock(val type: String = "", val text: String? = null)

@Serializable
private data class ApiError(val type: String? = null, val message: String? = null)

@Serializable
private data class StopDetails(val type: String? = null, val category: String? = null, val explanation: String? = null)

@Serializable
private data class ApiResponse(
    val content: List<ResponseBlock> = emptyList(),
    @SerialName("stop_reason") val stopReason: String? = null,
    @SerialName("stop_details") val stopDetails: StopDetails? = null,
    val error: ApiError? = null,
)

/**
 * The one place Forge talks to Anthropic's Messages API, with the user's own key. Plain
 * HttpURLConnection on purpose — this project avoids adding a networking library where a simpler
 * built-in approach works (see CLAUDE.md). Both the workout review and the body check go through
 * [send]; they differ only in system prompt and content blocks.
 *
 * Request-shape notes, so nobody "simplifies" them away:
 *   * `thinking` is deliberately NOT sent. On the default model (Claude Opus 5) omitting it *is*
 *     adaptive thinking; on older models omitting it means no thinking; either way nothing 400s.
 *     Sending an explicit config would have to be gated per model family.
 *   * Because thinking tokens count against `max_tokens`, the old cap of 1024 would be eaten by
 *     reasoning and truncate the visible answer. Callers pass a real budget (several thousand).
 *   * `stop_reason: "refusal"` is checked BEFORE reading content — it arrives as HTTP 200 and
 *     content may be empty, so it would otherwise surface as a confusing "empty response".
 *   * Thinking blocks come back in `content` (with empty text); only `text` blocks are read.
 */
object AnthropicClient {
    private val json = Json { ignoreUnknownKeys = true }

    /** Pure, so the wire shape can be unit-tested without a network. */
    internal fun buildRequestBody(model: String, system: String, userContent: List<ContentBlock>, maxTokens: Int): String =
        buildJsonObject {
            put("model", model)
            put("max_tokens", maxTokens)
            put("system", system)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", JsonArray(userContent))
                })
            })
            if (supportsDefaultFallbacks(model)) put("fallbacks", "default")
        }.toString()

    internal fun usesFallbacks(model: String): Boolean = supportsDefaultFallbacks(model)

    suspend fun send(
        apiKey: String,
        model: String,
        system: String,
        userContent: List<ContentBlock>,
        maxTokens: Int,
    ): AiResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext AiResult.Failure("No API key set — add one in Settings.")

        val body = buildRequestBody(model, system, userContent, maxTokens)

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(ANTHROPIC_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                // Adaptive thinking plus a multi-image prompt can legitimately take a while.
                readTimeout = 180_000
                doOutput = true
                setRequestProperty("content-type", "application/json")
                setRequestProperty("x-api-key", apiKey)
                setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
                if (supportsDefaultFallbacks(model)) setRequestProperty("anthropic-beta", FALLBACK_BETA)
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()

            if (status !in 200..299) {
                val parsed = runCatching { json.decodeFromString(ApiResponse.serializer(), raw) }.getOrNull()
                val msg = parsed?.error?.message
                    ?: when (status) {
                        401 -> "That API key was rejected (HTTP 401)."
                        413 -> "That request was too large — try fewer or smaller photos."
                        429 -> "Rate limited by the API — wait a moment and try again."
                        else -> "Request failed (HTTP $status)."
                    }
                return@withContext AiResult.Failure(msg)
            }

            val parsed = json.decodeFromString(ApiResponse.serializer(), raw)
            if (parsed.stopReason == "refusal") {
                val why = parsed.stopDetails?.explanation?.takeIf { it.isNotBlank() }
                return@withContext AiResult.Failure(
                    "The model declined this request" + (why?.let { ": $it" } ?: ".") +
                        " Try different photos or a narrower question.",
                )
            }
            val text = parsed.content.filter { it.type == "text" }.mapNotNull { it.text }.joinToString("\n").trim()
            if (text.isBlank()) AiResult.Failure("The API returned an empty response.")
            else AiResult.Success(text)
        } catch (e: Exception) {
            AiResult.Failure(e.message?.let { "Network error: $it" } ?: "Network error.")
        } finally {
            conn?.disconnect()
        }
    }
}
