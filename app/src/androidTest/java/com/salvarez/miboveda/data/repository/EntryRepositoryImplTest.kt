package com.salvarez.miboveda.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.salvarez.miboveda.data.db.VaultSession
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EntryRepositoryImplTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val session = VaultSession(context)
    private val repository = EntryRepositoryImpl(session)
    private val key = ByteArray(32) { it.toByte() }

    @Before
    fun setUp() = deleteDatabaseFiles()

    @After
    fun tearDown() {
        session.close()
        deleteDatabaseFiles()
    }

    private suspend fun entries() = (repository.observeAll().first() as Outcome.Success).value

    @Test
    fun savedEntriesAreListedAndUpdatedInPlace() = runTest {
        session.open(key)

        val id = (repository.save(PasswordEntry(title = "Bank", username = "ana", password = "one")) as Outcome.Success).value
        assertTrue(id > 0)
        assertEquals(listOf("Bank"), entries().map { it.title })

        repository.save(PasswordEntry(id = id, title = "Bank", username = "ana", password = "two"))

        val entry = entries().single()
        assertEquals(id, entry.id)
        assertEquals("two", entry.password)
    }

    @Test
    fun observeEmitsTheEntryAndThenNotFoundAfterDeleting() = runTest {
        session.open(key)
        val id = (repository.save(PasswordEntry(title = "Bank", username = "ana", password = "x")) as Outcome.Success).value

        assertEquals("Bank", (repository.observe(id).first() as Outcome.Success).value.title)

        repository.delete(id)

        assertEquals(Outcome.Failure(VaultError.EntryNotFound), repository.observe(id).first())
        assertTrue(entries().isEmpty())
    }

    @Test
    fun importedEntriesAreAddedAsNewOnesKeepingTheirDates() = runTest {
        session.open(key)
        val imported = listOf(
            PasswordEntry(id = 77, title = "Bank", username = "ana", password = "x", updatedAt = java.time.Instant.ofEpochMilli(1_000)),
            PasswordEntry(id = 77, title = "Mail", username = "ana", password = "y", updatedAt = java.time.Instant.ofEpochMilli(2_000)),
        )

        assertEquals(Outcome.Success(Unit), repository.addImported(imported))

        val stored = entries()
        assertEquals(listOf("Bank", "Mail"), stored.map { it.title })
        assertEquals(2, stored.map { it.id }.toSet().size)
        assertEquals(listOf(1_000L, 2_000L), stored.map { it.updatedAt.toEpochMilli() })
    }

    @Test
    fun operationsWhileLockedFailWithVaultLocked() = runTest {
        assertEquals(Outcome.Failure(VaultError.VaultLocked), repository.observeAll().first())
        assertEquals(
            Outcome.Failure(VaultError.VaultLocked),
            repository.save(PasswordEntry(title = "Bank", username = "ana", password = "x")),
        )
        assertEquals(Outcome.Failure(VaultError.VaultLocked), repository.delete(1))
    }

    private fun deleteDatabaseFiles() {
        val file = session.databaseFile()
        listOf("", "-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }
    }
}
