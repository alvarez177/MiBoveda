package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.repository.BackupRepository
import javax.inject.Inject

class ExportBackup @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(masterPassword: CharArray, destinationUri: String): Outcome<Unit> =
        backupRepository.export(masterPassword, destinationUri)
}
