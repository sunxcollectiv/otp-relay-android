package app.otprelay.ui

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.otprelay.data.AuthRepository
import app.otprelay.data.RelayApiException
import app.otprelay.data.authDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why a sign-in failed, kept as a type so the screen picks the wording. */
sealed interface SignInError {
    data object InvalidCredentials : SignInError
    data object Unreachable : SignInError
    data class Server(val message: String) : SignInError
}

data class SignInUiState(
    val isSubmitting: Boolean = false,
    val error: SignInError? = null,
    val isSignedIn: Boolean = false
)

/**
 * Drives the sign-in form. The field values live in the screen, not here, so
 * the password is never held anywhere longer-lived than the composition.
 */
class SignInViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }

        viewModelScope.launch {
            try {
                repository.signIn(email.trim(), password, deviceName())
                _uiState.update { it.copy(isSubmitting = false, isSignedIn = true) }
            } catch (e: RelayApiException) {
                _uiState.update { it.copy(isSubmitting = false, error = e.toSignInError()) }
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
                SignInViewModel(AuthRepository(application.authDataStore))
            }
        }
    }
}

private fun RelayApiException.toSignInError(): SignInError = when (status) {
    null -> SignInError.Unreachable
    401 -> SignInError.InvalidCredentials
    else -> SignInError.Server(message.orEmpty())
}

/** What the device shows up as in the account's device list. The API caps names at 64. */
internal fun deviceName(): String {
    val model = if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) {
        Build.MODEL
    } else {
        "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    return model.take(64)
}
