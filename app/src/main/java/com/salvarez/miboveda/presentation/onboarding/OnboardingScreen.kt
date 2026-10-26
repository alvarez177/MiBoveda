package com.salvarez.miboveda.presentation.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.model.RecoveryKey
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import com.salvarez.miboveda.presentation.theme.Motion
import com.salvarez.miboveda.presentation.ui.AnimatedCheck
import com.salvarez.miboveda.presentation.ui.LockEmblem
import com.salvarez.miboveda.presentation.ui.PasswordTextField
import com.salvarez.miboveda.presentation.ui.RecoveryKeyPanel
import com.salvarez.miboveda.presentation.ui.StrengthMeter
import com.salvarez.miboveda.presentation.ui.VaultBackground
import com.salvarez.miboveda.presentation.ui.VaultButton
import com.salvarez.miboveda.presentation.ui.VaultTextAction

@Composable
fun OnboardingRoute(
    onVaultCreated: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> viewModel.onIntent(OnboardingIntent.BackupFileChosen(uri?.toString())) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.VaultCreated -> onVaultCreated()
                OnboardingEffect.PickBackupFile -> backupPicker.launch(arrayOf("*/*"))
            }
        }
    }
    OnboardingScreen(state = state, onIntent = viewModel::onIntent)
}

private enum class Step { Password, Restore, Recovery }

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
) {
    val step = when {
        state.recoveryKey != null -> Step.Recovery
        state.restoreFileUri != null -> Step.Restore
        else -> Step.Password
    }
    // The steps slide sideways into each other, like pages of the same book.
    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            val spec = tween<androidx.compose.ui.unit.IntOffset>(Motion.Long, easing = Motion.Emphasized)
            (slideInHorizontally(spec) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(Motion.Medium))) togetherWith
                (slideOutHorizontally(spec) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(Motion.Short)))
        },
        label = "onboarding-step",
    ) { current ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (current) {
                Step.Password -> PasswordStep(state, onIntent)
                Step.Restore -> RestoreStep(state, onIntent)
                Step.Recovery -> state.recoveryKey?.let { RecoveryStep(it, state, onIntent) }
            }
        }
    }
}

@Composable
private fun Header(title: String, description: String, isOpen: Boolean = false) {
    Spacer(Modifier.height(12.dp))
    LockEmblem(isOpen = isOpen, size = 132.dp)
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(
        description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun ErrorLine(state: OnboardingState) {
    AnimatedVisibility(visible = state.error != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        state.error?.let {
            Text(
                it.message(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp),
            )
        }
    }
}

@Composable
private fun PasswordStep(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    Header(stringResource(R.string.onboarding_title), stringResource(R.string.onboarding_description))
    PasswordTextField(
        value = state.password,
        onValueChange = { onIntent(OnboardingIntent.PasswordChanged(it)) },
        label = stringResource(R.string.master_password_label),
        isVisible = state.isPasswordVisible,
        onToggleVisibility = { onIntent(OnboardingIntent.TogglePasswordVisibility) },
    )
    AnimatedVisibility(visible = state.password.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        StrengthMeter(state.strength, Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp))
    }
    Spacer(Modifier.height(14.dp))
    val matches = state.confirmation.isNotEmpty() && state.confirmation == state.password
    PasswordTextField(
        value = state.confirmation,
        onValueChange = { onIntent(OnboardingIntent.ConfirmationChanged(it)) },
        label = stringResource(R.string.master_password_confirm_label),
        isVisible = state.isPasswordVisible,
        imeAction = ImeAction.Done,
        onImeAction = { onIntent(OnboardingIntent.Submit) },
        extraTrailing = {
            AnimatedVisibility(visible = matches, enter = scaleIn(tween(Motion.Medium)) + fadeIn(), exit = fadeOut()) {
                AnimatedCheck(Modifier.padding(end = 4.dp))
            }
        },
    )
    ErrorLine(state)
    Spacer(Modifier.height(24.dp))
    VaultButton(
        text = stringResource(R.string.create_vault),
        onClick = { onIntent(OnboardingIntent.Submit) },
        enabled = !state.isLoading,
        loading = state.isLoading,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    VaultTextAction(stringResource(R.string.restore_link), onClick = { onIntent(OnboardingIntent.RestoreClicked) })
}

@Composable
private fun RestoreStep(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    Header(stringResource(R.string.restore_title), stringResource(R.string.restore_description))
    PasswordTextField(
        value = state.password,
        onValueChange = { onIntent(OnboardingIntent.PasswordChanged(it)) },
        label = stringResource(R.string.restore_password_label),
        isVisible = state.isPasswordVisible,
        imeAction = ImeAction.Done,
        onImeAction = { onIntent(OnboardingIntent.Submit) },
        onToggleVisibility = { onIntent(OnboardingIntent.TogglePasswordVisibility) },
        isError = state.error != null,
    )
    ErrorLine(state)
    Spacer(Modifier.height(24.dp))
    VaultButton(
        text = stringResource(R.string.restore_vault),
        onClick = { onIntent(OnboardingIntent.Submit) },
        enabled = !state.isLoading,
        loading = state.isLoading,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    VaultTextAction(stringResource(R.string.cancel), onClick = { onIntent(OnboardingIntent.CancelRestore) })
}

@Composable
private fun RecoveryStep(recoveryKey: RecoveryKey, state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    // The padlock opens: the vault exists now.
    Header(stringResource(R.string.recovery_title), stringResource(R.string.recovery_description), isOpen = true)
    RecoveryKeyPanel(
        recoveryKey = recoveryKey.value,
        revealed = state.isRecoveryRevealed,
        onReveal = { onIntent(OnboardingIntent.RevealRecoveryKey) },
        onCopy = { onIntent(OnboardingIntent.CopyRecoveryKey) },
    )
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = state.isRecoverySaved, role = Role.Checkbox, onValueChange = { onIntent(OnboardingIntent.ToggleRecoverySaved) })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = state.isRecoverySaved, onCheckedChange = null)
        Text(stringResource(R.string.recovery_saved_confirmation), style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(16.dp))
    VaultButton(
        text = stringResource(R.string.continue_action),
        onClick = { onIntent(OnboardingIntent.ContinueAfterRecovery) },
        enabled = state.isRecoverySaved,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    MiBovedaTheme {
        VaultBackground {
            OnboardingScreen(state = OnboardingState(password = "una contraseña larga 1"), onIntent = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecoveryStepPreview() {
    MiBovedaTheme {
        VaultBackground {
            OnboardingScreen(
                state = OnboardingState(
                    recoveryKey = RecoveryKey("ABCD-EFGH-IJKL-MNOP-QRST-UVWX-YZ23-4567-ABCD-EFGH-IJKL-MNOP-QRST"),
                    isRecoveryRevealed = true,
                ),
                onIntent = {},
            )
        }
    }
}
