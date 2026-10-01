package com.collectivworks.otprelay.ui

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.collectivworks.otprelay.R
import com.collectivworks.otprelay.data.MIN_PASSWORD_LENGTH
import com.collectivworks.otprelay.ui.theme.OtpRelayTheme

/** Same cap as the sign-in form, so the two screens line up. */
private val FORM_MAX_WIDTH = 380.dp

/**
 * Email, password and confirmation. Submitting creates the account and signs
 * this phone in; [onRegistered] fires once both have succeeded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onSubmit: (email: String, password: String, confirmPassword: String) -> Unit,
    onInputChange: () -> Unit,
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // As on the sign-in screen: the email survives rotation, the passwords do not.
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val canSubmit = email.isNotBlank() &&
        password.isNotEmpty() &&
        confirmPassword.isNotEmpty() &&
        !state.isSubmitting
    val submit = { if (canSubmit) onSubmit(email, password, confirmPassword) }

    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) onRegistered()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_account)) },
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
                    text = stringResource(R.string.create_account_description),
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
                    supportingText = {
                        Text(
                            pluralStringResource(
                                R.plurals.password_min_length,
                                MIN_PASSWORD_LENGTH,
                                MIN_PASSWORD_LENGTH
                            )
                        )
                    },
                    isError = state.error == RegisterError.PasswordTooShort,
                    singleLine = true,
                    enabled = !state.isSubmitting,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.NewPassword }
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        onInputChange()
                    },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    isError = state.error == RegisterError.PasswordMismatch,
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
                        .semantics { contentType = ContentType.NewPassword }
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
                        Text(stringResource(R.string.create_account))
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterError.message(): String = when (this) {
    RegisterError.PasswordTooShort -> pluralStringResource(
        R.plurals.register_error_password_short,
        MIN_PASSWORD_LENGTH,
        MIN_PASSWORD_LENGTH
    )
    RegisterError.PasswordMismatch -> stringResource(R.string.register_error_password_mismatch)
    RegisterError.CannotCreate -> stringResource(R.string.register_error_cannot_create)
    RegisterError.Unreachable -> stringResource(R.string.sign_in_error_unreachable)
    is RegisterError.Server -> stringResource(R.string.register_error_server, message)
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    OtpRelayTheme(dynamicColor = false) {
        RegisterScreen(
            state = RegisterUiState(error = RegisterError.PasswordMismatch),
            onSubmit = { _, _, _ -> },
            onInputChange = {},
            onRegistered = {},
            onBack = {}
        )
    }
}
