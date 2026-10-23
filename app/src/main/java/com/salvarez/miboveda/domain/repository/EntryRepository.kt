package com.salvarez.miboveda.domain.repository

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.PasswordEntry
import kotlinx.coroutines.flow.Flow

interface EntryRepository {
    fun observeAll(): Flow<Outcome<List<PasswordEntry>>>

    suspend fun get(id: Long): Outcome<PasswordEntry>

    /** Inserts when [PasswordEntry.id] is [PasswordEntry.NEW_ID], updates otherwise. Returns the entry id. */
    suspend fun save(entry: PasswordEntry): Outcome<Long>

    suspend fun delete(id: Long): Outcome<Unit>
}
