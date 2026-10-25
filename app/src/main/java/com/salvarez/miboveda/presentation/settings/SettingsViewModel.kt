package com.salvarez.miboveda.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.usecase.DisableBiometric
import com.salvarez.miboveda.domain.usecase.EnableBiometric
import com.salvarez.miboveda.domain.usecase.ExportBackup
import com.salvarez.miboveda.domain.usecase.GenerateRecoveryKey
import com.salvarez.miboveda.domain.usecase.ObserveAutoLockTimeout
import com.salvarez.miboveda.domain.usecase.ObserveBiometricStatus
import com.salvarez.miboveda.domain.usecase.ObserveRecoveryKeyStatus
import com.salvarez.miboveda.domain.usecase.PrepareBiometricEnrollment
import com.salvarez.miboveda.domain.usecase.SetAutoLockTimeout
import com.salvarez.miboveda.presentation.common.SensitiveClipboard
import com.salvarez.miboveda.presentation.lock.ExternalActivityGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeBiometricStatus: ObserveBiometricStatus,
    observeAutoLockTimeout: ObserveAutoLockTimeout,
    observeRecoveryKeyStatus: ObserveRecoveryKeyStatus,
    private val generateRecoveryKey: GenerateRecoveryKey,
    private val clipboard: SensitiveClipboard,
    private val setAutoLockTimeout: SetAutoLockTimeout,
    private val prepareBiometricEnrollment: PrepareBiometricEnrollment,
    private val enableBiometric: EnableBiometric,
    private val disableBiometric: DisableBiometric,
    private val exportBackup: ExportBackup,
    private val externalActivityGuard: ExternalActivityGuard,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // Kept only between checking the password and the biometric approval, then cleared.
    private var pendingPassword: CharArray? = null

    // Where the backup will be written, between choosing the file and confirming the password.
    private var pendingBackupUri: String? = null

    init {
        observeBiometricStatus()
            .onEach { status -> _state.update { it.copy(biometricStatus = status) } }
            .launchIn(viewModelScope)
        observeAutoLockTimeout()
            .onEach { timeout -> _state.update { it.copy(autoLockTimeout = timeout) } }
            .launchIn(viewModelScope)
        observeRecoveryKeyStatus()
            .onEach { has -> _state.update { it.copy(hasRecoveryKey = has) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ToggleBiometric -> toggleBiometric(intent.enable)
            SettingsIntent.RequestRecoveryKey -> askForPassword(PasswordDialogPurpose.RECOVERY_KEY)
            SettingsIntent.RequestBackup -> requestBackup()
            is SettingsIntent.BackupLocationChosen -> backupLocationChosen(intent.uri)
            SettingsIntent.CopyRecoveryKey -> _state.value.newRecoveryKey?.let { clipboard.copy("recovery", it.value) }
            SettingsIntent.DismissRecoveryKey -> _state.update { it.copy(newRecoveryKey = null) }
            is SettingsIntent.SelectAutoLockTimeout -> viewModelScope.launch { setAutoLockTimeout(intent.timeout) }
            is SettingsIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null) }
            SettingsIntent.ConfirmPassword -> confirmPassword()
            SettingsIntent.DismissPasswordDialog -> {
                pendingBackupUri = null
                _state.update { it.copy(isPasswordDialogVisible = false, password = "") }
            }
            is SettingsIntent.BiometricApproved -> completeEnrollment(intent.cipher)
            is SettingsIntent.BiometricFailed -> {
                clearPendingPassword()
                _state.update {
                    it.copy(isBusy = false, error = if (intent.dismissedByUser) null else VaultError.BiometricUnavailable)
                }
            }
        }
    }

    private fun toggleBiometric(enable: Boolean) {
        if (_state.value.isBusy) return
        if (enable) askForPassword(PasswordDialogPurpose.BIOMETRIC) else viewModelScope.launch { disableBiometric() }
    }

    private fun askForPassword(purpose: PasswordDialogPurpose) {
        if (_state.value.isBusy) return
        _state.update {
            it.copy(isPasswordDialogVisible = true, passwordDialogPurpose = purpose, password = "", error = null)
        }
    }

    private fun confirmPassword() {
        val purpose = _state.value.passwordDialogPurpose
        val password = _state.value.password.toCharArray()
        _state.update { it.copy(isPasswordDialogVisible = false, password = "", isBusy = true, error = null) }
        when (purpose) {
            PasswordDialogPurpose.BIOMETRIC -> prepareBiometric(password)
            PasswordDialogPurpose.RECOVERY_KEY -> makeRecoveryKey(password)
            PasswordDialogPurpose.EXPORT_BACKUP -> makeBackup(password)
        }
    }

    private fun requestBackup() {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBackupSaved = false, error = null) }
        // The file picker is another app on screen: do not lock the vault while the user is choosing a file.
        externalActivityGuard.allowExternalActivity()
        _effects.trySend(SettingsEffect.PickBackupLocation("MiBoveda-${LocalDate.now()}.backup"))
    }

    private fun backupLocationChosen(uri: String?) {
        if (uri == null) return
        pendingBackupUri = uri
        askForPassword(PasswordDialogPurpose.EXPORT_BACKUP)
    }

    private fun makeBackup(password: CharArray) {
        val uri = pendingBackupUri
        pendingBackupUri = null
        if (uri == null) {
            password.fill('\u0000')
            _state.update { it.copy(isBusy = false) }
            return
        }
        viewModelScope.launch {
            val result = try {
                exportBackup(password, uri)
            } finally {
                password.fill('\u0000')
            }
            _state.update {
                when (result) {
                    is Outcome.Success -> it.copy(isBusy = false, isBackupSaved = true)
                    is Outcome.Failure -> it.copy(isBusy = false, error = result.error)
                }
            }
        }
    }

    private fun makeRecoveryKey(password: CharArray) {
        viewModelScope.launch {
            val result = try {
                generateRecoveryKey(password)
            } finally {
                password.fill('\u0000')
            }
            _state.update {
                when (result) {
                    is Outcome.Success -> it.copy(isBusy = false, newRecoveryKey = result.value)
                    is Outcome.Failure -> it.copy(isBusy = false, error = result.error)
                }
            }
        }
    }

    private fun prepareBiometric(password: CharArray) {
        viewModelScope.launch {
            when (val result = prepareBiometricEnrollment(password)) {
                is Outcome.Success -> {
                    pendingPassword = password
                    _effects.send(SettingsEffect.ShowBiometricPrompt(result.value))
                }
                is Outcome.Failure -> {
                    password.fill('\u0000')
                    _state.update { it.copy(isBusy = false, error = result.error) }
                }
            }
        }
    }

    private fun completeEnrollment(cipher: Cipher) {
        val password = pendingPassword ?: return
        viewModelScope.launch {
            val result = try {
                enableBiometric(cipher, password)
            } finally {
                clearPendingPassword()
            }
            _state.update { it.copy(isBusy = false, error = (result as? Outcome.Failure)?.error) }
        }
    }

    private fun clearPendingPassword() {
        pendingPassword?.fill('\u0000')
        pendingPassword = null
    }

    override fun onCleared() = clearPendingPassword()
}
