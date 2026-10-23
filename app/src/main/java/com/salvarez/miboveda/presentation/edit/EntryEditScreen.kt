package com.salvarez.miboveda.presentation.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.salvarez.miboveda.R
import com.salvarez.miboveda.presentation.common.BackButton
import com.salvarez.miboveda.presentation.common.PasswordTextField
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme

@Composable
fun EntryEditRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EntryEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EntryEditEffect.Saved -> onSaved()
            }
        }
    }
    EntryEditScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditScreen(
    state: EntryEditState,
    onIntent: (EntryEditIntent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.new_entry_title else R.string.edit_entry_title)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Form(state, onIntent)
            }
        }
    }
}

@Composable
private fun Form(state: EntryEditState, onIntent: (EntryEditIntent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.title,
            onValueChange = { onIntent(EntryEditIntent.TitleChanged(it)) },
            label = { Text(stringResource(R.string.title_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.username,
            onValueChange = { onIntent(EntryEditIntent.UsernameChanged(it)) },
            label = { Text(stringResource(R.string.username_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        PasswordTextField(
            value = state.password,
            onValueChange = { onIntent(EntryEditIntent.PasswordChanged(it)) },
            label = stringResource(R.string.password_label),
            isVisible = state.isPasswordVisible,
        )
        TextButton(onClick = { onIntent(EntryEditIntent.TogglePasswordVisibility) }) {
            Text(stringResource(if (state.isPasswordVisible) R.string.hide_password else R.string.show_password))
        }
        OutlinedTextField(
            value = state.url,
            onValueChange = { onIntent(EntryEditIntent.UrlChanged(it)) },
            label = { Text(stringResource(R.string.url_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.notes,
            onValueChange = { onIntent(EntryEditIntent.NotesChanged(it)) },
            label = { Text(stringResource(R.string.notes_label)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = { onIntent(EntryEditIntent.Save) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryEditPreview() {
    MiBovedaTheme { EntryEditScreen(state = EntryEditState(title = "Banco"), onIntent = {}, onBack = {}) }
}
