package com.salvarez.miboveda.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.salvarez.miboveda.domain.model.PasswordStrength
import com.salvarez.miboveda.domain.model.RecoveryKey
import com.salvarez.miboveda.presentation.common.PasswordTextField
import com.salvarez.miboveda.presentation.common.RecoveryKeyPanel
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme

@Composable
fun OnboardingRoute(
    onVaultCreated: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.VaultCreated -> onVaultCreated()
            }
        }
    }
    OnboardingScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val recoveryKey = state.recoveryKey
        if (recoveryKey == null) {
            PasswordStep(state, onIntent)
        } else {
            RecoveryStep(recoveryKey, state.isRecoverySaved, onIntent)
        }
    }
}

@Composable
private fun PasswordStep(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.onboarding_description), style = MaterialTheme.typography.bodyMedium)

    PasswordTextField(
        value = state.password,
        onValueChange = { onIntent(OnboardingIntent.PasswordChanged(it)) },
        label = stringResource(R.string.master_password_label),
        isVisible = state.isPasswordVisible,
    )
    if (state.password.isNotEmpty()) {
        StrengthIndicator(state.strength)
    }
    PasswordTextField(
        value = state.confirmation,
        onValueChange = { onIntent(OnboardingIntent.ConfirmationChanged(it)) },
        label = stringResource(R.string.master_password_confirm_label),
        isVisible = state.isPasswordVisible,
        imeAction = ImeAction.Done,
        onImeAction = { onIntent(OnboardingIntent.Submit) },
    )
    TextButton(onClick = { onIntent(OnboardingIntent.TogglePasswordVisibility) }) {
        Text(stringResource(if (state.isPasswordVisible) R.string.hide_passwords else R.string.show_passwords))
    }
    state.error?.let {
        Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
    Button(
        onClick = { onIntent(OnboardingIntent.Submit) },
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            Text(stringResource(R.string.create_vault))
        }
    }
}

@Composable
private fun RecoveryStep(recoveryKey: RecoveryKey, isSaved: Boolean, onIntent: (OnboardingIntent) -> Unit) {
    Text(stringResource(R.string.recovery_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.recovery_description), style = MaterialTheme.typography.bodyMedium)
    RecoveryKeyPanel(recoveryKey = recoveryKey.value, onCopy = { onIntent(OnboardingIntent.CopyRecoveryKey) })
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = isSaved, role = Role.Checkbox, onValueChange = { onIntent(OnboardingIntent.ToggleRecoverySaved) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = isSaved, onCheckedChange = null)
        Text(stringResource(R.string.recovery_saved_confirmation), modifier = Modifier.padding(start = 12.dp))
    }
    Button(
        onClick = { onIntent(OnboardingIntent.ContinueAfterRecovery) },
        enabled = isSaved,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.continue_action)) }
}

@Composable
private fun StrengthIndicator(strength: PasswordStrength) {
    val (progress, labelRes) = when (strength) {
        PasswordStrength.WEAK -> 0.33f to R.string.strength_weak
        PasswordStrength.MEDIUM -> 0.66f to R.string.strength_medium
        PasswordStrength.STRONG -> 1f to R.string.strength_strong
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    MiBovedaTheme {
        OnboardingScreen(state = OnboardingState(password = "una contraseña larga 1"), onIntent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun RecoveryStepPreview() {
    MiBovedaTheme {
        OnboardingScreen(
            state = OnboardingState(recoveryKey = RecoveryKey("ABCD-EFGH-IJKL-MNOP-QRST-UVWX-YZ23-4567-ABCD-EFGH-IJKL-MNOP-QRST")),
            onIntent = {},
        )
    }
}
