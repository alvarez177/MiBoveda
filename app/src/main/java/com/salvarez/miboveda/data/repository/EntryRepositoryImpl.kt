package com.salvarez.miboveda.data.repository

import com.salvarez.miboveda.data.db.EntryEntity
import com.salvarez.miboveda.data.db.VaultLockedException
import com.salvarez.miboveda.data.db.VaultSession
import com.salvarez.miboveda.data.db.toDomain
import com.salvarez.miboveda.data.db.toEntity
import com.salvarez.miboveda.data.outcomeOf
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.repository.EntryRepository
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class EntryRepositoryImpl @Inject constructor(
    private val session: VaultSession,
) : EntryRepository {

    override fun observeAll(): Flow<Outcome<List<PasswordEntry>>> =
        observing { session.entryDao().observeAll() }.map { outcome ->
            when (outcome) {
                is Outcome.Success -> Outcome.Success(outcome.value.map(EntryEntity::toDomain))
                is Outcome.Failure -> outcome
            }
        }

    override fun observe(id: Long): Flow<Outcome<PasswordEntry>> =
        observing { session.entryDao().observeById(id) }.map { outcome ->
            when (outcome) {
                is Outcome.Success -> outcome.value?.let { Outcome.Success(it.toDomain()) }
                    ?: Outcome.Failure(VaultError.EntryNotFound)
                is Outcome.Failure -> outcome
            }
        }

    override suspend fun save(entry: PasswordEntry): Outcome<Long> = outcomeOf(::errorFor) {
        val toSave = entry.copy(updatedAt = Instant.now())
        // Upsert returns the new row id on insert and -1 when an existing row was updated.
        val rowId = session.entryDao().upsert(toSave.toEntity())
        if (rowId == UPDATED_ROW) toSave.id else rowId
    }

    override suspend fun delete(id: Long): Outcome<Unit> = outcomeOf(::errorFor) {
        session.entryDao().deleteById(id)
        Unit
    }

    private fun <T> observing(source: () -> Flow<T>): Flow<Outcome<T>> =
        flow<Outcome<T>> { emitAll(source().map { Outcome.Success(it) }) }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(Outcome.Failure(errorFor(e)))
            }

    private fun errorFor(e: Throwable): VaultError =
        if (e is VaultLockedException) VaultError.VaultLocked else VaultError.StorageFailure

    private companion object {
        const val UPDATED_ROW = -1L
    }
}
