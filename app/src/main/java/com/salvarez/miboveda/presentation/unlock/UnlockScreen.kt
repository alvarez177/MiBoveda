package com.salvarez.miboveda.presentation.unlock

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.presentation.common.PasswordTextField
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme

@Composable
fun UnlockRoute(
    onUnlocked: () -> Unit,
    viewModel: UnlockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                UnlockEffect.Unlocked -> onUnlocked()
            }
        }
    }
    UnlockScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun UnlockScreen(
    state: UnlockState,
    onIntent: (UnlockIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.unlock_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.unlock_description), style = MaterialTheme.typography.bodyMedium)
        PasswordTextField(
            value = state.password,
            onValueChange = { onIntent(UnlockIntent.PasswordChanged(it)) },
            label = stringResource(R.string.master_password_label),
            isVisible = state.isPasswordVisible,
            imeAction = ImeAction.Done,
            onImeAction = { onIntent(UnlockIntent.Submit) },
        )
        TextButton(onClick = { onIntent(UnlockIntent.TogglePasswordVisibility) }) {
            Text(stringResource(if (state.isPasswordVisible) R.string.hide_password else R.string.show_password))
        }
        state.error?.let {
            Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = { onIntent(UnlockIntent.Submit) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text(stringResource(R.string.unlock_vault))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UnlockScreenPreview() {
    MiBovedaTheme {
        UnlockScreen(state = UnlockState(error = VaultError.WrongMasterPassword), onIntent = {})
    }
}
