package com.salvarez.miboveda.presentation.onboarding

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.usecase.CreateVault
import com.salvarez.miboveda.domain.usecase.FakeVaultRepository
import com.salvarez.miboveda.presentation.common.FakeSensitiveClipboard
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val clipboard = FakeSensitiveClipboard()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repository: FakeVaultRepository = FakeVaultRepository()) =
        OnboardingViewModel(CreateVault(repository), clipboard)

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
        assertNull(vm.state.value.recoveryKey)
    }

    @Test
    fun `creating the vault shows the recovery key and drops the passwords`() = runTest {
        val vm = viewModel()
        vm.fill("a long enough password", "a long enough password")
        vm.onIntent(OnboardingIntent.Submit)

        assertEquals(FakeVaultRepository.SAMPLE_RECOVERY_KEY, vm.state.value.recoveryKey)
        assertEquals("", vm.state.value.password)
        assertEquals("", vm.state.value.confirmation)
    }

    @Test
    fun `the user cannot leave until confirming the key was saved`() = runTest {
        val vm = viewModel()
        vm.fill("a long enough password", "a long enough password")
        vm.onIntent(OnboardingIntent.Submit)

        vm.onIntent(OnboardingIntent.ContinueAfterRecovery)
        assertNotNull(vm.state.value.recoveryKey)

        vm.onIntent(OnboardingIntent.ToggleRecoverySaved)
        vm.onIntent(OnboardingIntent.ContinueAfterRecovery)

        assertEquals(OnboardingEffect.VaultCreated, vm.effects.first())
        assertNull(vm.state.value.recoveryKey)
    }

    @Test
    fun `copying sends the recovery key to the sensitive clipboard`() = runTest {
        val vm = viewModel()
        vm.fill("a long enough password", "a long enough password")
        vm.onIntent(OnboardingIntent.Submit)
        vm.onIntent(OnboardingIntent.CopyRecoveryKey)

        assertEquals(listOf("recovery" to FakeVaultRepository.SAMPLE_RECOVERY_KEY.value), clipboard.copies)
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

        assertNull(vm.state.value.error)
    }

    @Test
    fun `state and intents do not leak the recovery key or passwords`() = runTest {
        val vm = viewModel()
        vm.fill("a long enough password", "a long enough password")
        vm.onIntent(OnboardingIntent.Submit)

        assertFalse(vm.state.value.toString().contains(FakeVaultRepository.SAMPLE_RECOVERY_KEY.value))
        assertFalse(OnboardingIntent.PasswordChanged("s3cret-value").toString().contains("s3cret-value"))
        assertFalse(FakeVaultRepository.SAMPLE_RECOVERY_KEY.toString().contains("ABCD"))
    }
}
