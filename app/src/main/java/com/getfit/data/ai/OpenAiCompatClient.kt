package com.getfit.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * A preset for the OpenAI-compatible provider: a base URL and a vision-capable default model.
 * Model IDs were checked against each provider's live docs on 2026-09-11 — they go stale, which is
 * why the model is a free-text field in Settings and this is only a starting value.
 */
enum class CompatPreset(val label: String, val baseUrl: String, val defaultModel: String, val hint: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-5.6-luna", "platform.openai.com — pay as you go, separate from ChatGPT Plus"),
    GROQ("Groq", "https://api.groq.com/openai/v1", "qwen/qwen3.6-27b", "console.groq.com — free tier (no card); body check fits ~2 photos per request"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", "deepseek-flash", "platform.deepseek.com — very cheap; supports images"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", "", "openrouter.ai — one key, many models; pick a vision-capable one"),
    OLLAMA("Self-hosted", "https://your-server/v1", "", "Ollama or similar behind HTTPS (plain http:// is blocked on Android)"),
    CUSTOM("Custom", "", "", "Any OpenAI-compatible /chat/completions endpoint"),
}

/**
 * Talks to any OpenAI-compatible Chat Completions endpoint. One client covers OpenAI itself, Groq,
 * DeepSeek, OpenRouter, Together, DashScope's compatible mode, and a self-hosted Ollama — they all
 * speak this shape. Same plain-HttpURLConnection stance as [AnthropicClient].
 *
 * Takes the same [ContentBlock]s the Anthropic client takes (built by [textBlock]/[imageBlock]) and
 * translates them, so callers don't know or care which provider is configured.
 */
object OpenAiCompatClient {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Pure, so the translation can be unit-tested. Anthropic image blocks become OpenAI `image_url`
     * blocks with a data: URL; text blocks pass through. `max_tokens` is sent as
     * `max_completion_tokens` AND `max_tokens`: newer OpenAI models reject the old name, older
     * compatible servers only know the old name, and every implementation ignores the one it
     * doesn't recognise.
     */
    internal fun buildRequestBody(model: String, system: String, userContent: List<ContentBlock>, maxTokens: Int, baseUrl: String = ""): String =
        buildJsonObject {
            put("model", model)
            put("max_completion_tokens", maxTokens)
            put("max_tokens", maxTokens)
            // Groq's Qwen models think inline (<think>…</think>) before answering. On the free tier
            // that reasoning competes with the answer for an 8K tokens/minute budget — and with two
            // images already costing 4,096 — so switch it off there. Verified: with it, a 20-token
            // reply was 100% scratchpad; with "none", completion_tokens fell from ~130 to 2. The
            // parameter is Groq-specific, hence gated on the host rather than sent everywhere.
            if (baseUrl.contains("groq.com", ignoreCase = true)) put("reasoning_effort", "none")
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", "system"); put("content", system) })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", JsonArray(userContent.map(::translateBlock)))
                })
            })
        }.toString()

    private fun translateBlock(b: ContentBlock): JsonObject {
        val type = b["type"]?.jsonPrimitive?.content
        return if (type == "image") {
            val src = b["source"]!!.jsonObject
            val mediaType = src["media_type"]!!.jsonPrimitive.content
            val data = src["data"]!!.jsonPrimitive.content
            buildJsonObject {
                put("type", "image_url")
                putJsonObject("image_url") { put("url", "data:$mediaType;base64,$data") }
            }
        } else b // text blocks share the {type:"text", text} shape
    }

    /** `choices[0].message.content` is a string on most servers, but some return a content-part array. */
    internal fun extractText(response: JsonObject): String? {
        val msg = response["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject ?: return null
        return when (val c: JsonElement? = msg["content"]) {
            is JsonPrimitive -> c.content
            is JsonArray -> c.mapNotNull { part ->
                val o = part.jsonObject
                if (o["type"]?.jsonPrimitive?.content == "text") o["text"]?.jsonPrimitive?.content else null
            }.joinToString("\n")
            else -> null
        }?.let(::stripThinking)?.trim()
    }

    /**
     * Open reasoning models (Qwen 3, DeepSeek-R1 lineage, various OpenRouter routes) may emit their
     * chain of thought inline as <think>…</think> before the answer. That is never something to
     * show the user, so it is removed wherever it appears — including an unterminated block, which
     * is what a max_tokens cut-off mid-thought looks like.
     */
    internal fun stripThinking(text: String): String =
        text.replace(Regex("(?s)<think>.*?</think>"), "")
            .replace(Regex("(?s)<think>.*$"), "")

    suspend fun send(
        baseUrl: String,
        apiKey: String,
        model: String,
        system: String,
        userContent: List<ContentBlock>,
        maxTokens: Int,
    ): AiResult = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext AiResult.Failure("No endpoint set — pick a provider in Settings.")
        if (model.isBlank()) return@withContext AiResult.Failure("No model set — enter one in Settings.")
        if (!baseUrl.startsWith("https://", ignoreCase = true)) {
            // Android blocks cleartext by default and this app doesn't opt out. Say so instead of
            // letting the connection fail with an opaque "Cleartext HTTP traffic not permitted".
            return@withContext AiResult.Failure("The endpoint must use https:// (plain http is blocked on Android).")
        }

        val body = buildRequestBody(model, system, userContent, maxTokens, baseUrl)
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(baseUrl.trimEnd('/') + "/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 180_000
                doOutput = true
                setRequestProperty("content-type", "application/json")
                // Some providers reject a blank bearer; a self-hosted Ollama doesn't need one at all.
                if (apiKey.isNotBlank()) setRequestProperty("authorization", "Bearer $apiKey")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()

            val parsed = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
            if (status !in 200..299) {
                val msg = parsed?.get("error")?.let { e ->
                    (e as? JsonObject)?.get("message")?.jsonPrimitive?.content ?: (e as? JsonPrimitive)?.content
                } ?: when (status) {
                    401 -> "That API key was rejected (HTTP 401)."
                    404 -> "Endpoint not found (HTTP 404) — check the base URL ends in the provider's /v1."
                    413 -> "That request was too large — try fewer or smaller photos."
                    429 -> "Rate limited by the provider — wait a moment and try again."
                    else -> "Request failed (HTTP $status)."
                }
                return@withContext AiResult.Failure(msg)
            }
            val text = parsed?.let(::extractText)
            if (text.isNullOrBlank()) AiResult.Failure("The provider returned an empty response.")
            else AiResult.Success(text)
        } catch (e: Exception) {
            AiResult.Failure(e.message?.let { "Network error: $it" } ?: "Network error.")
        } finally {
            conn?.disconnect()
        }
    }
}
