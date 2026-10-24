package com.salvarez.miboveda.presentation.settings

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.domain.usecase.DisableBiometric
import com.salvarez.miboveda.domain.usecase.EnableBiometric
import com.salvarez.miboveda.domain.usecase.FakeBiometricRepository
import com.salvarez.miboveda.domain.usecase.ObserveBiometricStatus
import com.salvarez.miboveda.domain.usecase.PrepareBiometricEnrollment
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val biometrics = FakeBiometricRepository(BiometricStatus.AVAILABLE)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SettingsViewModel(
        ObserveBiometricStatus(biometrics),
        PrepareBiometricEnrollment(biometrics),
        EnableBiometric(biometrics),
        DisableBiometric(biometrics),
    )

    private fun SettingsViewModel.askToEnableWith(password: String) {
        onIntent(SettingsIntent.ToggleBiometric(enable = true))
        onIntent(SettingsIntent.PasswordChanged(password))
        onIntent(SettingsIntent.ConfirmPassword)
    }

    @Test
    fun `turning it on asks for the master password first`() = runTest {
        val vm = viewModel()
        vm.onIntent(SettingsIntent.ToggleBiometric(enable = true))

        assertTrue(vm.state.value.isPasswordDialogVisible)
    }

    @Test
    fun `a wrong master password shows the error and never reaches the prompt`() = runTest {
        biometrics.prepareEnrollmentResult = Outcome.Failure(VaultError.WrongMasterPassword)
        val vm = viewModel()
        vm.askToEnableWith("wrong")

        assertEquals(VaultError.WrongMasterPassword, vm.state.value.error)
        assertFalse(vm.state.value.isBusy)
        assertEquals("", vm.state.value.password)
    }

    @Test
    fun `a correct password shows the prompt and an approval enables biometrics`() = runTest {
        val vm = viewModel()
        vm.askToEnableWith("my master password")

        assertEquals(SettingsEffect.ShowBiometricPrompt(biometrics.cipher), vm.effects.first())
        vm.onIntent(SettingsIntent.BiometricApproved(biometrics.cipher))

        assertEquals(listOf("my master password"), biometrics.enrollmentPasswords)
        assertEquals(BiometricStatus.ENABLED, vm.state.value.biometricStatus)
        assertFalse(vm.state.value.isBusy)
        assertEquals(null, vm.state.value.error)
    }

    @Test
    fun `dismissing the prompt stops quietly and forgets the password`() = runTest {
        val vm = viewModel()
        vm.askToEnableWith("my master password")
        vm.onIntent(SettingsIntent.BiometricFailed(dismissedByUser = true))

        assertFalse(vm.state.value.isBusy)
        assertEquals(null, vm.state.value.error)

        // Without a pending password an approval can no longer complete the enrollment.
        vm.onIntent(SettingsIntent.BiometricApproved(biometrics.cipher))
        assertTrue(biometrics.enrollmentPasswords.isEmpty())
    }

    @Test
    fun `turning it off disables biometrics`() = runTest {
        val vm = viewModel()
        vm.onIntent(SettingsIntent.ToggleBiometric(enable = false))

        assertEquals(1, biometrics.disableCalls)
    }

    @Test
    fun `state toString does not leak the password`() {
        assertFalse(SettingsState(password = "s3cret-value").toString().contains("s3cret-value"))
    }
}
