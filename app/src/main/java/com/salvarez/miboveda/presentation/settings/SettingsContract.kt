package com.salvarez.miboveda.presentation.settings

import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.model.BiometricStatus
import javax.crypto.Cipher

data class SettingsState(
    val biometricStatus: BiometricStatus = BiometricStatus.UNAVAILABLE,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.DEFAULT,
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
    data class SelectAutoLockTimeout(val timeout: AutoLockTimeout) : SettingsIntent
    data class PasswordChanged(val value: String) : SettingsIntent {
        override fun toString(): String = "PasswordChanged"
    }
    data object ConfirmPassword : SettingsIntent
    data object DismissPasswordDialog : SettingsIntent
    data class BiometricApproved(val cipher: Cipher) : SettingsIntent
    data class BiometricFailed(val dismissedByUser: Boolean) : SettingsIntent
}

sealed interface SettingsEffect {
    data class ShowBiometricPrompt(val cipher: Cipher) : SettingsEffect
}
