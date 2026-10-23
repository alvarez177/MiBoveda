package com.salvarez.miboveda.presentation.onboarding

import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordStrength
import com.salvarez.miboveda.domain.model.evaluatePasswordStrength

data class OnboardingState(
    val password: String = "",
    val confirmation: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: VaultError? = null,
) {
    val strength: PasswordStrength get() = evaluatePasswordStrength(password.toCharArray())

    // Keeps the passwords out of logs.
    override fun toString(): String = "OnboardingState(isLoading=$isLoading, error=$error)"
}

sealed interface OnboardingIntent {
    data class PasswordChanged(val value: String) : OnboardingIntent
    data class ConfirmationChanged(val value: String) : OnboardingIntent
    data object TogglePasswordVisibility : OnboardingIntent
    data object Submit : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object VaultCreated : OnboardingEffect
}
