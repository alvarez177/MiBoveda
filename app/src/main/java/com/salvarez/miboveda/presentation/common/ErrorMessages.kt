package com.salvarez.miboveda.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.MIN_MASTER_PASSWORD_LENGTH

@Composable
fun VaultError.message(): String = when (this) {
    VaultError.WrongMasterPassword -> stringResource(R.string.error_wrong_master_password)
    VaultError.VaultLocked -> stringResource(R.string.error_vault_locked)
    VaultError.MasterPasswordTooShort -> stringResource(R.string.error_master_password_too_short, MIN_MASTER_PASSWORD_LENGTH)
    VaultError.MasterPasswordMismatch -> stringResource(R.string.error_master_password_mismatch)
    VaultError.VaultNotCreated -> stringResource(R.string.error_vault_not_created)
    VaultError.VaultAlreadyExists -> stringResource(R.string.error_vault_already_exists)
    VaultError.EntryNotFound -> stringResource(R.string.error_entry_not_found)
    VaultError.BiometricUnavailable -> stringResource(R.string.error_biometric_unavailable)
    VaultError.CorruptBackup -> stringResource(R.string.error_corrupt_backup)
    VaultError.StorageFailure -> stringResource(R.string.error_storage_failure)
}
