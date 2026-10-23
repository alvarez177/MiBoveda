package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.repository.EntryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveEntries @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    operator fun invoke(): Flow<Outcome<List<PasswordEntry>>> = entryRepository.observeAll()
}
