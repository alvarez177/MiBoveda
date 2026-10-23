package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.repository.EntryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveEntry @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    operator fun invoke(id: Long): Flow<Outcome<PasswordEntry>> = entryRepository.observe(id)
}
