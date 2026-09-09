package com.getfit.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val ANTHROPIC_ENDPOINT = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION = "2023-06-01"

private const val SYSTEM_PROMPT =
    "You are a knowledgeable, direct strength-training coach reviewing a lifter's logged " +
        "workout history from their Forge app. Give a short, practical review: what's trending " +
        "well, what looks stalled or inconsistent, and 2-4 concrete, specific suggestions for the " +
        "next few weeks (progression, volume, recovery, exercise selection). Be honest and " +
        "specific, referencing the actual numbers given rather than generic advice. No filler, no " +
        "disclaimers about not being a doctor. Plain text, a few short paragraphs, no markdown."

@Serializable
private data class AnthropicMessage(val role: String, val content: String)

@Serializable
private data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<AnthropicMessage>,
)

@Serializable
private data class AnthropicContentBlock(val type: String = "", val text: String? = null)

@Serializable
private data class AnthropicError(val type: String? = null, val message: String? = null)

@Serializable
private data class AnthropicResponse(
    val content: List<AnthropicContentBlock> = emptyList(),
    val error: AnthropicError? = null,
)

sealed class AiReviewResult {
    data class Success(val text: String) : AiReviewResult()
    data class Failure(val message: String) : AiReviewResult()
}

/**
 * Sends a workout summary to Anthropic's Messages API using the user's own key and returns the
 * written review. Deliberately dependency-free (plain HttpURLConnection, no OkHttp/Retrofit) —
 * this project has no networking library yet and this is one endpoint, so pulling in a whole HTTP
 * stack for it isn't worth the added Gradle dependency risk. If the app grows more network
 * features later (sync, import from other services), that's the point to introduce a real client.
 */
object AiReviewClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun review(apiKey: String, model: String, workoutSummary: String): AiReviewResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext AiReviewResult.Failure("No API key set — add one in Settings.")

            val requestBody = json.encodeToString(
                AnthropicRequest.serializer(),
                AnthropicRequest(
                    model = model,
                    maxTokens = 1024,
                    system = SYSTEM_PROMPT,
                    messages = listOf(AnthropicMessage(role = "user", content = workoutSummary)),
                ),
            )

            var conn: HttpURLConnection? = null
            try {
                conn = (URL(ANTHROPIC_ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 45_000
                    doOutput = true
                    setRequestProperty("content-type", "application/json")
                    setRequestProperty("x-api-key", apiKey)
                    setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(requestBody) }

                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()

                if (status !in 200..299) {
                    val parsedErr = runCatching { json.decodeFromString(AnthropicResponse.serializer(), raw) }.getOrNull()
                    val msg = parsedErr?.error?.message
                        ?: if (status == 401) "That API key was rejected (HTTP 401)."
                        else "Request failed (HTTP $status)."
                    return@withContext AiReviewResult.Failure(msg)
                }

                val parsed = json.decodeFromString(AnthropicResponse.serializer(), raw)
                val text = parsed.content.firstOrNull { it.type == "text" }?.text
                if (text.isNullOrBlank()) AiReviewResult.Failure("The API returned an empty response.")
                else AiReviewResult.Success(text.trim())
            } catch (e: Exception) {
                AiReviewResult.Failure(e.message?.let { "Network error: $it" } ?: "Network error.")
            } finally {
                conn?.disconnect()
            }
        }
}
