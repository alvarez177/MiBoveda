package com.salvarez.miboveda.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.presentation.common.BackButton
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme

@Composable
fun EntryDetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: EntryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val copiedMessage = stringResource(R.string.copied_to_clipboard)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EntryDetailEffect.Copied -> snackbarHostState.showSnackbar(copiedMessage)
                EntryDetailEffect.Deleted -> onBack()
            }
        }
    }
    EntryDetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onEdit = { state.entry?.let { onEdit(it.id) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    state: EntryDetailState,
    snackbarHostState: SnackbarHostState,
    onIntent: (EntryDetailIntent) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.entry?.title.orEmpty()) },
                navigationIcon = { BackButton(onBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val entry = state.entry
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                entry == null -> Text(
                    text = state.error?.message().orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> EntryContent(entry, state, onIntent, onEdit)
            }
        }
    }
    if (state.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(EntryDetailIntent.DismissDelete) },
            title = { Text(stringResource(R.string.delete_entry_title)) },
            text = { Text(stringResource(R.string.delete_entry_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(EntryDetailIntent.ConfirmDelete) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(EntryDetailIntent.DismissDelete) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun EntryContent(
    entry: PasswordEntry,
    state: EntryDetailState,
    onIntent: (EntryDetailIntent) -> Unit,
    onEdit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Field(stringResource(R.string.username_label), entry.username) {
            TextButton(onClick = { onIntent(EntryDetailIntent.CopyUsername) }) { Text(stringResource(R.string.copy)) }
        }
        Field(
            label = stringResource(R.string.password_label),
            value = if (state.isPasswordVisible) entry.password else HIDDEN_PASSWORD,
        ) {
            TextButton(onClick = { onIntent(EntryDetailIntent.TogglePasswordVisibility) }) {
                Text(stringResource(if (state.isPasswordVisible) R.string.hide else R.string.show))
            }
            TextButton(onClick = { onIntent(EntryDetailIntent.CopyPassword) }) { Text(stringResource(R.string.copy)) }
        }
        if (entry.url.isNotBlank()) Field(stringResource(R.string.url_label), entry.url)
        if (entry.notes.isNotBlank()) Field(stringResource(R.string.notes_label), entry.notes)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onEdit, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.edit)) }
            OutlinedButton(
                onClick = { onIntent(EntryDetailIntent.RequestDelete) },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.delete)) }
        }
    }
}

@Composable
private fun Field(label: String, value: String, actions: @Composable () -> Unit = {}) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            actions()
        }
    }
}

private const val HIDDEN_PASSWORD = "••••••••••"

@Preview(showBackground = true)
@Composable
private fun EntryDetailPreview() {
    MiBovedaTheme {
        EntryDetailScreen(
            state = EntryDetailState(
                entry = PasswordEntry(id = 1, title = "Banco", username = "ana", password = "s3cret", url = "https://banco.example"),
                isLoading = false,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onBack = {},
            onEdit = {},
        )
    }
}
