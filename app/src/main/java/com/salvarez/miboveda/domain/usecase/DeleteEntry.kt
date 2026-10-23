package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.repository.EntryRepository
import javax.inject.Inject

class DeleteEntry @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke(id: Long): Outcome<Unit> = entryRepository.delete(id)
}
