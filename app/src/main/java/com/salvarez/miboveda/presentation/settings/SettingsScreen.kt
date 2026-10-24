package com.salvarez.miboveda.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.presentation.common.BackButton
import com.salvarez.miboveda.presentation.common.PasswordTextField
import com.salvarez.miboveda.presentation.common.labelRes
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.common.rememberBiometricAuthenticator
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authenticator = rememberBiometricAuthenticator()
    val promptTitle = stringResource(R.string.biometric_prompt_enable_title)
    val promptNegative = stringResource(R.string.cancel)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowBiometricPrompt -> authenticator.authenticate(
                    cipher = effect.cipher,
                    title = promptTitle,
                    negativeButtonText = promptNegative,
                    onSuccess = { viewModel.onIntent(SettingsIntent.BiometricApproved(it)) },
                    onFailure = { viewModel.onIntent(SettingsIntent.BiometricFailed(it)) },
                )
            }
        }
    }
    SettingsScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.biometric_setting_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(
                            if (state.biometricStatus == BiometricStatus.UNAVAILABLE) {
                                R.string.biometric_setting_unavailable
                            } else {
                                R.string.biometric_setting_description
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = state.biometricStatus == BiometricStatus.ENABLED,
                    onCheckedChange = { onIntent(SettingsIntent.ToggleBiometric(it)) },
                    enabled = state.biometricStatus != BiometricStatus.UNAVAILABLE && !state.isBusy,
                )
            }
            state.error?.let {
                Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AutoLockSection(state.autoLockTimeout) { onIntent(SettingsIntent.SelectAutoLockTimeout(it)) }
        }
    }
    if (state.isPasswordDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissPasswordDialog) },
            title = { Text(stringResource(R.string.biometric_password_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.biometric_password_dialog_message))
                    PasswordTextField(
                        value = state.password,
                        onValueChange = { onIntent(SettingsIntent.PasswordChanged(it)) },
                        label = stringResource(R.string.master_password_label),
                        isVisible = false,
                        imeAction = ImeAction.Done,
                        onImeAction = { onIntent(SettingsIntent.ConfirmPassword) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.ConfirmPassword) }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.DismissPasswordDialog) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun AutoLockSection(selected: AutoLockTimeout, onSelect: (AutoLockTimeout) -> Unit) {
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.auto_lock_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.auto_lock_description), style = MaterialTheme.typography.bodyMedium)
        AutoLockTimeout.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = option == selected, onClick = { onSelect(option) }, role = Role.RadioButton)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(stringResource(option.labelRes()), modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MiBovedaTheme {
        SettingsScreen(state = SettingsState(biometricStatus = BiometricStatus.AVAILABLE), onIntent = {}, onBack = {})
    }
}
