package app.otprelay.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.otprelay.R
import app.otprelay.ui.theme.OtpRelayTheme

/** Same cap as the landing checklist, so the form doesn't span a tablet. */
private val FORM_MAX_WIDTH = 380.dp

/**
 * Email and password form. Submitting logs in and registers this phone as a
 * publishing device in one go; [onSignedIn] fires once both have succeeded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    state: SignInUiState,
    onSubmit: (email: String, password: String) -> Unit,
    onInputChange: () -> Unit,
    onSignedIn: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The email survives rotation; the password deliberately does not, since
    // saved instance state is written out to disk by the system.
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val canSubmit = email.isNotBlank() && password.isNotEmpty() && !state.isSubmitting
    val submit = { if (canSubmit) onSubmit(email, password) }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sign_in)) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.widthIn(max = FORM_MAX_WIDTH),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.sign_in_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        onInputChange()
                    },
                    label = { Text(stringResource(R.string.email)) },
                    singleLine = true,
                    enabled = !state.isSubmitting,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.EmailAddress }
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        onInputChange()
                    },
                    label = { Text(stringResource(R.string.password)) },
                    singleLine = true,
                    enabled = !state.isSubmitting,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Password }
                )

                state.error?.let { error ->
                    Text(
                        text = error.message(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = submit,
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(stringResource(R.string.sign_in))
                    }
                }

                TextButton(
                    onClick = onCreateAccountClick,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.no_account_create_one))
                }
            }
        }
    }
}

@Composable
private fun SignInError.message(): String = when (this) {
    SignInError.InvalidCredentials -> stringResource(R.string.sign_in_error_credentials)
    SignInError.Unreachable -> stringResource(R.string.sign_in_error_unreachable)
    is SignInError.Server -> stringResource(R.string.sign_in_error_server, message)
}

@Preview(showBackground = true)
@Composable
private fun SignInScreenPreview() {
    OtpRelayTheme(dynamicColor = false) {
        SignInScreen(
            state = SignInUiState(error = SignInError.InvalidCredentials),
            onSubmit = { _, _ -> },
            onInputChange = {},
            onSignedIn = {},
            onCreateAccountClick = {},
            onBack = {}
        )
    }
}
