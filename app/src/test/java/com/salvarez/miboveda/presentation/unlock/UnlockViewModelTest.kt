package com.salvarez.miboveda.presentation.unlock

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.usecase.FakeVaultRepository
import com.salvarez.miboveda.domain.usecase.UnlockVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class UnlockViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repository: FakeVaultRepository) = UnlockViewModel(UnlockVault(repository))

    @Test
    fun `emits Unlocked and clears the password on success`() = runTest {
        val vm = viewModel(FakeVaultRepository())
        vm.onIntent(UnlockIntent.PasswordChanged("my master password"))
        vm.onIntent(UnlockIntent.Submit)

        assertEquals(UnlockEffect.Unlocked, vm.effects.first())
        assertEquals("", vm.state.value.password)
    }

    @Test
    fun `shows the wrong password error and keeps the screen`() = runTest {
        val vm = viewModel(FakeVaultRepository(unlockResult = Outcome.Failure(VaultError.WrongMasterPassword)))
        vm.onIntent(UnlockIntent.PasswordChanged("wrong"))
        vm.onIntent(UnlockIntent.Submit)

        assertEquals(VaultError.WrongMasterPassword, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `editing the password clears the error`() = runTest {
        val vm = viewModel(FakeVaultRepository(unlockResult = Outcome.Failure(VaultError.WrongMasterPassword)))
        vm.onIntent(UnlockIntent.PasswordChanged("wrong"))
        vm.onIntent(UnlockIntent.Submit)
        vm.onIntent(UnlockIntent.PasswordChanged("wrong2"))

        assertEquals(null, vm.state.value.error)
    }
}
