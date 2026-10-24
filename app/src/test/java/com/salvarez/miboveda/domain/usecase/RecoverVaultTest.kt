package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecoverVaultTest {
    private val newPassword = "a brand new password".toCharArray()

    @Test
    fun `rejects a short new password without calling the repository`() = runTest {
        val repository = FakeVaultRepository()
        val result = RecoverVault(repository)("key", "short".toCharArray(), "short".toCharArray())

        assertEquals(Outcome.Failure(VaultError.MasterPasswordTooShort), result)
        assertEquals(0, repository.recoverCalls)
    }

    @Test
    fun `rejects a mismatching confirmation`() = runTest {
        val repository = FakeVaultRepository()
        val result = RecoverVault(repository)("key", newPassword, "another new password".toCharArray())

        assertEquals(Outcome.Failure(VaultError.MasterPasswordMismatch), result)
        assertEquals(0, repository.recoverCalls)
    }

    @Test
    fun `recovers with a valid new password`() = runTest {
        val repository = FakeVaultRepository()
        val result = RecoverVault(repository)("key", newPassword, newPassword.copyOf())

        assertEquals(Outcome.Success(Unit), result)
        assertEquals(1, repository.recoverCalls)
    }
}
