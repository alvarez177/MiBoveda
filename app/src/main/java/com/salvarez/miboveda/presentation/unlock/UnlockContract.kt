package com.salvarez.miboveda.presentation.unlock

import com.salvarez.miboveda.domain.error.VaultError

data class UnlockState(
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: VaultError? = null,
) {
    // Keeps the password out of logs.
    override fun toString(): String = "UnlockState(isLoading=$isLoading, error=$error)"
}

sealed interface UnlockIntent {
    data class PasswordChanged(val value: String) : UnlockIntent
    data object TogglePasswordVisibility : UnlockIntent
    data object Submit : UnlockIntent
}

sealed interface UnlockEffect {
    data object Unlocked : UnlockEffect
}
