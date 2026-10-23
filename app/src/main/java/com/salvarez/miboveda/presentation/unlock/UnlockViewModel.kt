package com.salvarez.miboveda.presentation.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.usecase.UnlockVault
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
class UnlockViewModel @Inject constructor(
    private val unlockVault: UnlockVault,
) : ViewModel() {

    private val _state = MutableStateFlow(UnlockState())
    val state: StateFlow<UnlockState> = _state.asStateFlow()

    private val _effects = Channel<UnlockEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: UnlockIntent) {
        when (intent) {
            is UnlockIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null) }
            UnlockIntent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            UnlockIntent.Submit -> submit()
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
            when (result) {
                is Outcome.Success -> {
                    _state.value = UnlockState()
                    _effects.send(UnlockEffect.Unlocked)
                }
                is Outcome.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
