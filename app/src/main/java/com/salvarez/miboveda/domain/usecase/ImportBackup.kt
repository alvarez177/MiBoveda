package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.ImportSummary
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.repository.BackupRepository
import com.salvarez.miboveda.domain.repository.EntryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Adds the entries of a backup to the open vault, leaving out the ones that are already there. */
class ImportBackup @Inject constructor(
    private val backupRepository: BackupRepository,
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke(masterPassword: CharArray, sourceUri: String): Outcome<ImportSummary> {
        val backed = when (val result = backupRepository.read(masterPassword, sourceUri)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value
        }
        val existing = when (val result = entryRepository.observeAll().first()) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value.map { it.content() }.toSet()
        }
        val toAdd = backed.filter { it.content() !in existing }.distinctBy { it.content() }
        return when (val result = entryRepository.addImported(toAdd)) {
            is Outcome.Failure -> result
            is Outcome.Success -> Outcome.Success(ImportSummary(added = toAdd.size, skipped = backed.size - toAdd.size))
        }
    }

    // Two entries are the same when everything the user typed matches; ids and dates do not count.
    private fun PasswordEntry.content() = listOf(title, username, password, url, notes)
}
