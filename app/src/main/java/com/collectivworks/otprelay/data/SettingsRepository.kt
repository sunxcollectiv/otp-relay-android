package com.collectivworks.otprelay.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The app's single DataStore file. Add new preference keys to [SettingsRepository]. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Reads and writes user preferences. Everything is exposed as a [Flow] so the UI
 * re-renders on its own when a value changes, including from another screen.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * Whether to derive the color scheme from the device wallpaper (Material You)
     * instead of the app's own palette. Defaults to off so the brand colors in
     * `ui/theme/Color.kt` are what actually ships.
     */
    val dynamicColor: Flow<Boolean> = dataStore.data.map { it[DYNAMIC_COLOR] ?: false }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    /**
     * Whether the SMS permission dialog has ever been shown.
     *
     * Android reports "no rationale needed" both for a permission never asked
     * for and for one denied twice, so this flag is the only way to tell them
     * apart after the process has been killed. Without it the first tap on a
     * permanently-denied requirement does nothing at all.
     */
    val smsPermissionRequested: Flow<Boolean> =
        dataStore.data.map { it[SMS_PERMISSION_REQUESTED] ?: false }

    suspend fun setSmsPermissionRequested() {
        dataStore.edit { it[SMS_PERMISSION_REQUESTED] = true }
    }

    private companion object {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SMS_PERMISSION_REQUESTED = booleanPreferencesKey("sms_permission_requested")
    }
}
