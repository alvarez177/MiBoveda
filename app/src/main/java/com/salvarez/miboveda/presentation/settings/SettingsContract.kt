package com.salvarez.miboveda.presentation.settings

import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.BiometricStatus
import javax.crypto.Cipher

data class SettingsState(
    val biometricStatus: BiometricStatus = BiometricStatus.UNAVAILABLE,
    val isPasswordDialogVisible: Boolean = false,
    val password: String = "",
    val isBusy: Boolean = false,
    val error: VaultError? = null,
) {
    // Keeps the password out of logs.
    override fun toString(): String = "SettingsState(biometricStatus=$biometricStatus, isBusy=$isBusy, error=$error)"
}

sealed interface SettingsIntent {
    data class ToggleBiometric(val enable: Boolean) : SettingsIntent
    data class PasswordChanged(val value: String) : SettingsIntent
    data object ConfirmPassword : SettingsIntent
    data object DismissPasswordDialog : SettingsIntent
    data class BiometricApproved(val cipher: Cipher) : SettingsIntent
    data class BiometricFailed(val dismissedByUser: Boolean) : SettingsIntent
}

sealed interface SettingsEffect {
    data class ShowBiometricPrompt(val cipher: Cipher) : SettingsEffect
}
