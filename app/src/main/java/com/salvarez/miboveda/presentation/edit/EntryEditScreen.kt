package com.salvarez.miboveda.presentation.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import com.salvarez.miboveda.presentation.ui.PasswordTextField
import com.salvarez.miboveda.presentation.ui.VaultBackground
import com.salvarez.miboveda.presentation.ui.VaultButton
import com.salvarez.miboveda.presentation.ui.VaultScaffold
import com.salvarez.miboveda.presentation.ui.VaultTextField
import com.salvarez.miboveda.presentation.ui.VaultTopBar
import com.salvarez.miboveda.presentation.ui.cascadeEntrance

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

@Composable
fun EntryEditScreen(
    state: EntryEditState,
    onIntent: (EntryEditIntent) -> Unit,
    onBack: () -> Unit,
) {
    VaultScaffold(
        topBar = {
            VaultTopBar(
                title = stringResource(if (state.isNew) R.string.new_entry_title else R.string.edit_entry_title),
                onBack = onBack,
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VaultTextField(
            value = state.title,
            onValueChange = { onIntent(EntryEditIntent.TitleChanged(it)) },
            label = stringResource(R.string.title_label),
            leadingIcon = Icons.Rounded.Badge,
            isError = state.error != null,
            modifier = Modifier.cascadeEntrance(0),
        )
        VaultTextField(
            value = state.username,
            onValueChange = { onIntent(EntryEditIntent.UsernameChanged(it)) },
            label = stringResource(R.string.username_label),
            leadingIcon = Icons.Rounded.Person,
            modifier = Modifier.cascadeEntrance(1),
        )
        PasswordTextField(
            value = state.password,
            onValueChange = { onIntent(EntryEditIntent.PasswordChanged(it)) },
            label = stringResource(R.string.password_label),
            isVisible = state.isPasswordVisible,
            onToggleVisibility = { onIntent(EntryEditIntent.TogglePasswordVisibility) },
            modifier = Modifier.cascadeEntrance(2),
        )
        VaultTextField(
            value = state.url,
            onValueChange = { onIntent(EntryEditIntent.UrlChanged(it)) },
            label = stringResource(R.string.url_label),
            leadingIcon = Icons.Rounded.Language,
            modifier = Modifier.cascadeEntrance(3),
        )
        VaultTextField(
            value = state.notes,
            onValueChange = { onIntent(EntryEditIntent.NotesChanged(it)) },
            label = stringResource(R.string.notes_label),
            leadingIcon = Icons.AutoMirrored.Rounded.Notes,
            singleLine = false,
            minLines = 3,
            modifier = Modifier.cascadeEntrance(4),
        )
        AnimatedVisibility(visible = state.error != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            state.error?.let {
                Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(4.dp))
        VaultButton(
            text = stringResource(R.string.save),
            onClick = { onIntent(EntryEditIntent.Save) },
            enabled = !state.isSaving,
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth().cascadeEntrance(5),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryEditPreview() {
    MiBovedaTheme { VaultBackground { EntryEditScreen(state = EntryEditState(title = "Banco"), onIntent = {}, onBack = {}) } }
}
