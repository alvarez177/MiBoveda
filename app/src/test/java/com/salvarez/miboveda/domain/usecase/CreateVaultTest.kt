package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateVaultTest {
    private val repository = FakeVaultRepository()
    private val createVault = CreateVault(repository)

    @Test
    fun `rejects a short password without calling the repository`() = runTest {
        val result = createVault("short".toCharArray(), "short".toCharArray())
        assertEquals(Outcome.Failure(VaultError.MasterPasswordTooShort), result)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `rejects a mismatching confirmation`() = runTest {
        val result = createVault("a long enough password".toCharArray(), "another long password".toCharArray())
        assertEquals(Outcome.Failure(VaultError.MasterPasswordMismatch), result)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `creates the vault with a valid password`() = runTest {
        val result = createVault("a long enough password".toCharArray(), "a long enough password".toCharArray())
        assertEquals(Outcome.Success(Unit), result)
        assertEquals(1, repository.createCalls)
    }
}
