package app.otprelay.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Whether the app is exempt from battery optimization, which is what the
 * system's "Unrestricted" battery setting means under the hood. [manage] moves
 * it in whichever direction is possible from the current state, matching
 * [SmsPermissionState].
 */
@Stable
class BatteryOptimizationState(
    val isUnrestricted: Boolean,
    val manage: () -> Unit
)

/**
 * Tracks the battery exemption for as long as the calling composable is alive.
 *
 * Re-read on every resume for the same reason as the SMS grant: both the
 * request dialog and the settings screen live outside the app, and neither
 * reports back.
 */
@Composable
fun rememberBatteryOptimizationState(): BatteryOptimizationState {
    val context = LocalContext.current

    var isUnrestricted by remember { mutableStateOf(context.isIgnoringBatteryOptimizations()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isUnrestricted = context.isIgnoringBatteryOptimizations()
    }

    return remember(isUnrestricted, context) {
        BatteryOptimizationState(
            isUnrestricted = isUnrestricted,
            manage = {
                // The request dialog can only grant the exemption, never remove it, so
                // going back to Optimized has to happen on the App info page.
                if (isUnrestricted) {
                    context.openAppInfoSettings()
                } else {
                    context.requestBatteryExemption()
                }
            }
        )
    }
}

private fun Context.isIgnoringBatteryOptimizations(): Boolean =
    getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

/**
 * Shows the system's one-tap "Let app always run in background?" dialog.
 *
 * Falls back to the full optimization list if the dialog has no handler, which
 * some OEM skins strip out. That list needs no permission but makes the user
 * find the app themselves, so it is the second choice rather than the first.
 */
// The lint check is Google Play policy, not a platform restriction: the relay's
// whole job is to forward codes while the phone sits idle, which is one of the
// use cases the policy allows. Expect to justify it at review.
@SuppressLint("BatteryLife")
private fun Context.requestBatteryExemption() {
    val request = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.fromParts("package", packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        startActivity(request)
    } catch (_: ActivityNotFoundException) {
        try {
            startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            // Nothing further to offer on a skin with neither screen.
        }
    }
}
