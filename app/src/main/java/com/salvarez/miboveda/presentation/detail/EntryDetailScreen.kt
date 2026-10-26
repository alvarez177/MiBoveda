package com.salvarez.miboveda.presentation.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import com.salvarez.miboveda.presentation.theme.Motion
import com.salvarez.miboveda.presentation.ui.AnimatedCheck
import com.salvarez.miboveda.presentation.ui.Avatar
import com.salvarez.miboveda.presentation.ui.SecretText
import com.salvarez.miboveda.presentation.ui.VaultBackground
import com.salvarez.miboveda.presentation.ui.VaultButton
import com.salvarez.miboveda.presentation.ui.VaultCard
import com.salvarez.miboveda.presentation.ui.VaultOutlinedButton
import com.salvarez.miboveda.presentation.ui.VaultScaffold
import com.salvarez.miboveda.presentation.ui.VaultTopBar
import com.salvarez.miboveda.presentation.ui.cascadeEntrance
import kotlinx.coroutines.delay

@Composable
fun EntryDetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: EntryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var copiedSignal by remember { mutableIntStateOf(0) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EntryDetailEffect.Copied -> copiedSignal++
                EntryDetailEffect.Deleted -> onBack()
            }
        }
    }
    EntryDetailScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onEdit = { state.entry?.let { onEdit(it.id) } },
        copiedSignal = copiedSignal,
    )
}

@Composable
fun EntryDetailScreen(
    state: EntryDetailState,
    onIntent: (EntryDetailIntent) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    /** Goes up each time something is copied; every increase shows the confirmation for a moment. */
    copiedSignal: Int = 0,
) {
    VaultScaffold(topBar = { VaultTopBar(title = "", onBack = onBack) }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val entry = state.entry
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                entry == null -> Text(
                    text = state.error?.message().orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> EntryContent(entry, state, onIntent, onEdit)
            }
            CopiedPill(copiedSignal, Modifier.align(Alignment.BottomCenter))
        }
    }
    if (state.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(EntryDetailIntent.DismissDelete) },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(R.string.delete_entry_title)) },
            text = { Text(stringResource(R.string.delete_entry_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(EntryDetailIntent.ConfirmDelete) }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().cascadeEntrance(0).padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(entry.title, size = 84.dp)
            Spacer(Modifier.height(14.dp))
            Text(entry.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        }
        VaultCard(modifier = Modifier.cascadeEntrance(1)) {
            FieldRow(Icons.Rounded.Person, stringResource(R.string.username_label)) {
                Text(entry.username.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
                if (entry.username.isNotBlank()) {
                    IconAction(Icons.Rounded.ContentCopy, stringResource(R.string.copy)) { onIntent(EntryDetailIntent.CopyUsername) }
                }
            }
            Divider()
            FieldRow(Icons.Rounded.Lock, stringResource(R.string.password_label)) {
                SecretText(
                    secret = entry.password,
                    revealed = state.isPasswordVisible,
                    modifier = Modifier.weight(1f),
                )
                IconAction(
                    icon = if (state.isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    description = stringResource(if (state.isPasswordVisible) R.string.hide else R.string.show),
                ) { onIntent(EntryDetailIntent.TogglePasswordVisibility) }
                IconAction(Icons.Rounded.ContentCopy, stringResource(R.string.copy)) { onIntent(EntryDetailIntent.CopyPassword) }
            }
        }
        if (entry.url.isNotBlank() || entry.notes.isNotBlank()) {
            VaultCard(modifier = Modifier.cascadeEntrance(2)) {
                if (entry.url.isNotBlank()) {
                    FieldRow(Icons.Rounded.Language, stringResource(R.string.url_label)) {
                        Text(entry.url, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (entry.url.isNotBlank() && entry.notes.isNotBlank()) Divider()
                if (entry.notes.isNotBlank()) {
                    FieldRow(Icons.AutoMirrored.Rounded.Notes, stringResource(R.string.notes_label)) {
                        Text(entry.notes, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().cascadeEntrance(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VaultButton(
                text = stringResource(R.string.edit),
                onClick = onEdit,
                icon = Icons.Rounded.Edit,
                modifier = Modifier.weight(1f),
            )
            VaultOutlinedButton(
                text = stringResource(R.string.delete),
                onClick = { onIntent(EntryDetailIntent.RequestDelete) },
                icon = Icons.Rounded.Delete,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FieldRow(icon: ImageVector, label: String, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) { content() }
        }
    }
}

@Composable
private fun Divider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.IconAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Slides up with a drawn check mark each time something is copied, then goes away by itself. */
@Composable
private fun CopiedPill(signal: Int, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(signal) {
        if (signal > 0) {
            visible = true
            delay(2800)
            visible = false
        }
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.safeDrawingPadding().padding(24.dp),
        enter = slideInVertically(tween(Motion.Medium, easing = Motion.EmphasizedDecelerate)) { it } + fadeIn(tween(Motion.Medium)),
        exit = slideOutVertically(tween(Motion.Medium, easing = Motion.EmphasizedAccelerate)) { it } + fadeOut(tween(Motion.Short)),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnimatedCheck(color = MaterialTheme.colorScheme.inversePrimary)
                Text(stringResource(R.string.copied_to_clipboard), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryDetailPreview() {
    MiBovedaTheme {
        VaultBackground {
            EntryDetailScreen(
                state = EntryDetailState(
                    entry = PasswordEntry(id = 1, title = "Banco", username = "ana", password = "s3cret", url = "https://banco.example"),
                    isLoading = false,
                ),
                onIntent = {},
                onBack = {},
                onEdit = {},
            )
        }
    }
}
