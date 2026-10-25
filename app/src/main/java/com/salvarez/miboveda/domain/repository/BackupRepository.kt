package com.salvarez.miboveda.domain.repository

import com.salvarez.miboveda.domain.Outcome

interface BackupRepository {
    /**
     * Writes every entry to [destinationUri] in an encrypted file that only [masterPassword] can open.
     * Fails with `WrongMasterPassword` (and writes nothing) when the password is not the vault's.
     * The caller owns [masterPassword] and may clear it afterwards.
     */
    suspend fun export(masterPassword: CharArray, destinationUri: String): Outcome<Unit>
}
