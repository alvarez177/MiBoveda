package com.salvarez.miboveda.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme

@Composable
fun EntryListRoute(
    onAdd: () -> Unit,
    onEntryClick: (Long) -> Unit,
    viewModel: EntryListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EntryListScreen(state = state, onAdd = onAdd, onEntryClick = onEntryClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    state: EntryListState,
    onAdd: () -> Unit,
    onEntryClick: (Long) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.entries_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd) { Text(stringResource(R.string.add_entry)) }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null -> Text(
                    text = state.error.message(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )
                state.entries.isEmpty() -> Text(
                    text = stringResource(R.string.entries_empty),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.entries, key = { it.id }) { entry ->
                        ListItem(
                            headlineContent = { Text(entry.title) },
                            supportingContent = if (entry.username.isNotBlank()) {
                                { Text(entry.username) }
                            } else {
                                null
                            },
                            modifier = Modifier.clickable { onEntryClick(entry.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryListScreenPreview() {
    MiBovedaTheme {
        EntryListScreen(
            state = EntryListState(
                entries = listOf(PasswordEntry(id = 1, title = "Banco", username = "ana", password = "x")),
                isLoading = false,
            ),
            onAdd = {},
            onEntryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryListErrorPreview() {
    MiBovedaTheme {
        EntryListScreen(
            state = EntryListState(isLoading = false, error = VaultError.VaultLocked),
            onAdd = {},
            onEntryClick = {},
        )
    }
}
