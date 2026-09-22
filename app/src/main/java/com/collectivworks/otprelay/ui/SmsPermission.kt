package com.collectivworks.otprelay.ui

import android.Manifest
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collectivworks.otprelay.data.SettingsRepository
import com.collectivworks.otprelay.data.settingsDataStore
import kotlinx.coroutines.launch

/**
 * The one SMS permission the relay needs. `RECEIVE_SMS` delivers the broadcast
 * for an arriving message; `READ_SMS` would only add the existing inbox, which
 * is a much broader grant for no benefit here.
 */
const val SMS_PERMISSION: String = Manifest.permission.RECEIVE_SMS

/**
 * Whether the app holds [SMS_PERMISSION], plus the one action that can change
 * it in either direction. [manage] works out the right route on its own — ask,
 * or hand off to system settings — so callers can treat it as a single button
 * whether the permission is currently held or not.
 */
@Stable
class SmsPermissionState(
    val isGranted: Boolean,
    val manage: () -> Unit
)

/**
 * Tracks [SMS_PERMISSION] for as long as the calling composable is alive.
 *
 * The value is re-read on every resume because the system settings screen is
 * outside the app's lifecycle: a user can revoke the grant there and come back,
 * and nothing else would tell us.
 */
@Composable
fun rememberSmsPermissionState(): SmsPermissionState {
    val context = LocalContext.current
    val activity = LocalActivity.current

    var isGranted by remember { mutableStateOf(context.hasSmsPermission()) }

    // Persisted rather than remembered: it has to survive process death, because
    // after a cold start Android cannot tell us whether the dialog was ever shown.
    // Defaults to false while DataStore loads, so a slow first read costs at most
    // one no-op request rather than an unexpected jump into system settings.
    val repository = remember(context) {
        SettingsRepository(context.applicationContext.settingsDataStore)
    }
    val hasAsked by repository.smsPermissionRequested.collectAsStateWithLifecycle(
        initialValue = false
    )
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
        scope.launch { repository.setSmsPermissionRequested() }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isGranted = context.hasSmsPermission()
    }

    return remember(isGranted, hasAsked, activity, context) {
        SmsPermissionState(
            isGranted = isGranted,
            manage = {
                // Android stops showing the dialog after the user denies twice, and
                // signals that by reporting no rationale *after* a request. Launching
                // anyway would be a tap that visibly does nothing, so send them to the
                // settings page instead — the only place the grant can still be given.
                val dialogStillPossible = activity == null || !hasAsked ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, SMS_PERMISSION)

                when {
                    // Taking a permission back is settings-only. `revokeSelfPermissionOnKill`
                    // exists from API 33, but it is both above this app's minSdk and fatal to
                    // the process — a checklist row is no place to kill the app from.
                    isGranted -> context.openPermissionSettings()
                    dialogStillPossible -> launcher.launch(SMS_PERMISSION)
                    else -> context.openPermissionSettings()
                }
            }
        )
    }
}

private fun Context.hasSmsPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, SMS_PERMISSION) == PackageManager.PERMISSION_GRANTED

/**
 * Opens the App info page, which is as close to the SMS toggle as an ordinary
 * app can get: the user still has to tap through Permissions and then SMS.
 *
 * There is no supported deep link to the per-app permissions list. Both
 * `MANAGE_APP_PERMISSIONS` and `MANAGE_APP_PERMISSION` resolve to
 * PermissionController activities declared with `GRANT_RUNTIME_PERMISSIONS`,
 * a signature-level permission, so launching either from here is refused by
 * ActivityTaskManager and only adds a Permission Denial line to the log.
 *
 * The catch is for OEM skins that ship no handler for the App info screen at
 * all — rare, but an uncaught ActivityNotFoundException would crash the app.
 */
private fun Context.openPermissionSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Nothing further to offer: the grant simply cannot be changed from here.
    }
}
