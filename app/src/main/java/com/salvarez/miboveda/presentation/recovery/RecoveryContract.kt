package com.salvarez.miboveda.presentation.recovery

import com.salvarez.miboveda.domain.error.VaultError

data class RecoveryState(
    val recoveryKey: String = "",
    val password: String = "",
    val confirmation: String = "",
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: VaultError? = null,
) {
    // Keeps the key and the passwords out of logs.
    override fun toString(): String = "RecoveryState(isLoading=$isLoading, error=$error)"
}

sealed interface RecoveryIntent {
    data class RecoveryKeyChanged(val value: String) : RecoveryIntent {
        override fun toString(): String = "RecoveryKeyChanged"
    }
    data class PasswordChanged(val value: String) : RecoveryIntent {
        override fun toString(): String = "PasswordChanged"
    }
    data class ConfirmationChanged(val value: String) : RecoveryIntent {
        override fun toString(): String = "ConfirmationChanged"
    }
    data object ToggleVisibility : RecoveryIntent
    data object Submit : RecoveryIntent
}

sealed interface RecoveryEffect {
    data object Recovered : RecoveryEffect
}
