package com.salvarez.miboveda.presentation.recovery

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.usecase.FakeVaultRepository
import com.salvarez.miboveda.domain.usecase.RecoverVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class RecoveryViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun RecoveryViewModel.fill(key: String = "KEY", password: String = "a brand new password", confirmation: String = password) {
        onIntent(RecoveryIntent.RecoveryKeyChanged(key))
        onIntent(RecoveryIntent.PasswordChanged(password))
        onIntent(RecoveryIntent.ConfirmationChanged(confirmation))
    }

    @Test
    fun `a valid key and new password recover the vault`() = runTest {
        val repository = FakeVaultRepository()
        val vm = RecoveryViewModel(RecoverVault(repository))
        vm.fill()
        vm.onIntent(RecoveryIntent.Submit)

        assertEquals(RecoveryEffect.Recovered, vm.effects.first())
        assertEquals("", vm.state.value.password)
        assertEquals("", vm.state.value.recoveryKey)
    }

    @Test
    fun `an invalid key shows an error and stays on the screen`() = runTest {
        val vm = RecoveryViewModel(RecoverVault(FakeVaultRepository(recoverResult = Outcome.Failure(VaultError.InvalidRecoveryKey))))
        vm.fill()
        vm.onIntent(RecoveryIntent.Submit)

        assertEquals(VaultError.InvalidRecoveryKey, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `a mismatching new password is rejected before reaching the repository`() = runTest {
        val repository = FakeVaultRepository()
        val vm = RecoveryViewModel(RecoverVault(repository))
        vm.fill(confirmation = "another new password!")
        vm.onIntent(RecoveryIntent.Submit)

        assertEquals(VaultError.MasterPasswordMismatch, vm.state.value.error)
        assertEquals(0, repository.recoverCalls)
    }

    @Test
    fun `editing a field clears the error`() = runTest {
        val vm = RecoveryViewModel(RecoverVault(FakeVaultRepository(recoverResult = Outcome.Failure(VaultError.InvalidRecoveryKey))))
        vm.fill()
        vm.onIntent(RecoveryIntent.Submit)
        vm.onIntent(RecoveryIntent.RecoveryKeyChanged("KEY2"))

        assertEquals(null, vm.state.value.error)
    }

    @Test
    fun `state and intents do not leak the secrets`() {
        assertFalse(RecoveryState(recoveryKey = "s3cret-key", password = "s3cret-pw").toString().contains("s3cret"))
        assertFalse(RecoveryIntent.RecoveryKeyChanged("s3cret-key").toString().contains("s3cret"))
    }
}
