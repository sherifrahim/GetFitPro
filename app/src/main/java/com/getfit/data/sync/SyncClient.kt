package com.getfit.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class SyncClientResult {
    data class Success(val accepted: Int) : SyncClientResult()
    data class Failure(val message: String) : SyncClientResult()
}

/**
 * Talks to the (future) Oracle-hosted sync endpoint. Plain HttpURLConnection, matching
 * AiReviewClient — no networking library added for one more endpoint given this project's
 * zero-new-dependency approach this session. The endpoint shape (`POST {baseUrl}/sync/push` with a
 * `{device_id, ops}` JSON body) is a placeholder guess, not a confirmed contract — once the real
 * Oracle server details are shared, this is the one file that needs to change to match it.
 */
object SyncClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun push(baseUrl: String, deviceId: String, ops: List<SyncOp>): SyncClientResult =
        withContext(Dispatchers.IO) {
            if (baseUrl.isBlank()) return@withContext SyncClientResult.Failure("No sync server configured.")
            if (ops.isEmpty()) return@withContext SyncClientResult.Success(0)

            val body = json.encodeToString(SyncPushRequest(deviceId, ops))
            var conn: HttpURLConnection? = null
            try {
                val url = baseUrl.trimEnd('/') + "/sync/push"
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    doOutput = true
                    setRequestProperty("content-type", "application/json")
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()

                if (status !in 200..299) {
                    val parsedErr = runCatching { json.decodeFromString(SyncPushResponse.serializer(), raw) }.getOrNull()
                    return@withContext SyncClientResult.Failure(parsedErr?.error ?: "Sync failed (HTTP $status).")
                }
                val parsed = runCatching { json.decodeFromString(SyncPushResponse.serializer(), raw) }.getOrNull()
                SyncClientResult.Success(parsed?.accepted ?: ops.size)
            } catch (e: Exception) {
                SyncClientResult.Failure(e.message?.let { "Couldn't reach the sync server: $it" } ?: "Couldn't reach the sync server.")
            } finally {
                conn?.disconnect()
            }
        }
}
