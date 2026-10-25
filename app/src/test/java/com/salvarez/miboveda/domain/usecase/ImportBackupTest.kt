package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.ImportSummary
import com.salvarez.miboveda.domain.model.PasswordEntry
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportBackupTest {
    private val bank = PasswordEntry(id = 5, title = "Bank", username = "ana", password = "x", updatedAt = Instant.ofEpochMilli(1))
    private val mail = PasswordEntry(id = 6, title = "Mail", username = "ana", password = "y", updatedAt = Instant.ofEpochMilli(2))

    private fun import(backup: FakeBackupRepository, entries: FakeEntryRepository) =
        ImportBackup(backup, entries)

    @Test
    fun `adds every entry of the backup to an empty vault`() = runTest {
        val entries = FakeEntryRepository()
        val result = import(FakeBackupRepository(readResult = Outcome.Success(listOf(bank, mail))), entries)("pw".toCharArray(), "uri")

        assertEquals(Outcome.Success(ImportSummary(added = 2, skipped = 0)), result)
        assertEquals(listOf("Bank", "Mail"), entries.current.map { it.title })
    }

    @Test
    fun `leaves out entries that are already in the vault, whatever their id or date`() = runTest {
        val entries = FakeEntryRepository(listOf(bank.copy(id = 1, updatedAt = Instant.ofEpochMilli(99))))
        val result = import(FakeBackupRepository(readResult = Outcome.Success(listOf(bank, mail))), entries)("pw".toCharArray(), "uri")

        assertEquals(Outcome.Success(ImportSummary(added = 1, skipped = 1)), result)
        assertEquals(listOf("Bank", "Mail"), entries.current.map { it.title }.sorted())
    }

    @Test
    fun `importing the same backup twice adds nothing the second time`() = runTest {
        val entries = FakeEntryRepository()
        val useCase = import(FakeBackupRepository(readResult = Outcome.Success(listOf(bank, mail))), entries)
        useCase("pw".toCharArray(), "uri")

        assertEquals(Outcome.Success(ImportSummary(added = 0, skipped = 2)), useCase("pw".toCharArray(), "uri"))
        assertEquals(2, entries.current.size)
    }

    @Test
    fun `duplicates inside the backup are added once`() = runTest {
        val entries = FakeEntryRepository()
        val result = import(FakeBackupRepository(readResult = Outcome.Success(listOf(bank, bank.copy(id = 9)))), entries)("pw".toCharArray(), "uri")

        assertEquals(Outcome.Success(ImportSummary(added = 1, skipped = 1)), result)
    }

    @Test
    fun `a backup that cannot be opened changes nothing`() = runTest {
        val entries = FakeEntryRepository()
        val result = import(FakeBackupRepository(readResult = Outcome.Failure(VaultError.WrongBackupPassword)), entries)("pw".toCharArray(), "uri")

        assertEquals(Outcome.Failure(VaultError.WrongBackupPassword), result)
        assertEquals(0, entries.current.size)
    }

    @Test
    fun `a storage failure is reported`() = runTest {
        val entries = FakeEntryRepository(importResult = Outcome.Failure(VaultError.StorageFailure))
        val result = import(FakeBackupRepository(readResult = Outcome.Success(listOf(bank))), entries)("pw".toCharArray(), "uri")

        assertEquals(Outcome.Failure(VaultError.StorageFailure), result)
    }
}
