package com.salvarez.miboveda.presentation.unlock

import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.BiometricStatus
import javax.crypto.Cipher

data class UnlockState(
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: VaultError? = null,
    val biometricStatus: BiometricStatus = BiometricStatus.UNAVAILABLE,
) {
    // Keeps the password out of logs.
    override fun toString(): String = "UnlockState(isLoading=$isLoading, error=$error)"
}

sealed interface UnlockIntent {
    data class PasswordChanged(val value: String) : UnlockIntent {
        override fun toString(): String = "PasswordChanged"
    }
    data object TogglePasswordVisibility : UnlockIntent
    data object Submit : UnlockIntent
    data object BiometricClicked : UnlockIntent
    data class BiometricApproved(val cipher: Cipher) : UnlockIntent

    /** The prompt closed without approval; [dismissedByUser] is true when the user just cancelled it. */
    data class BiometricFailed(val dismissedByUser: Boolean) : UnlockIntent
}

sealed interface UnlockEffect {
    data object Unlocked : UnlockEffect
    data class ShowBiometricPrompt(val cipher: Cipher) : UnlockEffect
}
