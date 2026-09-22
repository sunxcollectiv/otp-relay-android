package com.collectivworks.otprelay.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Credentials live in their own DataStore file, separate from [settingsDataStore],
 * so backup rules can exclude them without also dropping the user's preferences.
 */
val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

/** The signed-in account, as far as the UI needs to know it. */
data class Account(val email: String)

/**
 * Owns sign-in state: the session token from `/auth/login` and the publish
 * token this phone gets from `POST /devices`.
 *
 * The account counts as signed in only once both are stored. Sign-in writes
 * them together, so a half-finished sign-in never leaves the app believing it
 * can relay codes when it cannot.
 */
class AuthRepository(
    private val dataStore: DataStore<Preferences>,
    private val api: RelayApi = RelayApi()
) {

    val account: Flow<Account?> = dataStore.data.map { prefs ->
        if (prefs[PUBLISH_TOKEN] != null) Account(email = prefs[EMAIL].orEmpty()) else null
    }

    /** Bearer token for `POST /otp`. Null while signed out. */
    val publishToken: Flow<String?> = dataStore.data.map { it[PUBLISH_TOKEN] }

    /**
     * Relays a code using whatever publish token is stored at the moment of the
     * call, so it always follows the current sign-in rather than a cached copy.
     */
    suspend fun relayCode(code: String): RelayApi.OtpResult {
        val token = publishToken.first() ?: throw RelayApiException("Not signed in", status = 401)
        return api.postOtp(token, code)
    }

    /** Logs in, registers this phone as a publishing device, and stores both tokens. */
    suspend fun signIn(email: String, password: String, deviceName: String) {
        val session = api.login(email, password)
        val device = try {
            api.createPublishDevice(session.token, deviceName)
        } catch (e: RelayApiException) {
            // Nothing is stored yet, so the session token would otherwise be
            // orphaned on the server until it expires.
            tryQuietly { api.logout(session.token) }
            throw e
        }

        dataStore.edit {
            it[EMAIL] = email
            it[SESSION_TOKEN] = session.token
            it[SESSION_EXPIRES_AT] = session.expiresAt
            it[DEVICE_ID] = device.id
            it[PUBLISH_TOKEN] = device.token
        }
    }

    /**
     * Signs out locally first, then revokes the device and session on the
     * server as a best effort. Being offline should never stop someone signing out.
     */
    suspend fun signOut() {
        val prefs = dataStore.data.first()
        dataStore.edit { it.clear() }

        val session = prefs[SESSION_TOKEN] ?: return
        prefs[DEVICE_ID]?.let { deviceId -> tryQuietly { api.revokeDevice(session, deviceId) } }
        tryQuietly { api.logout(session) }
    }

    private inline fun tryQuietly(block: () -> Unit) {
        try {
            block()
        } catch (_: RelayApiException) {
            // Best-effort cleanup; the server expires sessions on its own.
        }
    }

    private companion object {
        val EMAIL = stringPreferencesKey("email")
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val SESSION_EXPIRES_AT = stringPreferencesKey("session_expires_at")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val PUBLISH_TOKEN = stringPreferencesKey("publish_token")
    }
}
