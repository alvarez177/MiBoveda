package com.salvarez.miboveda.presentation.onboarding

import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordStrength
import com.salvarez.miboveda.domain.model.RecoveryKey
import com.salvarez.miboveda.domain.model.evaluatePasswordStrength

data class OnboardingState(
    val password: String = "",
    val confirmation: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: VaultError? = null,
    /** Set once the vault exists: the screen then shows the recovery key until the user confirms saving it. */
    val recoveryKey: RecoveryKey? = null,
    val isRecoverySaved: Boolean = false,
    /** Set when the user chose a backup to restore: the form then asks only for the backup's password. */
    val restoreFileUri: String? = null,
) {
    val strength: PasswordStrength get() = evaluatePasswordStrength(password.toCharArray())

    // Keeps the passwords and the recovery key out of logs.
    override fun toString(): String =
        "OnboardingState(isLoading=$isLoading, error=$error, hasRecoveryKey=${recoveryKey != null})"
}

sealed interface OnboardingIntent {
    data class PasswordChanged(val value: String) : OnboardingIntent {
        override fun toString(): String = "PasswordChanged"
    }
    data class ConfirmationChanged(val value: String) : OnboardingIntent {
        override fun toString(): String = "ConfirmationChanged"
    }
    data object TogglePasswordVisibility : OnboardingIntent
    data object Submit : OnboardingIntent
    data object RestoreClicked : OnboardingIntent

    /** [uri] is null when the user closed the file picker without choosing a file. */
    data class BackupFileChosen(val uri: String?) : OnboardingIntent
    data object CancelRestore : OnboardingIntent
    data object CopyRecoveryKey : OnboardingIntent
    data object ToggleRecoverySaved : OnboardingIntent
    data object ContinueAfterRecovery : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object VaultCreated : OnboardingEffect
    data object PickBackupFile : OnboardingEffect
}
