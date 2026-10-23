package com.salvarez.miboveda.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.usecase.DeleteEntry
import com.salvarez.miboveda.domain.usecase.ObserveEntry
import com.salvarez.miboveda.presentation.common.SensitiveClipboard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeEntry: ObserveEntry,
    private val deleteEntry: DeleteEntry,
    private val clipboard: SensitiveClipboard,
) : ViewModel() {

    private val entryId: Long = checkNotNull(savedStateHandle[ARG_ID])

    private val _state = MutableStateFlow(EntryDetailState())
    val state: StateFlow<EntryDetailState> = _state.asStateFlow()

    private val _effects = Channel<EntryDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var isDeleting = false

    init {
        viewModelScope.launch {
            observeEntry(entryId).collect { outcome ->
                // Once deleted, the entry disappears; the screen is already closing.
                if (isDeleting) return@collect
                when (outcome) {
                    is Outcome.Success -> _state.update { it.copy(entry = outcome.value, isLoading = false, error = null) }
                    is Outcome.Failure -> _state.update { it.copy(entry = null, isLoading = false, error = outcome.error) }
                }
            }
        }
    }

    fun onIntent(intent: EntryDetailIntent) {
        when (intent) {
            EntryDetailIntent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            EntryDetailIntent.CopyUsername -> copy(LABEL_USERNAME) { it.username }
            EntryDetailIntent.CopyPassword -> copy(LABEL_PASSWORD) { it.password }
            EntryDetailIntent.RequestDelete -> _state.update { it.copy(isDeleteDialogVisible = true) }
            EntryDetailIntent.DismissDelete -> _state.update { it.copy(isDeleteDialogVisible = false) }
            EntryDetailIntent.ConfirmDelete -> delete()
        }
    }

    private fun copy(label: String, value: (com.salvarez.miboveda.domain.model.PasswordEntry) -> String) {
        val entry = _state.value.entry ?: return
        clipboard.copy(label, value(entry))
        _effects.trySend(EntryDetailEffect.Copied)
    }

    private fun delete() {
        _state.update { it.copy(isDeleteDialogVisible = false) }
        viewModelScope.launch {
            isDeleting = true
            when (val result = deleteEntry(entryId)) {
                is Outcome.Success -> _effects.send(EntryDetailEffect.Deleted)
                is Outcome.Failure -> {
                    isDeleting = false
                    _state.update { it.copy(error = result.error) }
                }
            }
        }
    }

    companion object {
        const val ARG_ID = "id"
        private const val LABEL_USERNAME = "username"
        private const val LABEL_PASSWORD = "password"
    }
}
