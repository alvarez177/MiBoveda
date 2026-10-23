package com.salvarez.miboveda.presentation.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.usecase.ObserveBiometricStatus
import com.salvarez.miboveda.domain.usecase.PrepareBiometricUnlock
import com.salvarez.miboveda.domain.usecase.UnlockVault
import com.salvarez.miboveda.domain.usecase.UnlockWithBiometric
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
class UnlockViewModel @Inject constructor(
    private val unlockVault: UnlockVault,
    observeBiometricStatus: ObserveBiometricStatus,
    private val prepareBiometricUnlock: PrepareBiometricUnlock,
    private val unlockWithBiometric: UnlockWithBiometric,
) : ViewModel() {

    private val _state = MutableStateFlow(UnlockState())
    val state: StateFlow<UnlockState> = _state.asStateFlow()

    private val _effects = Channel<UnlockEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeBiometricStatus()
            .onEach { status -> _state.update { it.copy(biometricStatus = status) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: UnlockIntent) {
        when (intent) {
            is UnlockIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null) }
            UnlockIntent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            UnlockIntent.Submit -> submit()
            UnlockIntent.BiometricClicked -> startBiometricUnlock()
            is UnlockIntent.BiometricApproved -> finishBiometricUnlock(intent.cipher)
            is UnlockIntent.BiometricFailed -> if (!intent.dismissedByUser) {
                _state.update { it.copy(error = VaultError.BiometricUnavailable) }
            }
        }
    }

    private fun submit() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val password = _state.value.password.toCharArray()
            val result = try {
                unlockVault(password)
            } finally {
                password.fill('\u0000')
            }
            onUnlockResult(result)
        }
    }

    private fun startBiometricUnlock() {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            when (val result = prepareBiometricUnlock()) {
                is Outcome.Success -> _effects.send(UnlockEffect.ShowBiometricPrompt(result.value))
                is Outcome.Failure -> _state.update { it.copy(error = result.error) }
            }
        }
    }

    private fun finishBiometricUnlock(cipher: Cipher) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch { onUnlockResult(unlockWithBiometric(cipher)) }
    }

    private suspend fun onUnlockResult(result: Outcome<Unit>) {
        when (result) {
            is Outcome.Success -> {
                _state.update { UnlockState(biometricStatus = it.biometricStatus) }
                _effects.send(UnlockEffect.Unlocked)
            }
            is Outcome.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
        }
    }
}
