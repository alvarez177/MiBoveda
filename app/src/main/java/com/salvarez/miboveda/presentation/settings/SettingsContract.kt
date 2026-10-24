package com.salvarez.miboveda.presentation.settings

import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.domain.model.RecoveryKey
import javax.crypto.Cipher

/** What the master password dialog is being asked for. */
enum class PasswordDialogPurpose { BIOMETRIC, RECOVERY_KEY }

data class SettingsState(
    val biometricStatus: BiometricStatus = BiometricStatus.UNAVAILABLE,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.DEFAULT,
    val hasRecoveryKey: Boolean = false,
    /** The key just generated; shown once and dropped as soon as the user closes the dialog. */
    val newRecoveryKey: RecoveryKey? = null,
    val isPasswordDialogVisible: Boolean = false,
    val passwordDialogPurpose: PasswordDialogPurpose = PasswordDialogPurpose.BIOMETRIC,
    val password: String = "",
    val isBusy: Boolean = false,
    val error: VaultError? = null,
) {
    // Keeps the password out of logs.
    override fun toString(): String = "SettingsState(biometricStatus=$biometricStatus, isBusy=$isBusy, error=$error)"
}

sealed interface SettingsIntent {
    data class ToggleBiometric(val enable: Boolean) : SettingsIntent
    data object RequestRecoveryKey : SettingsIntent
    data object CopyRecoveryKey : SettingsIntent
    data object DismissRecoveryKey : SettingsIntent
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
