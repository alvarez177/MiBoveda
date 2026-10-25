package com.salvarez.miboveda.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.usecase.CreateVault
import com.salvarez.miboveda.domain.usecase.RestoreBackup
import com.salvarez.miboveda.presentation.common.SensitiveClipboard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val createVault: CreateVault,
    private val restoreBackup: RestoreBackup,
    private val clipboard: SensitiveClipboard,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null) }
            is OnboardingIntent.ConfirmationChanged -> _state.update { it.copy(confirmation = intent.value, error = null) }
            OnboardingIntent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            OnboardingIntent.Submit -> submit()
            OnboardingIntent.RestoreClicked -> _effects.trySend(OnboardingEffect.PickBackupFile)
            is OnboardingIntent.BackupFileChosen -> intent.uri?.let { uri ->
                _state.update { it.copy(restoreFileUri = uri, password = "", confirmation = "", error = null) }
            }
            OnboardingIntent.CancelRestore -> _state.update { it.copy(restoreFileUri = null, password = "", error = null) }
            OnboardingIntent.CopyRecoveryKey -> _state.value.recoveryKey?.let { clipboard.copy("recovery", it.value) }
            OnboardingIntent.ToggleRecoverySaved -> _state.update { it.copy(isRecoverySaved = !it.isRecoverySaved) }
            OnboardingIntent.ContinueAfterRecovery -> continueAfterRecovery()
        }
    }

    private fun submit() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val password = _state.value.password.toCharArray()
            val confirmation = _state.value.confirmation.toCharArray()
            val restoreUri = _state.value.restoreFileUri
            val result = try {
                if (restoreUri != null) restoreBackup(password, restoreUri) else createVault(password, confirmation)
            } finally {
                password.fill('\u0000')
                confirmation.fill('\u0000')
            }
            when (result) {
                // The passwords are dropped here; only the recovery key stays until the user has saved it.
                is Outcome.Success -> _state.value = OnboardingState(recoveryKey = result.value)
                is Outcome.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    private fun continueAfterRecovery() {
        if (!_state.value.isRecoverySaved) return
        // Clear the key from memory before leaving the screen.
        _state.value = OnboardingState()
        _effects.trySend(OnboardingEffect.VaultCreated)
    }
}
