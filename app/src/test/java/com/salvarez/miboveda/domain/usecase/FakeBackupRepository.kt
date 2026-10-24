package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.repository.BackupRepository

class FakeBackupRepository(var result: Outcome<Unit> = Outcome.Success(Unit)) : BackupRepository {
    /** Password (copied before the caller clears it) and destination of every export. */
    val exports = mutableListOf<Pair<String, String>>()

    override suspend fun export(masterPassword: CharArray, destinationUri: String): Outcome<Unit> {
        exports += String(masterPassword) to destinationUri
        return result
    }
}
