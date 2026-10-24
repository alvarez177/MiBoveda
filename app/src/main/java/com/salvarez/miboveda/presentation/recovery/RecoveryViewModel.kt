package com.salvarez.miboveda.presentation.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.usecase.RecoverVault
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
class RecoveryViewModel @Inject constructor(
    private val recoverVault: RecoverVault,
) : ViewModel() {

    private val _state = MutableStateFlow(RecoveryState())
    val state: StateFlow<RecoveryState> = _state.asStateFlow()

    private val _effects = Channel<RecoveryEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: RecoveryIntent) {
        when (intent) {
            is RecoveryIntent.RecoveryKeyChanged -> _state.update { it.copy(recoveryKey = intent.value, error = null) }
            is RecoveryIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null) }
            is RecoveryIntent.ConfirmationChanged -> _state.update { it.copy(confirmation = intent.value, error = null) }
            RecoveryIntent.ToggleVisibility -> _state.update { it.copy(isVisible = !it.isVisible) }
            RecoveryIntent.Submit -> submit()
        }
    }

    private fun submit() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val current = _state.value
            val password = current.password.toCharArray()
            val confirmation = current.confirmation.toCharArray()
            val result = try {
                recoverVault(current.recoveryKey, password, confirmation)
            } finally {
                password.fill('\u0000')
                confirmation.fill('\u0000')
            }
            when (result) {
                is Outcome.Success -> {
                    _state.value = RecoveryState()
                    _effects.send(RecoveryEffect.Recovered)
                }
                is Outcome.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
