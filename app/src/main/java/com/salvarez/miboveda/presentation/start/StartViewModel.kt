package com.salvarez.miboveda.presentation.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StartState {
    data object Loading : StartState
    data object NoVault : StartState
    data object VaultExists : StartState
    data class Failed(val error: VaultError) : StartState
}

@HiltViewModel
class StartViewModel @Inject constructor(
    vaultRepository: VaultRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<StartState>(StartState.Loading)
    val state: StateFlow<StartState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = when (val result = vaultRepository.exists()) {
                is Outcome.Success -> if (result.value) StartState.VaultExists else StartState.NoVault
                is Outcome.Failure -> StartState.Failed(result.error)
            }
        }
    }
}
