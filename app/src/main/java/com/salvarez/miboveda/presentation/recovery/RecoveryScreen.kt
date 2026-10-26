package com.salvarez.miboveda.presentation.recovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import com.salvarez.miboveda.presentation.ui.LockEmblem
import com.salvarez.miboveda.presentation.ui.PasswordTextField
import com.salvarez.miboveda.presentation.ui.VaultBackground
import com.salvarez.miboveda.presentation.ui.VaultButton
import com.salvarez.miboveda.presentation.ui.VaultScaffold
import com.salvarez.miboveda.presentation.ui.VaultTopBar
import com.salvarez.miboveda.presentation.ui.cascadeEntrance

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

@Composable
fun RecoveryScreen(
    state: RecoveryState,
    onIntent: (RecoveryIntent) -> Unit,
    onBack: () -> Unit,
) {
    VaultScaffold(topBar = { VaultTopBar(title = stringResource(R.string.recover_title), onBack = onBack) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LockEmblem(isOpen = false, size = 112.dp, isError = state.error != null)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.recover_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PasswordTextField(
                    value = state.recoveryKey,
                    onValueChange = { onIntent(RecoveryIntent.RecoveryKeyChanged(it)) },
                    label = stringResource(R.string.recovery_key_label),
                    isVisible = state.isVisible,
                    leadingIcon = Icons.Rounded.Key,
                    onToggleVisibility = { onIntent(RecoveryIntent.ToggleVisibility) },
                    isError = state.error == VaultError.InvalidRecoveryKey,
                    modifier = Modifier.cascadeEntrance(0),
                )
                PasswordTextField(
                    value = state.password,
                    onValueChange = { onIntent(RecoveryIntent.PasswordChanged(it)) },
                    label = stringResource(R.string.new_master_password_label),
                    isVisible = state.isVisible,
                    modifier = Modifier.cascadeEntrance(1),
                )
                PasswordTextField(
                    value = state.confirmation,
                    onValueChange = { onIntent(RecoveryIntent.ConfirmationChanged(it)) },
                    label = stringResource(R.string.new_master_password_confirm_label),
                    isVisible = state.isVisible,
                    imeAction = ImeAction.Done,
                    onImeAction = { onIntent(RecoveryIntent.Submit) },
                    modifier = Modifier.cascadeEntrance(2),
                )
            }
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
            Spacer(Modifier.height(24.dp))
            VaultButton(
                text = stringResource(R.string.recover_vault),
                onClick = { onIntent(RecoveryIntent.Submit) },
                enabled = !state.isLoading,
                loading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecoveryScreenPreview() {
    MiBovedaTheme {
        VaultBackground {
            RecoveryScreen(state = RecoveryState(error = VaultError.InvalidRecoveryKey), onIntent = {}, onBack = {})
        }
    }
}
