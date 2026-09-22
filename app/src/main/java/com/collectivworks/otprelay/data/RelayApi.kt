package com.collectivworks.otprelay.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Where the relay server lives. `localhost` on a device or emulator is the
 * device itself, so during development forward the port with
 * `adb reverse tcp:4820 tcp:4820` to reach the server on the dev machine.
 */
const val RELAY_BASE_URL = "http://localhost:4820"

private const val TIMEOUT_MILLIS = 10_000

/**
 * A failed call to the relay. [status] is the HTTP status when the server
 * answered, or null when it could not be reached at all.
 */
class RelayApiException(
    message: String,
    val status: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Thin client for the relay's HTTP API, built on [HttpURLConnection] and
 * `org.json` so the handful of calls here need no networking dependency.
 * Every call throws [RelayApiException] and nothing else on failure.
 */
class RelayApi(
    private val baseUrl: String = RELAY_BASE_URL,
    private val timeoutMillis: Int = TIMEOUT_MILLIS
) {

    /** What `POST /auth/login` returns: a bearer token for the management endpoints. */
    data class Session(val token: String, val expiresAt: String)

    /** What `POST /devices` returns. The server shows [token] exactly once. */
    data class Device(val id: String, val token: String)

    suspend fun login(email: String, password: String): Session {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
        val json = request("POST", "/auth/login", body).requireBody()
        return Session(
            token = json.getStringOrThrow("token"),
            expiresAt = json.optString("expires_at")
        )
    }

    /** Registers this phone as a publisher, which is what lets it post codes to `/otp`. */
    suspend fun createPublishDevice(sessionToken: String, name: String): Device {
        val body = JSONObject()
            .put("name", name)
            .put("kind", "android")
            .put("capability", "publish")
        val json = request("POST", "/devices", body, bearer = sessionToken).requireBody()
        return Device(
            id = json.getStringOrThrow("id"),
            token = json.getStringOrThrow("token")
        )
    }

    /**
     * What `POST /otp` reports: how many connected clients got the code right
     * now, and whether it is held for a client that connects shortly after.
     */
    data class OtpResult(val delivered: Int, val buffered: Boolean)

    /**
     * Relays [code] to the account's clients. Needs a publish device token, not a
     * session token. [sender] is shown alongside the code so the user can tell
     * which service it is for.
     */
    suspend fun postOtp(publishToken: String, code: String, sender: String? = null): OtpResult {
        val body = JSONObject().put("code", code)
        sender?.let { body.put("sender", it) }
        val json = request("POST", "/otp", body, bearer = publishToken).requireBody()
        return OtpResult(
            delivered = json.optInt("delivered"),
            buffered = json.optBoolean("buffered")
        )
    }

    suspend fun revokeDevice(sessionToken: String, deviceId: String) {
        request("DELETE", "/devices/$deviceId", bearer = sessionToken)
    }

    suspend fun logout(sessionToken: String) {
        request("POST", "/auth/logout", bearer = sessionToken)
    }

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        bearer: String? = null
    ): JSONObject? = withContext(Dispatchers.IO) {
        val connection = try {
            URL(baseUrl + path).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw RelayApiException("Could not open a connection to the relay", cause = e)
        }
        try {
            connection.requestMethod = method
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            bearer?.let { connection.setRequestProperty("Authorization", "Bearer $it") }

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                throw RelayApiException(errorDetail(text) ?: "HTTP $status", status)
            }
            if (text.isBlank()) null else JSONObject(text)
        } catch (e: IOException) {
            throw RelayApiException("Could not reach the relay", cause = e)
        } catch (e: JSONException) {
            throw RelayApiException("The relay sent a response that could not be read", cause = e)
        } finally {
            connection.disconnect()
        }
    }
}

private fun JSONObject?.requireBody(): JSONObject =
    this ?: throw RelayApiException("The relay sent an empty response")

private fun JSONObject.getStringOrThrow(key: String): String =
    optString(key).ifEmpty { throw RelayApiException("The relay response had no \"$key\"") }

/**
 * Pulls a readable message out of a FastAPI error body. `detail` is a plain
 * string for errors the app raises itself (`{"detail": "bad credentials"}`)
 * and a list of field errors for request validation failures (422).
 */
private fun errorDetail(text: String): String? = try {
    when (val detail = JSONObject(text).opt("detail")) {
        is String -> detail
        is JSONArray -> detail.optJSONObject(0)?.optString("msg")?.ifEmpty { null }
        else -> null
    }
} catch (_: JSONException) {
    null
}
