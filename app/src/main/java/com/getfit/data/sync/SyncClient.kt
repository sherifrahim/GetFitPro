package com.getfit.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** Any failure the user should see, with the message already phrased for a Settings card. */
class SyncException(message: String) : Exception(message)

/**
 * HTTP client for forge-sync (server/forge-sync/main.py). Plain HttpURLConnection, like every other
 * network call in this app. Three endpoints, all tiny. HTTPS is required: Android blocks cleartext
 * by default and this app does not opt out, so an http:// URL is refused up front with a message
 * rather than failing with an opaque "Cleartext HTTP traffic not permitted".
 */
object SyncClient {
    private val json = Json { ignoreUnknownKeys = true }

    /** Unauthenticated liveness check. */
    suspend fun health(baseUrl: String): Result<Unit> = request(baseUrl, "/v1/health", "GET", token = null).map { }

    suspend fun putSnapshot(baseUrl: String, token: String, deviceId: String, body: String): Result<SnapshotPutResponse> =
        request(baseUrl, "/v1/snapshot", "PUT", token, body, extraHeaders = mapOf("X-Device-Id" to deviceId))
            .mapCatching { (text, _) -> json.decodeFromString(SnapshotPutResponse.serializer(), text) }

    suspend fun getLatest(baseUrl: String, token: String): Result<RemoteSnapshot> =
        request(baseUrl, "/v1/snapshot/latest", "GET", token).mapCatching { (text, headers) ->
            RemoteSnapshot(
                body = text,
                version = headers["x-snapshot-version"]?.toLongOrNull() ?: 0,
                createdAt = headers["x-snapshot-created"].orEmpty(),
                sha256 = headers["x-snapshot-sha256"].orEmpty(),
            )
        }

    private suspend fun request(
        baseUrl: String,
        path: String,
        method: String,
        token: String?,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): Result<Pair<String, Map<String, String>>> = withContext(Dispatchers.IO) {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isBlank()) return@withContext Result.failure(SyncException("No sync server set."))
        if (!base.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(SyncException("The server URL must start with https://."))
        }
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 60_000
                if (token != null) setRequestProperty("authorization", "Bearer $token")
                extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("content-type", "application/json")
                }
            }
            if (body != null) OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()
            if (status !in 200..299) {
                val detail = runCatching { json.parseToJsonElement(text).jsonObject["detail"]?.jsonPrimitive?.content }.getOrNull()
                val msg = when (status) {
                    401 -> "The server rejected the token."
                    404 -> if (path.endsWith("/latest")) "Nothing has been synced to this server yet." else "Not a forge-sync server (404)."
                    413 -> "The snapshot is too large for the server."
                    else -> detail ?: "Server error (HTTP $status)."
                }
                return@withContext Result.failure(SyncException(msg))
            }
            // Header names are case-insensitive; normalise so callers can look them up predictably.
            val headers = conn.headerFields.entries
                .filter { it.key != null }
                .associate { it.key.lowercase() to it.value.firstOrNull().orEmpty() }
            Result.success(text to headers)
        } catch (e: SyncException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(SyncException("Couldn't reach the server: ${e.message ?: e.javaClass.simpleName}"))
        } finally {
            conn?.disconnect()
        }
    }
}
