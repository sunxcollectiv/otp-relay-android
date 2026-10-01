package app.otprelay.relay

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * The power button's backing state: whether [SmsRelayReceiver] is enabled.
 *
 * Toggling the component itself, rather than a stored flag the receiver
 * checks, means that while the relay is off Android does not wake the app for
 * incoming texts at all. It also leaves a single source of truth: the enabled
 * state survives restarts and updates, and resets on uninstall or clear data.
 */
class RelaySwitch(context: Context) {

    private val packageManager = context.packageManager
    private val component = ComponentName(context.applicationContext, SmsRelayReceiver::class.java)

    /** DEFAULT means "whatever the manifest says", which is disabled. */
    val isOn: Boolean
        get() = packageManager.getComponentEnabledSetting(component) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun setOn(on: Boolean) {
        packageManager.setComponentEnabledSetting(
            component,
            if (on) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            // Without this, changing a component's state kills the app's process.
            PackageManager.DONT_KILL_APP
        )
    }
}
