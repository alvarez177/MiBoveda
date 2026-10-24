package com.salvarez.miboveda.presentation.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.presentation.common.BackButton
import com.salvarez.miboveda.presentation.common.PasswordTextField
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme

@Composable
fun RecoveryRoute(
    onBack: () -> Unit,
    onRecovered: () -> Unit,
    viewModel: RecoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RecoveryEffect.Recovered -> onRecovered()
            }
        }
    }
    RecoveryScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen(
    state: RecoveryState,
    onIntent: (RecoveryIntent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recover_title)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.recover_description), style = MaterialTheme.typography.bodyMedium)
            PasswordTextField(
                value = state.recoveryKey,
                onValueChange = { onIntent(RecoveryIntent.RecoveryKeyChanged(it)) },
                label = stringResource(R.string.recovery_key_label),
                isVisible = state.isVisible,
            )
            PasswordTextField(
                value = state.password,
                onValueChange = { onIntent(RecoveryIntent.PasswordChanged(it)) },
                label = stringResource(R.string.new_master_password_label),
                isVisible = state.isVisible,
            )
            PasswordTextField(
                value = state.confirmation,
                onValueChange = { onIntent(RecoveryIntent.ConfirmationChanged(it)) },
                label = stringResource(R.string.new_master_password_confirm_label),
                isVisible = state.isVisible,
                imeAction = ImeAction.Done,
                onImeAction = { onIntent(RecoveryIntent.Submit) },
            )
            TextButton(onClick = { onIntent(RecoveryIntent.ToggleVisibility) }) {
                Text(stringResource(if (state.isVisible) R.string.hide_passwords else R.string.show_passwords))
            }
            state.error?.let {
                Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = { onIntent(RecoveryIntent.Submit) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.recover_vault))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecoveryScreenPreview() {
    MiBovedaTheme {
        RecoveryScreen(state = RecoveryState(error = VaultError.InvalidRecoveryKey), onIntent = {}, onBack = {})
    }
}
