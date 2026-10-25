package com.salvarez.miboveda.domain.repository

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.PasswordEntry

interface BackupRepository {
    /**
     * Writes every entry to [destinationUri] in an encrypted file that only [masterPassword] can open.
     * Fails with `WrongMasterPassword` (and writes nothing) when the password is not the vault's.
     * The caller owns [masterPassword] and may clear it afterwards.
     */
    suspend fun export(masterPassword: CharArray, destinationUri: String): Outcome<Unit>

    /**
     * Opens the backup at [sourceUri] with [masterPassword], the one the vault had when the backup was made.
     * Fails with `WrongBackupPassword` or `CorruptBackup`. Needs no vault, so it also works on a new device.
     */
    suspend fun read(masterPassword: CharArray, sourceUri: String): Outcome<List<PasswordEntry>>
}
