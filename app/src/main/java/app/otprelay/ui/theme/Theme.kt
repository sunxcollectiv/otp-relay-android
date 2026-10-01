package app.otprelay.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = Background,
    onBackground = OnBackground,
    surface = Background,
    onSurface = OnBackground,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = BackgroundDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

@Composable
fun OtpRelayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You wallpaper colors, available from Android 12. Off by default so
    // the palette in Color.kt is what actually renders; the user can opt in from
    // the settings screen, which is why this is a parameter and not a constant.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/** Container and content colors for the power button's on and off states. */
class RelayColors(
    val activeContainer: Color,
    val activeContent: Color,
    val inactiveContainer: Color,
    val inactiveContent: Color
)

@Composable
fun relayColors(darkTheme: Boolean = isSystemInDarkTheme()): RelayColors =
    if (darkTheme) {
        RelayColors(
            activeContainer = RelayActiveContainerDark,
            activeContent = RelayActiveContentDark,
            inactiveContainer = RelayInactiveContainerDark,
            inactiveContent = RelayInactiveContentDark
        )
    } else {
        RelayColors(
            activeContainer = RelayActiveContainer,
            activeContent = RelayActiveContent,
            inactiveContainer = RelayInactiveContainer,
            inactiveContent = RelayInactiveContent
        )
    }
