package com.salvarez.miboveda.domain.model

import com.salvarez.miboveda.domain.error.VaultError

/** Returns why a new master password cannot be used, or null when it is fine. */
fun validateNewMasterPassword(password: CharArray, confirmation: CharArray): VaultError? = when {
    password.size < MIN_MASTER_PASSWORD_LENGTH -> VaultError.MasterPasswordTooShort
    !password.contentEquals(confirmation) -> VaultError.MasterPasswordMismatch
    else -> null
}
