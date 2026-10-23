package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockLockVaultTest {
    @Test
    fun `unlock forwards the repository result`() = runTest {
        val failing = FakeVaultRepository(unlockResult = Outcome.Failure(VaultError.WrongMasterPassword))
        assertEquals(Outcome.Failure(VaultError.WrongMasterPassword), UnlockVault(failing)("x".toCharArray()))

        val working = FakeVaultRepository()
        assertEquals(Outcome.Success(Unit), UnlockVault(working)("x".toCharArray()))
        assertTrue(working.isUnlocked.value)
    }

    @Test
    fun `lock locks the vault`() = runTest {
        val repository = FakeVaultRepository(unlocked = true)
        LockVault(repository)()
        assertFalse(repository.isUnlocked.value)
        assertEquals(1, repository.lockCalls)
    }
}
