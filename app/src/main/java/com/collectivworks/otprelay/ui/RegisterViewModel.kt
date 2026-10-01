package com.collectivworks.otprelay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.collectivworks.otprelay.data.AuthRepository
import com.collectivworks.otprelay.data.MIN_PASSWORD_LENGTH
import com.collectivworks.otprelay.data.RelayApiException
import com.collectivworks.otprelay.data.authDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why a registration failed, kept as a type so the screen picks the wording. */
sealed interface RegisterError {
    /** Fewer than [MIN_PASSWORD_LENGTH] characters. Caught before any request is made. */
    data object PasswordTooShort : RegisterError
    data object PasswordMismatch : RegisterError
    /** The server's 409. It deliberately does not say whether the email exists. */
    data object CannotCreate : RegisterError
    data object Unreachable : RegisterError
    data class Server(val message: String) : RegisterError
}

data class RegisterUiState(
    val isSubmitting: Boolean = false,
    val error: RegisterError? = null,
    /** True once the account exists and this phone is signed in to it. */
    val isRegistered: Boolean = false
)

/**
 * Drives the registration form. As with [SignInViewModel], the field values stay
 * in the screen so the password is never held anywhere longer-lived than the composition.
 */
class RegisterViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(email: String, password: String, confirmPassword: String) {
        if (_uiState.value.isSubmitting) return

        val localError = when {
            password.length < MIN_PASSWORD_LENGTH -> RegisterError.PasswordTooShort
            password != confirmPassword -> RegisterError.PasswordMismatch
            else -> null
        }
        if (localError != null) {
            _uiState.update { it.copy(error = localError) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            try {
                repository.register(email.trim(), password, deviceName())
                _uiState.update { it.copy(isSubmitting = false, isRegistered = true) }
            } catch (e: RelayApiException) {
                _uiState.update { it.copy(isSubmitting = false, error = e.toRegisterError()) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                RegisterViewModel(AuthRepository(application.authDataStore))
            }
        }
    }
}

private fun RelayApiException.toRegisterError(): RegisterError = when (status) {
    null -> RegisterError.Unreachable
    409 -> RegisterError.CannotCreate
    else -> RegisterError.Server(message.orEmpty())
}
