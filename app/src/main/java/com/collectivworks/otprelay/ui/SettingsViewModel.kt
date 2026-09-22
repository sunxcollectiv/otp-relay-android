package com.collectivworks.otprelay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.collectivworks.otprelay.data.Account
import com.collectivworks.otprelay.data.AuthRepository
import com.collectivworks.otprelay.data.RelayApiException
import com.collectivworks.otprelay.data.SettingsRepository
import com.collectivworks.otprelay.data.authDataStore
import com.collectivworks.otprelay.data.settingsDataStore
import com.collectivworks.otprelay.relay.RelaySwitch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Code sent by the settings test row. Obviously fake, so nobody mistakes it for a real one. */
const val TEST_OTP_CODE = "123456"

/** Progress of the most recent test send from settings. */
sealed interface TestOtpStatus {
    data object Idle : TestOtpStatus
    data object Sending : TestOtpStatus
    data class Sent(val delivered: Int, val buffered: Boolean) : TestOtpStatus
    data class Failed(val message: String) : TestOtpStatus
}

/**
 * Everything the settings screen renders.
 *
 * [dynamicColor] and [account] are real, persisted state; [account] is null
 * while signed out. The subscription fields are still placeholders — replace
 * their defaults with values from `/me` once the app reads it.
 */
data class SettingsUiState(
    val dynamicColor: Boolean = false,
    val account: Account? = null,
    val testOtp: TestOtpStatus = TestOtpStatus.Idle,
    val subscriptionStatus: String = "Free plan",
    val subscriptionDetail: String = "No active subscription"
)

/**
 * Owned by `MainActivity` rather than by the settings destination, because the
 * theme sits above the NavHost and needs to read [SettingsUiState.dynamicColor] too.
 * The landing checklist reads [SettingsUiState.account] from here for the same reason.
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val relaySwitch: RelaySwitch
) : ViewModel() {

    private val testOtp = MutableStateFlow<TestOtpStatus>(TestOtpStatus.Idle)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.dynamicColor,
        authRepository.account,
        testOtp
    ) { dynamicColor, account, testOtp ->
        SettingsUiState(dynamicColor = dynamicColor, account = account, testOtp = testOtp)
    }.stateIn(
        scope = viewModelScope,
        // Eagerly so the stored value arrives as early as possible; the first
        // frame still uses the default until DataStore's first read lands.
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState()
    )

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    /** Posts [TEST_OTP_CODE] to `/otp` with the stored publish token. Ignored while one is in flight. */
    fun sendTestOtp() {
        if (testOtp.value == TestOtpStatus.Sending) return
        testOtp.value = TestOtpStatus.Sending
        viewModelScope.launch {
            testOtp.value = try {
                val result = authRepository.relayCode(TEST_OTP_CODE)
                TestOtpStatus.Sent(result.delivered, result.buffered)
            } catch (e: RelayApiException) {
                TestOtpStatus.Failed(e.message.orEmpty())
            }
        }
    }

    fun signOut() {
        // A result from the previous account would be misleading after sign-out.
        testOtp.value = TestOtpStatus.Idle
        // With no publish token every relay would fail anyway; turning off also
        // means signing into a different account never starts relaying unasked.
        relaySwitch.setOn(false)
        viewModelScope.launch { authRepository.signOut() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                SettingsViewModel(
                    SettingsRepository(application.settingsDataStore),
                    AuthRepository(application.authDataStore),
                    RelaySwitch(application)
                )
            }
        }
    }
}
