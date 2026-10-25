package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.repository.BackupRepository

class FakeBackupRepository(
    var result: Outcome<Unit> = Outcome.Success(Unit),
    var readResult: Outcome<List<PasswordEntry>> = Outcome.Success(emptyList()),
) : BackupRepository {
    /** Password (copied before the caller clears it) and destination of every export. */
    val exports = mutableListOf<Pair<String, String>>()

    /** Password (copied before the caller clears it) and source of every read. */
    val reads = mutableListOf<Pair<String, String>>()

    override suspend fun export(masterPassword: CharArray, destinationUri: String): Outcome<Unit> {
        exports += String(masterPassword) to destinationUri
        return result
    }

    override suspend fun read(masterPassword: CharArray, sourceUri: String): Outcome<List<PasswordEntry>> {
        reads += String(masterPassword) to sourceUri
        return readResult
    }
}
