package com.salvarez.miboveda.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.presentation.common.labelRes
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import com.salvarez.miboveda.presentation.theme.Motion
import com.salvarez.miboveda.presentation.ui.AnimatedCheck
import com.salvarez.miboveda.presentation.ui.PasswordTextField
import com.salvarez.miboveda.presentation.ui.RecoveryKeyPanel
import com.salvarez.miboveda.presentation.ui.VaultBackground
import com.salvarez.miboveda.presentation.ui.VaultCard
import com.salvarez.miboveda.presentation.ui.VaultOutlinedButton
import com.salvarez.miboveda.presentation.ui.VaultScaffold
import com.salvarez.miboveda.presentation.ui.VaultTopBar
import com.salvarez.miboveda.presentation.ui.cascadeEntrance
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.domain.model.ImportSummary
import com.salvarez.miboveda.presentation.common.rememberBiometricAuthenticator

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authenticator = rememberBiometricAuthenticator()
    val backupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> viewModel.onIntent(SettingsIntent.BackupLocationChosen(uri?.toString())) }
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> viewModel.onIntent(SettingsIntent.ImportFileChosen(uri?.toString())) }
    val promptTitle = stringResource(R.string.biometric_prompt_enable_title)
    val promptNegative = stringResource(R.string.cancel)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.PickBackupLocation -> backupPicker.launch(effect.suggestedName)
                SettingsEffect.PickBackupToImport -> importPicker.launch(arrayOf("*/*"))
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

@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    VaultScaffold(topBar = { VaultTopBar(title = stringResource(R.string.settings_title), onBack = onBack) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(Icons.Rounded.Fingerprint, stringResource(R.string.biometric_setting_title), Modifier.cascadeEntrance(0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            if (state.biometricStatus == BiometricStatus.UNAVAILABLE) {
                                R.string.biometric_setting_unavailable
                            } else {
                                R.string.biometric_setting_description
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.biometricStatus == BiometricStatus.ENABLED,
                        onCheckedChange = { onIntent(SettingsIntent.ToggleBiometric(it)) },
                        enabled = state.biometricStatus != BiometricStatus.UNAVAILABLE && !state.isBusy,
                    )
                }
                ErrorLine(state)
            }
            SectionCard(Icons.Rounded.Timer, stringResource(R.string.auto_lock_title), Modifier.cascadeEntrance(1)) {
                Text(
                    stringResource(R.string.auto_lock_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AutoLockTimeout.entries.forEach { option ->
                        TimeoutOption(
                            label = stringResource(option.labelRes()),
                            selected = option == state.autoLockTimeout,
                            onClick = { onIntent(SettingsIntent.SelectAutoLockTimeout(option)) },
                        )
                    }
                }
            }
            SectionCard(Icons.Rounded.Key, stringResource(R.string.recovery_setting_title), Modifier.cascadeEntrance(2)) {
                Text(
                    stringResource(if (state.hasRecoveryKey) R.string.recovery_setting_present else R.string.recovery_setting_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VaultOutlinedButton(
                    text = stringResource(if (state.hasRecoveryKey) R.string.generate_new_recovery_key else R.string.generate_recovery_key),
                    onClick = { onIntent(SettingsIntent.RequestRecoveryKey) },
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SectionCard(Icons.Rounded.Backup, stringResource(R.string.backup_title), Modifier.cascadeEntrance(3)) {
                Text(
                    stringResource(R.string.backup_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VaultOutlinedButton(
                    text = stringResource(R.string.backup_export),
                    onClick = { onIntent(SettingsIntent.RequestBackup) },
                    enabled = !state.isBusy,
                    icon = Icons.Rounded.FileUpload,
                    modifier = Modifier.fillMaxWidth(),
                )
                ResultLine(visible = state.isBackupSaved, text = stringResource(R.string.backup_saved))
                VaultOutlinedButton(
                    text = stringResource(R.string.backup_import),
                    onClick = { onIntent(SettingsIntent.RequestImport) },
                    enabled = !state.isBusy,
                    icon = Icons.Rounded.FileDownload,
                    modifier = Modifier.fillMaxWidth(),
                )
                ResultLine(
                    visible = state.importSummary != null,
                    text = state.importSummary?.let { stringResource(R.string.backup_import_summary, it.added, it.skipped) }.orEmpty(),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    state.newRecoveryKey?.let {
        RecoveryKeyDialog(
            recoveryKey = it.value,
            onCopy = { onIntent(SettingsIntent.CopyRecoveryKey) },
            onDismiss = { onIntent(SettingsIntent.DismissRecoveryKey) },
        )
    }
    if (state.isPasswordDialogVisible) {
        PasswordDialog(state, onIntent)
    }
}

@Composable
private fun SectionCard(icon: ImageVector, title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    VaultCard(modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun ErrorLine(state: SettingsState) {
    AnimatedVisibility(visible = state.error != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        state.error?.let {
            Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** A confirmation that draws its own check mark when it appears. */
@Composable
private fun ResultLine(visible: Boolean, text: String) {
    AnimatedVisibility(visible = visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnimatedCheck()
            Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** One choice of a list; the chosen one lights up and shows a check that pops in. */
@Composable
private fun TimeoutOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(
        if (selected) scheme.primary.copy(alpha = 0.14f) else Color.Transparent,
        tween(Motion.Medium),
        label = "option-background",
    )
    val border by animateColorAsState(if (selected) scheme.primary else scheme.outlineVariant, tween(Motion.Medium), label = "option-border")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, border, MaterialTheme.shapes.medium)
            .background(background)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        AnimatedVisibility(visible = selected, enter = scaleIn(tween(Motion.Medium, easing = Motion.EmphasizedDecelerate)) + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = scheme.primary)
        }
    }
}

@Composable
private fun RecoveryKeyDialog(recoveryKey: String, onCopy: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.recovery_key_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.recovery_description), style = MaterialTheme.typography.bodyMedium)
                RecoveryKeyPanel(recoveryKey = recoveryKey, revealed = true, onReveal = {}, onCopy = onCopy)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recovery_key_saved)) } },
    )
}

@Composable
private fun PasswordDialog(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { onIntent(SettingsIntent.DismissPasswordDialog) },
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.biometric_password_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        when (state.passwordDialogPurpose) {
                            PasswordDialogPurpose.RECOVERY_KEY -> R.string.recovery_password_dialog_message
                            PasswordDialogPurpose.EXPORT_BACKUP -> R.string.backup_password_dialog_message
                            PasswordDialogPurpose.IMPORT_BACKUP -> R.string.backup_import_password_dialog_message
                            PasswordDialogPurpose.BIOMETRIC -> R.string.biometric_password_dialog_message
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                PasswordTextField(
                    value = state.password,
                    onValueChange = { onIntent(SettingsIntent.PasswordChanged(it)) },
                    label = stringResource(R.string.master_password_label),
                    isVisible = visible,
                    imeAction = ImeAction.Done,
                    onImeAction = { onIntent(SettingsIntent.ConfirmPassword) },
                    onToggleVisibility = { visible = !visible },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onIntent(SettingsIntent.ConfirmPassword) }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(SettingsIntent.DismissPasswordDialog) }) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MiBovedaTheme {
        VaultBackground {
            SettingsScreen(state = SettingsState(biometricStatus = BiometricStatus.AVAILABLE), onIntent = {}, onBack = {})
        }
    }
}
