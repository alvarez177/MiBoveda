package com.salvarez.miboveda.presentation.unlock

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.domain.usecase.FakeBiometricRepository
import com.salvarez.miboveda.domain.usecase.FakeVaultRepository
import com.salvarez.miboveda.domain.usecase.ObserveBiometricStatus
import com.salvarez.miboveda.domain.usecase.PrepareBiometricUnlock
import com.salvarez.miboveda.domain.usecase.UnlockVault
import com.salvarez.miboveda.domain.usecase.UnlockWithBiometric
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
class UnlockViewModelTest {
    private val biometrics = FakeBiometricRepository(BiometricStatus.ENABLED)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repository: FakeVaultRepository = FakeVaultRepository()) = UnlockViewModel(
        UnlockVault(repository),
        ObserveBiometricStatus(biometrics),
        PrepareBiometricUnlock(biometrics),
        UnlockWithBiometric(biometrics),
    )

    @Test
    fun `emits Unlocked and clears the password on success`() = runTest {
        val vm = viewModel()
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

    @Test
    fun `exposes whether biometric unlock is enabled`() = runTest {
        assertEquals(BiometricStatus.ENABLED, viewModel().state.value.biometricStatus)
    }

    @Test
    fun `asking for biometrics shows the prompt with the prepared cipher`() = runTest {
        val vm = viewModel()
        vm.onIntent(UnlockIntent.BiometricClicked)

        assertEquals(UnlockEffect.ShowBiometricPrompt(biometrics.cipher), vm.effects.first())
    }

    @Test
    fun `changed biometrics show an explanation`() = runTest {
        biometrics.prepareUnlockResult = Outcome.Failure(VaultError.BiometricInvalidated)
        val vm = viewModel()
        vm.onIntent(UnlockIntent.BiometricClicked)

        assertEquals(VaultError.BiometricInvalidated, vm.state.value.error)
    }

    @Test
    fun `an approved prompt unlocks the vault`() = runTest {
        val vm = viewModel()
        vm.onIntent(UnlockIntent.BiometricApproved(biometrics.cipher))

        assertEquals(UnlockEffect.Unlocked, vm.effects.first())
    }

    @Test
    fun `a failed unlock after approval shows the error`() = runTest {
        biometrics.unlockResult = Outcome.Failure(VaultError.StorageFailure)
        val vm = viewModel()
        vm.onIntent(UnlockIntent.BiometricApproved(biometrics.cipher))

        assertEquals(VaultError.StorageFailure, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `dismissing the prompt shows nothing but a prompt error does`() = runTest {
        val vm = viewModel()
        vm.onIntent(UnlockIntent.BiometricFailed(dismissedByUser = true))
        assertEquals(null, vm.state.value.error)

        vm.onIntent(UnlockIntent.BiometricFailed(dismissedByUser = false))
        assertEquals(VaultError.BiometricUnavailable, vm.state.value.error)
    }
}
