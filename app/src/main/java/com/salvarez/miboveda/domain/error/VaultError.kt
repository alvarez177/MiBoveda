package com.salvarez.miboveda.domain.error

sealed interface VaultError {
    data object WrongMasterPassword : VaultError
    data object VaultLocked : VaultError
    data object MasterPasswordTooShort : VaultError
    data object MasterPasswordMismatch : VaultError
    data object VaultNotCreated : VaultError
    data object VaultAlreadyExists : VaultError
    data object EntryNotFound : VaultError
    data object EntryTitleRequired : VaultError
    data object BiometricUnavailable : VaultError
    data object CorruptBackup : VaultError
    data object StorageFailure : VaultError
}
