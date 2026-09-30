package com.collectivworks.otprelay.ui

import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.collectivworks.otprelay.BuildConfig
import com.collectivworks.otprelay.R
import com.collectivworks.otprelay.data.Account
import com.collectivworks.otprelay.ui.theme.OtpRelayTheme

/**
 * Where the privacy policy is published. Play requires it to be reachable from
 * inside the app as well as from the store listing, and the two must agree.
 */
const val PRIVACY_POLICY_URL = "https://otp-relay.app/privacy"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    state: SettingsUiState = SettingsUiState(),
    onAccountClick: () -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    onSendTestOtp: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            val account = state.account
            ListItem(
                headlineContent = { Text(stringResource(R.string.account_status)) },
                supportingContent = {
                    Text(
                        if (account != null) {
                            stringResource(R.string.account_signed_in_as, account.email)
                        } else {
                            stringResource(R.string.account_signed_out)
                        }
                    )
                },
                modifier = Modifier.clickable(onClick = onAccountClick)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.subscription_status)) },
                supportingContent = { Text("${state.subscriptionStatus} · ${state.subscriptionDetail}") },
                modifier = Modifier.clickable(onClick = onSubscriptionClick)
            )
            HorizontalDivider()

            // Material You only exists on Android 12+; below that the toggle would
            // be a control that visibly does nothing, so it isn't offered at all.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.dynamic_color)) },
                    supportingContent = { Text(stringResource(R.string.dynamic_color_description)) },
                    trailingContent = {
                        Switch(
                            checked = state.dynamicColor,
                            onCheckedChange = onDynamicColorChange
                        )
                    },
                    // The whole row toggles, so the Switch itself does not need to be a
                    // separate touch target for accessibility.
                    modifier = Modifier.clickable(role = Role.Switch) {
                        onDynamicColorChange(!state.dynamicColor)
                    }
                )
                HorizontalDivider()
            }

            // Needs the publish token, which only exists while signed in.
            if (account != null) {
                val sending = state.testOtp == TestOtpStatus.Sending
                ListItem(
                    headlineContent = { Text(stringResource(R.string.send_test_code)) },
                    supportingContent = { Text(state.testOtp.description()) },
                    trailingContent = {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    modifier = Modifier.clickable(enabled = !sending, onClick = onSendTestOtp)
                )
                HorizontalDivider()
            }

            // Opens in the browser rather than a WebView so the page can change
            // without an app release. openUri throws when nothing on the device
            // handles https, which is rare enough to just swallow.
            val uriHandler = LocalUriHandler.current
            ListItem(
                headlineContent = { Text(stringResource(R.string.privacy_policy)) },
                supportingContent = { Text(stringResource(R.string.privacy_policy_description)) },
                modifier = Modifier.clickable {
                    runCatching { uriHandler.openUri(PRIVACY_POLICY_URL) }
                }
            )
            HorizontalDivider()

            // Nothing to log out of while signed out.
            if (account != null) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.log_out),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    colors = ListItemDefaults.colors(leadingIconColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable(onClick = onLogoutClick)
                )
            }

            // Which build this is. The commit hash and install time are what tell
            // two dev builds apart, since the version name rarely changes.
            ListItem(
                headlineContent = { Text(stringResource(R.string.version)) },
                supportingContent = {
                    Text(
                        stringResource(
                            R.string.version_detail,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                            BuildConfig.GIT_COMMIT,
                            rememberInstallTime()
                        )
                    )
                }
            )
        }
    }
}

/**
 * When this APK was last installed. Falls back to "unknown" in
 * previews, where there is no real package to look up.
 */
@Composable
private fun rememberInstallTime(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            DateFormat.format("yyyy-MM-dd HH:mm", info.lastUpdateTime).toString()
        }.getOrDefault("unknown")
    }
}

@Composable
private fun TestOtpStatus.description(): String = when (this) {
    TestOtpStatus.Idle -> stringResource(R.string.send_test_code_description, TEST_OTP_CODE)
    TestOtpStatus.Sending -> stringResource(R.string.send_test_code_sending)
    is TestOtpStatus.Sent -> if (buffered) {
        stringResource(R.string.send_test_code_sent_buffered, delivered)
    } else {
        stringResource(R.string.send_test_code_sent, delivered)
    }
    is TestOtpStatus.Failed -> stringResource(R.string.send_test_code_failed, message)
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    OtpRelayTheme(dynamicColor = false) {
        SettingsScreen(
            onBack = {},
            state = SettingsUiState(account = Account("someone@example.com"))
        )
    }
}
