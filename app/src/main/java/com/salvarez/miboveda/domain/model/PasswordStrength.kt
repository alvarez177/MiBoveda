package com.salvarez.miboveda.domain.model

const val MIN_MASTER_PASSWORD_LENGTH = 12

enum class PasswordStrength { WEAK, MEDIUM, STRONG }

/** Rough length and variety check; it only guides the user, it is not a guarantee. */
fun evaluatePasswordStrength(password: CharArray): PasswordStrength {
    val classes = listOf<(Char) -> Boolean>(
        Char::isLowerCase,
        Char::isUpperCase,
        Char::isDigit,
        { c -> !c.isLetterOrDigit() },
    ).count { matches -> password.any(matches) }
    return when {
        password.size < MIN_MASTER_PASSWORD_LENGTH || classes < 2 -> PasswordStrength.WEAK
        password.size >= 16 && classes >= 3 -> PasswordStrength.STRONG
        else -> PasswordStrength.MEDIUM
    }
}
