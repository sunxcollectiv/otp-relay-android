package com.collectivworks.otprelay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.collectivworks.otprelay.ui.LandingScreen
import com.collectivworks.otprelay.ui.theme.OtpRelayTheme

/**
 * The app's only activity. Everything else is Compose, so new screens get added
 * inside [LandingScreen] or behind a NavHost here rather than as new activities.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OtpRelayTheme {
                LandingScreen()
            }
        }
    }
}
