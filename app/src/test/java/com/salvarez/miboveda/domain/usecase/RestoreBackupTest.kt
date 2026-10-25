package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RestoreBackupTest {
    private val entry = PasswordEntry(id = 3, title = "Bank", username = "ana", password = "x")

    @Test
    fun `creates the vault and restores the entries, returning the new recovery key`() = runTest {
        val vault = FakeVaultRepository()
        val entries = FakeEntryRepository()
        val useCase = RestoreBackup(FakeBackupRepository(readResult = Outcome.Success(listOf(entry))), vault, entries)

        val result = useCase("a long enough password".toCharArray(), "uri")

        assertEquals(Outcome.Success(FakeVaultRepository.SAMPLE_RECOVERY_KEY), result)
        assertEquals(1, vault.createCalls)
        assertEquals(listOf("Bank"), entries.current.map { it.title })
    }

    @Test
    fun `a wrong password or a damaged file leaves no vault behind`() = runTest {
        val vault = FakeVaultRepository()
        val useCase = RestoreBackup(FakeBackupRepository(readResult = Outcome.Failure(VaultError.CorruptBackup)), vault, FakeEntryRepository())

        assertEquals(Outcome.Failure(VaultError.CorruptBackup), useCase("pw".toCharArray(), "uri"))
        assertEquals(0, vault.createCalls)
    }

    @Test
    fun `a failure creating the vault is reported and nothing is imported`() = runTest {
        val entries = FakeEntryRepository()
        val vault = FakeVaultRepository(createResult = Outcome.Failure(VaultError.StorageFailure))
        val useCase = RestoreBackup(FakeBackupRepository(readResult = Outcome.Success(listOf(entry))), vault, entries)

        assertEquals(Outcome.Failure(VaultError.StorageFailure), useCase("pw".toCharArray(), "uri"))
        assertEquals(0, entries.current.size)
    }
}
