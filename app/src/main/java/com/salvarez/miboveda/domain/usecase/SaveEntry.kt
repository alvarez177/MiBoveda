package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.repository.EntryRepository
import javax.inject.Inject

class SaveEntry @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke(entry: PasswordEntry): Outcome<Long> =
        if (entry.title.isBlank()) {
            Outcome.Failure(VaultError.EntryTitleRequired)
        } else {
            entryRepository.save(entry.copy(title = entry.title.trim()))
        }
}
