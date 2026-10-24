package com.salvarez.miboveda.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.usecase.DisableBiometric
import com.salvarez.miboveda.domain.usecase.EnableBiometric
import com.salvarez.miboveda.domain.usecase.ObserveAutoLockTimeout
import com.salvarez.miboveda.domain.usecase.ObserveBiometricStatus
import com.salvarez.miboveda.domain.usecase.PrepareBiometricEnrollment
import com.salvarez.miboveda.domain.usecase.SetAutoLockTimeout
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeBiometricStatus: ObserveBiometricStatus,
    observeAutoLockTimeout: ObserveAutoLockTimeout,
    private val setAutoLockTimeout: SetAutoLockTimeout,
    private val prepareBiometricEnrollment: PrepareBiometricEnrollment,
    private val enableBiometric: EnableBiometric,
    private val disableBiometric: DisableBiometric,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // Kept only between checking the password and the biometric approval, then cleared.
    private var pendingPassword: CharArray? = null

    init {
        observeBiometricStatus()
            .onEach { status -> _state.update { it.copy(biometricStatus = status) } }
            .launchIn(viewModelScope)
        observeAutoLockTimeout()
            .onEach { timeout -> _state.update { it.copy(autoLockTimeout = timeout) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ToggleBiometric -> toggleBiometric(intent.enable)
            is SettingsIntent.SelectAutoLockTimeout -> viewModelScope.launch { setAutoLockTimeout(intent.timeout) }
            is SettingsIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null) }
            SettingsIntent.ConfirmPassword -> confirmPassword()
            SettingsIntent.DismissPasswordDialog -> _state.update { it.copy(isPasswordDialogVisible = false, password = "") }
            is SettingsIntent.BiometricApproved -> completeEnrollment(intent.cipher)
            is SettingsIntent.BiometricFailed -> {
                clearPendingPassword()
                _state.update {
                    it.copy(isBusy = false, error = if (intent.dismissedByUser) null else VaultError.BiometricUnavailable)
                }
            }
        }
    }

    private fun toggleBiometric(enable: Boolean) {
        if (_state.value.isBusy) return
        if (enable) {
            _state.update { it.copy(isPasswordDialogVisible = true, password = "", error = null) }
        } else {
            viewModelScope.launch { disableBiometric() }
        }
    }

    private fun confirmPassword() {
        val password = _state.value.password.toCharArray()
        _state.update { it.copy(isPasswordDialogVisible = false, password = "", isBusy = true, error = null) }
        viewModelScope.launch {
            when (val result = prepareBiometricEnrollment(password)) {
                is Outcome.Success -> {
                    pendingPassword = password
                    _effects.send(SettingsEffect.ShowBiometricPrompt(result.value))
                }
                is Outcome.Failure -> {
                    password.fill('\u0000')
                    _state.update { it.copy(isBusy = false, error = result.error) }
                }
            }
        }
    }

    private fun completeEnrollment(cipher: Cipher) {
        val password = pendingPassword ?: return
        viewModelScope.launch {
            val result = try {
                enableBiometric(cipher, password)
            } finally {
                clearPendingPassword()
            }
            _state.update { it.copy(isBusy = false, error = (result as? Outcome.Failure)?.error) }
        }
    }

    private fun clearPendingPassword() {
        pendingPassword?.fill('\u0000')
        pendingPassword = null
    }

    override fun onCleared() = clearPendingPassword()
}
