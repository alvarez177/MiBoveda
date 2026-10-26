package com.salvarez.miboveda.presentation.unlock

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.common.rememberBiometricAuthenticator
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import com.salvarez.miboveda.presentation.theme.Motion
import com.salvarez.miboveda.presentation.ui.LockEmblem
import com.salvarez.miboveda.presentation.ui.PasswordTextField
import com.salvarez.miboveda.presentation.ui.VaultBackground
import com.salvarez.miboveda.presentation.ui.VaultButton
import com.salvarez.miboveda.presentation.ui.VaultOutlinedButton
import com.salvarez.miboveda.presentation.ui.VaultTextAction
import kotlinx.coroutines.delay

@Composable
fun UnlockRoute(
    onUnlocked: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: UnlockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authenticator = rememberBiometricAuthenticator()
    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val promptNegative = stringResource(R.string.biometric_prompt_negative)
    // The padlock opens first; the screen changes once the animation has played.
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                UnlockEffect.Unlocked -> opened = true
                is UnlockEffect.ShowBiometricPrompt -> authenticator.authenticate(
                    cipher = effect.cipher,
                    title = promptTitle,
                    negativeButtonText = promptNegative,
                    onSuccess = { viewModel.onIntent(UnlockIntent.BiometricApproved(it)) },
                    onFailure = { viewModel.onIntent(UnlockIntent.BiometricFailed(it)) },
                )
            }
        }
    }
    LaunchedEffect(opened) {
        if (opened) {
            delay(Motion.Long + 350L)
            onUnlocked()
        }
    }
    // Offer the fingerprint right away, once; the button stays available if the user cancels.
    var promptedAutomatically by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.biometricStatus) {
        if (state.biometricStatus == BiometricStatus.ENABLED && !promptedAutomatically) {
            promptedAutomatically = true
            viewModel.onIntent(UnlockIntent.BiometricClicked)
        }
    }
    UnlockScreen(state = state, onIntent = viewModel::onIntent, onForgotPassword = onForgotPassword, isOpen = opened)
}

@Composable
fun UnlockScreen(
    state: UnlockState,
    onIntent: (UnlockIntent) -> Unit,
    onForgotPassword: () -> Unit,
    isOpen: Boolean = false,
) {
    // Each new error shakes the padlock once.
    var shakeSignal by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.error) {
        if (state.error != null) shakeSignal++
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        LockEmblem(isOpen = isOpen, size = 176.dp, shakeSignal = shakeSignal, isError = state.error != null)
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.unlock_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.unlock_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        PasswordTextField(
            value = state.password,
            onValueChange = { onIntent(UnlockIntent.PasswordChanged(it)) },
            label = stringResource(R.string.master_password_label),
            isVisible = state.isPasswordVisible,
            imeAction = ImeAction.Done,
            onImeAction = { onIntent(UnlockIntent.Submit) },
            onToggleVisibility = { onIntent(UnlockIntent.TogglePasswordVisibility) },
            isError = state.error != null,
        )
        AnimatedVisibility(
            visible = state.error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
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
            text = stringResource(R.string.unlock_vault),
            onClick = { onIntent(UnlockIntent.Submit) },
            enabled = !state.isLoading && !isOpen,
            loading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(visible = state.biometricStatus == BiometricStatus.ENABLED) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Spacer(Modifier.height(12.dp))
                VaultOutlinedButton(
                    text = stringResource(R.string.unlock_with_biometrics),
                    onClick = { onIntent(UnlockIntent.BiometricClicked) },
                    enabled = !state.isLoading,
                    icon = Icons.Rounded.Fingerprint,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        VaultTextAction(stringResource(R.string.forgot_password), onClick = onForgotPassword)
    }
}

@Preview(showBackground = true)
@Composable
private fun UnlockScreenPreview() {
    MiBovedaTheme {
        VaultBackground {
            UnlockScreen(
                state = UnlockState(error = VaultError.WrongMasterPassword),
                onIntent = {},
                onForgotPassword = {},
            )
        }
    }
}
