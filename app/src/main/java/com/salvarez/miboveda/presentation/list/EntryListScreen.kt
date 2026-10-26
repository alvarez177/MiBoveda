package com.salvarez.miboveda.presentation.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import com.salvarez.miboveda.presentation.ui.Avatar
import com.salvarez.miboveda.presentation.ui.LockEmblem
import com.salvarez.miboveda.presentation.ui.VaultBackground
import com.salvarez.miboveda.presentation.ui.VaultCard
import com.salvarez.miboveda.presentation.ui.VaultLargeTopBar
import com.salvarez.miboveda.presentation.ui.VaultScaffold
import com.salvarez.miboveda.presentation.ui.cascadeEntrance

@Composable
fun EntryListRoute(
    onAdd: () -> Unit,
    onEntryClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: EntryListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EntryListScreen(state = state, onAdd = onAdd, onEntryClick = onEntryClick, onOpenSettings = onOpenSettings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    state: EntryListState,
    onAdd: () -> Unit,
    onEntryClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    // The button shrinks to just the plus sign while the user scrolls down, and grows back when going up.
    val fabExpanded = listState.firstVisibleItemIndex == 0 || listState.lastScrolledBackward
    VaultScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            VaultLargeTopBar(
                title = stringResource(R.string.entries_title),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_action))
                    }
                },
            )
        },
        floatingActionButton = {
            val addLabel = stringResource(R.string.add_entry)
            ExtendedFloatingActionButton(
                onClick = onAdd,
                expanded = fabExpanded,
                // Without this a screen reader finds a button with no name, because the text hides while collapsed.
                modifier = Modifier.semantics { contentDescription = addLabel },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_entry)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    state.error.message(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center,
                )
            }
            state.entries.isEmpty() -> EmptyVault(Modifier.padding(innerPadding))
            else -> EntryList(state.entries, listState, innerPadding, onEntryClick)
        }
    }
}

@Composable
private fun EntryList(
    entries: List<PasswordEntry>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    innerPadding: PaddingValues,
    onEntryClick: (Long) -> Unit,
) {
    // Entries that already made their entrance do not repeat it when scrolled back into view.
    val shown = remember { mutableSetOf<Long>() }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = innerPadding.calculateTopPadding() + 4.dp,
            bottom = innerPadding.calculateBottomPadding() + 104.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
            val firstTime = entry.id !in shown
            LaunchedEffect(entry.id) { shown += entry.id }
            EntryCard(entry, onClick = { onEntryClick(entry.id) }, modifier = Modifier.cascadeEntrance(index, enabled = firstTime))
        }
    }
}

@Composable
private fun EntryCard(entry: PasswordEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    VaultCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Avatar(entry.title)
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (entry.username.isNotBlank()) {
                    Text(
                        entry.username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The vault, empty and waiting: the padlock floats gently above the invitation to add the first password. */
@Composable
private fun EmptyVault(modifier: Modifier = Modifier) {
    val float by rememberInfiniteTransition(label = "float").animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "float-offset",
    )
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(500)) }
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp).graphicsLayer { alpha = entrance.value },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LockEmblem(isOpen = false, size = 156.dp, modifier = Modifier.graphicsLayer { translationY = float.dp.toPx() })
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.entries_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryListScreenPreview() {
    MiBovedaTheme {
        VaultBackground {
            EntryListScreen(
                state = EntryListState(
                    entries = listOf(PasswordEntry(id = 1, title = "Banco", username = "ana", password = "x")),
                    isLoading = false,
                ),
                onAdd = {},
                onEntryClick = {},
                onOpenSettings = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryListErrorPreview() {
    MiBovedaTheme {
        VaultBackground {
            EntryListScreen(
                state = EntryListState(isLoading = false, error = VaultError.VaultLocked),
                onAdd = {},
                onEntryClick = {},
                onOpenSettings = {},
            )
        }
    }
}
