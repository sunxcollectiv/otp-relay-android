package app.otprelay.relay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import app.otprelay.data.AuthRepository
import app.otprelay.data.RelayApi
import app.otprelay.data.RelayApiException
import app.otprelay.data.authDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "SmsRelayReceiver"

/**
 * Connect and read timeouts for a relay from here. Together they stay under
 * the 10 seconds a receiver gets before Android treats it as hung, which the
 * app-wide default of 10 s each would not.
 */
private const val RECEIVER_TIMEOUT_MILLIS = 4_000

/**
 * Sees every incoming SMS while the relay is on, and forwards the ones that
 * [OtpExtractor] recognises as one-time codes to `POST /otp`. Everything else
 * is dropped here, on the phone.
 *
 * Disabled in the manifest and switched on by [RelaySwitch], so while the
 * relay is off this class is never instantiated. It runs whether or not the
 * app is open, which is why background usage has to be unrestricted: under
 * battery optimization the network is cut off while the phone is idle.
 *
 * Only the code and sender are sent, never the message body. The code is
 * never logged.
 */
class SmsRelayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val codes = extractCodes(intent)
        if (codes.isEmpty()) return

        // Keeps the process alive past onReceive while the request is in flight.
        val pending = goAsync()
        val auth = AuthRepository(
            context.applicationContext.authDataStore,
            RelayApi(timeoutMillis = RECEIVER_TIMEOUT_MILLIS)
        )
        scope.launch {
            try {
                for (otp in codes) {
                    try {
                        val result = auth.relayCode(otp.code, sender = otp.sender)
                        Log.i(TAG, "Relayed a code; reached ${result.delivered} client(s)")
                    } catch (e: RelayApiException) {
                        Log.w(TAG, "Relay failed: ${e.message}")
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private class IncomingOtp(val sender: String?, val code: String)

    /**
     * A long text arrives as several PDUs in one broadcast. They are joined per
     * sender before matching, so a code split across parts is still found.
     */
    private fun extractCodes(intent: Intent): List<IncomingOtp> =
        Telephony.Sms.Intents.getMessagesFromIntent(intent).orEmpty()
            .groupBy { it.displayOriginatingAddress }
            .mapNotNull { (sender, parts) ->
                val body = parts.joinToString("") { it.displayMessageBody.orEmpty() }
                OtpExtractor.extract(body)?.let { IncomingOtp(sender, it) }
            }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
