package com.salvarez.miboveda.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.model.PasswordStrength
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_description), style = MaterialTheme.typography.bodyMedium)

        val transformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
        OutlinedTextField(
            value = state.password,
            onValueChange = { onIntent(OnboardingIntent.PasswordChanged(it)) },
            label = { Text(stringResource(R.string.master_password_label)) },
            singleLine = true,
            visualTransformation = transformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.password.isNotEmpty()) {
            StrengthIndicator(state.strength)
        }
        OutlinedTextField(
            value = state.confirmation,
            onValueChange = { onIntent(OnboardingIntent.ConfirmationChanged(it)) },
            label = { Text(stringResource(R.string.master_password_confirm_label)) },
            singleLine = true,
            visualTransformation = transformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
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
