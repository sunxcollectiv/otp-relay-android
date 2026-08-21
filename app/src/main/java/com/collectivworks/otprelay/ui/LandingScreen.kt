package com.collectivworks.otprelay.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.collectivworks.otprelay.R
import com.collectivworks.otprelay.ui.theme.OtpRelayTheme

/**
 * The main app surface. Currently just the wordmark; the Scaffold is here so
 * top/bottom bars and content can be dropped in without restructuring.
 */
@Composable
fun LandingScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Wordmark()
        }
    }
}

/** Text stand-in for the logo. Swap for a vector drawable when there is one. */
@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.app_name),
        modifier = modifier,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary
    )
}

@Preview(showBackground = true)
@Composable
private fun LandingScreenPreview() {
    OtpRelayTheme {
        LandingScreen()
    }
}
