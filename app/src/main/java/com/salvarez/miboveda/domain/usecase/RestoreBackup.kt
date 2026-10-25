package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.RecoveryKey
import com.salvarez.miboveda.domain.repository.BackupRepository
import com.salvarez.miboveda.domain.repository.EntryRepository
import com.salvarez.miboveda.domain.repository.VaultRepository
import javax.inject.Inject

/**
 * Builds a new vault from a backup, for a device that has none yet. The backup's password becomes the
 * master password of the new vault, and the new vault gets its own recovery key.
 */
class RestoreBackup @Inject constructor(
    private val backupRepository: BackupRepository,
    private val vaultRepository: VaultRepository,
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke(masterPassword: CharArray, sourceUri: String): Outcome<RecoveryKey> {
        // Open the backup first: a wrong password or a damaged file must not leave an empty vault behind.
        val entries = when (val result = backupRepository.read(masterPassword, sourceUri)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value
        }
        val recoveryKey = when (val result = vaultRepository.create(masterPassword)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value
        }
        return when (val result = entryRepository.addImported(entries)) {
            is Outcome.Failure -> result
            is Outcome.Success -> Outcome.Success(recoveryKey)
        }
    }
}
