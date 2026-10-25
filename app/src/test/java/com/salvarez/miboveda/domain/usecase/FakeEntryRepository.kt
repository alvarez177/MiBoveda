package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.repository.EntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeEntryRepository(
    initial: List<PasswordEntry> = emptyList(),
    private val importResult: Outcome<Unit> = Outcome.Success(Unit),
) : EntryRepository {
    private val entries = MutableStateFlow(initial)

    val current: List<PasswordEntry> get() = entries.value

    override fun observeAll(): Flow<Outcome<List<PasswordEntry>>> =
        entries.map<List<PasswordEntry>, Outcome<List<PasswordEntry>>> { Outcome.Success(it) }

    override fun observe(id: Long): Flow<Outcome<PasswordEntry>> = entries.map { list ->
        list.find { it.id == id }?.let { Outcome.Success(it) } ?: Outcome.Failure(VaultError.EntryNotFound)
    }

    override suspend fun save(entry: PasswordEntry): Outcome<Long> {
        val saved = if (entry.id == PasswordEntry.NEW_ID) {
            entry.copy(id = (entries.value.maxOfOrNull { it.id } ?: 0) + 1)
        } else {
            entry
        }
        entries.value = entries.value.filterNot { it.id == saved.id } + saved
        return Outcome.Success(saved.id)
    }

    override suspend fun delete(id: Long): Outcome<Unit> {
        entries.value = entries.value.filterNot { it.id == id }
        return Outcome.Success(Unit)
    }

    override suspend fun addImported(entries: List<PasswordEntry>): Outcome<Unit> {
        if (importResult is Outcome.Failure) return importResult
        var nextId = (this.entries.value.maxOfOrNull { it.id } ?: 0) + 1
        this.entries.value = this.entries.value + entries.map { it.copy(id = nextId++) }
        return Outcome.Success(Unit)
    }
}
