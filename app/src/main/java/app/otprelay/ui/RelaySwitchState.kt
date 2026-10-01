package app.otprelay.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.otprelay.relay.RelaySwitch

/** Whether the relay is on, plus the power button's action. */
@Stable
class RelaySwitchState(
    val isOn: Boolean,
    val toggle: () -> Unit
)

/**
 * Tracks [RelaySwitch] for as long as the calling composable is alive.
 *
 * Re-read on resume because sign-out on the settings screen turns the relay
 * off, and coming back from there resumes this destination.
 */
@Composable
fun rememberRelaySwitchState(): RelaySwitchState {
    val context = LocalContext.current
    val relaySwitch = remember(context) { RelaySwitch(context) }

    var isOn by remember { mutableStateOf(relaySwitch.isOn) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isOn = relaySwitch.isOn
    }

    return remember(isOn, relaySwitch) {
        RelaySwitchState(
            isOn = isOn,
            toggle = {
                val next = !relaySwitch.isOn
                relaySwitch.setOn(next)
                isOn = next
            }
        )
    }
}
