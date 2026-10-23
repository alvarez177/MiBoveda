package com.salvarez.miboveda.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.usecase.CreateVault
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
        }
    }

    private fun submit() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val password = _state.value.password.toCharArray()
            val confirmation = _state.value.confirmation.toCharArray()
            val result = try {
                createVault(password, confirmation)
            } finally {
                password.fill('\u0000')
                confirmation.fill('\u0000')
            }
            when (result) {
                is Outcome.Success -> {
                    _state.value = OnboardingState()
                    _effects.send(OnboardingEffect.VaultCreated)
                }
                is Outcome.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
