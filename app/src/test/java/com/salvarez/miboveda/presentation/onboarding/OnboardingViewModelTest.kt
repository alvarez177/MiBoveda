package com.salvarez.miboveda.presentation.onboarding

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.usecase.CreateVault
import com.salvarez.miboveda.domain.usecase.FakeVaultRepository
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

class OnboardingViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repository: FakeVaultRepository = FakeVaultRepository()) =
        OnboardingViewModel(CreateVault(repository))

    private fun OnboardingViewModel.fill(password: String, confirmation: String) {
        onIntent(OnboardingIntent.PasswordChanged(password))
        onIntent(OnboardingIntent.ConfirmationChanged(confirmation))
    }

    @Test
    fun `shows a mismatch error and stays on the screen`() = runTest {
        val vm = viewModel()
        vm.fill("a long enough password", "a different password!")
        vm.onIntent(OnboardingIntent.Submit)

        assertEquals(VaultError.MasterPasswordMismatch, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `emits VaultCreated and clears the passwords on success`() = runTest {
        val vm = viewModel()
        vm.fill("a long enough password", "a long enough password")
        vm.onIntent(OnboardingIntent.Submit)

        assertEquals(OnboardingEffect.VaultCreated, vm.effects.first())
        assertEquals("", vm.state.value.password)
        assertEquals("", vm.state.value.confirmation)
    }

    @Test
    fun `shows repository failures`() = runTest {
        val vm = viewModel(FakeVaultRepository(createResult = Outcome.Failure(VaultError.StorageFailure)))
        vm.fill("a long enough password", "a long enough password")
        vm.onIntent(OnboardingIntent.Submit)

        assertEquals(VaultError.StorageFailure, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `editing a field clears the error`() = runTest {
        val vm = viewModel()
        vm.fill("short", "short")
        vm.onIntent(OnboardingIntent.Submit)
        vm.onIntent(OnboardingIntent.PasswordChanged("short1"))

        assertEquals(null, vm.state.value.error)
    }
}
