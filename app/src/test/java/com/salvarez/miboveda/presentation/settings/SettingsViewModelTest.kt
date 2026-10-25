package com.salvarez.miboveda.presentation.settings

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.domain.usecase.DisableBiometric
import com.salvarez.miboveda.domain.usecase.EnableBiometric
import com.salvarez.miboveda.domain.usecase.ExportBackup
import com.salvarez.miboveda.domain.usecase.FakeBackupRepository
import com.salvarez.miboveda.domain.usecase.FakeEntryRepository
import com.salvarez.miboveda.domain.usecase.ImportBackup
import com.salvarez.miboveda.domain.usecase.FakeBiometricRepository
import com.salvarez.miboveda.domain.usecase.FakeSettingsRepository
import com.salvarez.miboveda.domain.usecase.FakeVaultRepository
import com.salvarez.miboveda.domain.usecase.GenerateRecoveryKey
import com.salvarez.miboveda.domain.usecase.ObserveRecoveryKeyStatus
import com.salvarez.miboveda.presentation.common.FakeSensitiveClipboard
import com.salvarez.miboveda.presentation.lock.ExternalActivityGuard
import com.salvarez.miboveda.domain.usecase.ObserveAutoLockTimeout
import com.salvarez.miboveda.domain.usecase.ObserveBiometricStatus
import com.salvarez.miboveda.domain.usecase.PrepareBiometricEnrollment
import com.salvarez.miboveda.domain.usecase.SetAutoLockTimeout
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
    private val settings = FakeSettingsRepository()
    private val vault = FakeVaultRepository()
    private val backup = FakeBackupRepository()
    private val entries = FakeEntryRepository()
    private var guardCalls = 0
    private val guard = ExternalActivityGuard { guardCalls++ }
    private val clipboard = FakeSensitiveClipboard()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SettingsViewModel(
        ObserveBiometricStatus(biometrics),
        ObserveAutoLockTimeout(settings),
        ObserveRecoveryKeyStatus(vault),
        GenerateRecoveryKey(vault),
        clipboard,
        SetAutoLockTimeout(settings),
        PrepareBiometricEnrollment(biometrics),
        EnableBiometric(biometrics),
        DisableBiometric(biometrics),
        ExportBackup(backup),
        ImportBackup(backup, entries),
        guard,
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
    fun `starts with the default timeout and stores the one the user picks`() = runTest {
        val vm = viewModel()
        assertEquals(AutoLockTimeout.IMMEDIATE, vm.state.value.autoLockTimeout)

        vm.onIntent(SettingsIntent.SelectAutoLockTimeout(AutoLockTimeout.ONE_MINUTE))

        assertEquals(AutoLockTimeout.ONE_MINUTE, vm.state.value.autoLockTimeout)
        assertEquals(AutoLockTimeout.ONE_MINUTE, settings.autoLockTimeout.first())
    }

    @Test
    fun `generating a recovery key asks for the password and shows the key once`() = runTest {
        val vm = viewModel()
        assertFalse(vm.state.value.hasRecoveryKey)

        vm.onIntent(SettingsIntent.RequestRecoveryKey)
        assertTrue(vm.state.value.isPasswordDialogVisible)
        assertEquals(PasswordDialogPurpose.RECOVERY_KEY, vm.state.value.passwordDialogPurpose)

        vm.onIntent(SettingsIntent.PasswordChanged("my master password"))
        vm.onIntent(SettingsIntent.ConfirmPassword)

        assertEquals(FakeVaultRepository.SAMPLE_RECOVERY_KEY, vm.state.value.newRecoveryKey)
        assertEquals(listOf("my master password"), vault.generatePasswords)
        assertTrue(vm.state.value.hasRecoveryKey)
        assertFalse(vm.state.value.isBusy)

        vm.onIntent(SettingsIntent.CopyRecoveryKey)
        assertEquals(listOf("recovery" to FakeVaultRepository.SAMPLE_RECOVERY_KEY.value), clipboard.copies)

        vm.onIntent(SettingsIntent.DismissRecoveryKey)
        assertEquals(null, vm.state.value.newRecoveryKey)
    }

    @Test
    fun `a wrong password does not generate a recovery key`() = runTest {
        val failing = FakeVaultRepository(generateResult = Outcome.Failure(VaultError.WrongMasterPassword))
        val vm = SettingsViewModel(
            ObserveBiometricStatus(biometrics),
            ObserveAutoLockTimeout(settings),
            ObserveRecoveryKeyStatus(failing),
            GenerateRecoveryKey(failing),
            clipboard,
            SetAutoLockTimeout(settings),
            PrepareBiometricEnrollment(biometrics),
            EnableBiometric(biometrics),
            DisableBiometric(biometrics),
            ExportBackup(backup),
            ImportBackup(backup, entries),
            guard,
        )
        vm.onIntent(SettingsIntent.RequestRecoveryKey)
        vm.onIntent(SettingsIntent.PasswordChanged("wrong"))
        vm.onIntent(SettingsIntent.ConfirmPassword)

        assertEquals(VaultError.WrongMasterPassword, vm.state.value.error)
        assertEquals(null, vm.state.value.newRecoveryKey)
    }

    @Test
    fun `asking for a backup first opens the file picker, without any password dialog`() = runTest {
        val vm = viewModel()
        vm.onIntent(SettingsIntent.RequestBackup)

        val effect = vm.effects.first() as SettingsEffect.PickBackupLocation
        assertTrue(effect.suggestedName.startsWith("MiBoveda-") && effect.suggestedName.endsWith(".backup"))
        assertFalse(vm.state.value.isPasswordDialogVisible)
        assertEquals(1, guardCalls)
    }

    @Test
    fun `closing the file picker without a file does nothing`() = runTest {
        val vm = viewModel()
        vm.onIntent(SettingsIntent.BackupLocationChosen(null))

        assertFalse(vm.state.value.isPasswordDialogVisible)
        assertTrue(backup.exports.isEmpty())
    }

    @Test
    fun `a chosen file plus the password exports the backup`() = runTest {
        val vm = viewModel()
        vm.onIntent(SettingsIntent.BackupLocationChosen("content://files/backup"))
        assertTrue(vm.state.value.isPasswordDialogVisible)
        assertEquals(PasswordDialogPurpose.EXPORT_BACKUP, vm.state.value.passwordDialogPurpose)

        vm.onIntent(SettingsIntent.PasswordChanged("my master password"))
        vm.onIntent(SettingsIntent.ConfirmPassword)

        assertEquals(listOf("my master password" to "content://files/backup"), backup.exports)
        assertTrue(vm.state.value.isBackupSaved)
        assertFalse(vm.state.value.isBusy)
        assertEquals("", vm.state.value.password)
    }

    @Test
    fun `a failed backup shows the error and is not reported as saved`() = runTest {
        backup.result = Outcome.Failure(VaultError.WrongMasterPassword)
        val vm = viewModel()
        vm.onIntent(SettingsIntent.BackupLocationChosen("content://files/backup"))
        vm.onIntent(SettingsIntent.PasswordChanged("wrong"))
        vm.onIntent(SettingsIntent.ConfirmPassword)

        assertEquals(VaultError.WrongMasterPassword, vm.state.value.error)
        assertFalse(vm.state.value.isBackupSaved)
        assertFalse(vm.state.value.isBusy)
    }

    @Test
    fun `asking to import opens the file picker and gives the system screen a grace period`() = runTest {
        val vm = viewModel()
        vm.onIntent(SettingsIntent.RequestImport)

        assertEquals(SettingsEffect.PickBackupToImport, vm.effects.first())
        assertEquals(1, guardCalls)
        assertFalse(vm.state.value.isPasswordDialogVisible)
    }

    @Test
    fun `closing the import picker without a file does nothing`() = runTest {
        val vm = viewModel()
        vm.onIntent(SettingsIntent.ImportFileChosen(null))

        assertFalse(vm.state.value.isPasswordDialogVisible)
        assertTrue(backup.reads.isEmpty())
    }

    @Test
    fun `a chosen file plus the password imports it and shows the summary`() = runTest {
        backup.readResult = Outcome.Success(listOf(com.salvarez.miboveda.domain.model.PasswordEntry(title = "Bank", username = "ana", password = "x")))
        val vm = viewModel()
        vm.onIntent(SettingsIntent.ImportFileChosen("content://files/backup"))
        assertEquals(PasswordDialogPurpose.IMPORT_BACKUP, vm.state.value.passwordDialogPurpose)

        vm.onIntent(SettingsIntent.PasswordChanged("the old master password"))
        vm.onIntent(SettingsIntent.ConfirmPassword)

        assertEquals(listOf("the old master password" to "content://files/backup"), backup.reads)
        assertEquals(com.salvarez.miboveda.domain.model.ImportSummary(added = 1, skipped = 0), vm.state.value.importSummary)
        assertFalse(vm.state.value.isBusy)
        assertEquals("", vm.state.value.password)
    }

    @Test
    fun `a backup that cannot be opened shows the error and no summary`() = runTest {
        backup.readResult = Outcome.Failure(VaultError.CorruptBackup)
        val vm = viewModel()
        vm.onIntent(SettingsIntent.ImportFileChosen("content://files/backup"))
        vm.onIntent(SettingsIntent.PasswordChanged("pw"))
        vm.onIntent(SettingsIntent.ConfirmPassword)

        assertEquals(VaultError.CorruptBackup, vm.state.value.error)
        assertEquals(null, vm.state.value.importSummary)
    }

    @Test
    fun `password intents do not leak the password`() {
        assertFalse(SettingsIntent.PasswordChanged("s3cret-value").toString().contains("s3cret-value"))
    }

    @Test
    fun `state toString does not leak the password or the recovery key`() {
        val state = SettingsState(password = "s3cret-value", newRecoveryKey = FakeVaultRepository.SAMPLE_RECOVERY_KEY)
        assertFalse(state.toString().contains(FakeVaultRepository.SAMPLE_RECOVERY_KEY.value))
        assertFalse(state.toString().contains("s3cret-value"))
    }

    @Test
    fun `state toString does not leak the password`() {
        assertFalse(SettingsState(password = "s3cret-value").toString().contains("s3cret-value"))
    }
}
