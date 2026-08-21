package com.collectivworks.otprelay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collectivworks.otprelay.ui.OtpRelayApp
import com.collectivworks.otprelay.ui.SettingsViewModel
import com.collectivworks.otprelay.ui.theme.OtpRelayTheme

/**
 * The app's only activity. Everything else is Compose, so new screens get added
 * as destinations in [OtpRelayApp] rather than as new activities.
 */
class MainActivity : ComponentActivity() {

    // Held here, not in the settings destination, because the theme wraps the
    // whole NavHost and needs to read the same state the settings screen edits.
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
            OtpRelayTheme(dynamicColor = settings.dynamicColor) {
                OtpRelayApp(
                    settings = settings,
                    onDynamicColorChange = settingsViewModel::setDynamicColor
                )
            }
        }
    }
}
