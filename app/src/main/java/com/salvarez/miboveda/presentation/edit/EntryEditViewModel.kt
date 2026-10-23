package com.salvarez.miboveda.presentation.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.usecase.ObserveEntry
import com.salvarez.miboveda.domain.usecase.SaveEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Edits an existing entry, or creates one when the `id` argument is [PasswordEntry.NEW_ID]. */
@HiltViewModel
class EntryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeEntry: ObserveEntry,
    private val saveEntry: SaveEntry,
) : ViewModel() {

    private val entryId: Long = checkNotNull(savedStateHandle[ARG_ID])

    private val _state = MutableStateFlow(EntryEditState(id = entryId, isLoading = entryId != PasswordEntry.NEW_ID))
    val state: StateFlow<EntryEditState> = _state.asStateFlow()

    private val _effects = Channel<EntryEditEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        if (entryId != PasswordEntry.NEW_ID) {
            viewModelScope.launch {
                when (val outcome = observeEntry(entryId).first()) {
                    is Outcome.Success -> with(outcome.value) {
                        _state.update {
                            it.copy(
                                title = title,
                                username = username,
                                password = password,
                                url = url,
                                notes = notes,
                                isLoading = false,
                            )
                        }
                    }
                    is Outcome.Failure -> _state.update { it.copy(isLoading = false, error = outcome.error) }
                }
            }
        }
    }

    fun onIntent(intent: EntryEditIntent) {
        when (intent) {
            is EntryEditIntent.TitleChanged -> _state.update { it.copy(title = intent.value, error = null) }
            is EntryEditIntent.UsernameChanged -> _state.update { it.copy(username = intent.value) }
            is EntryEditIntent.PasswordChanged -> _state.update { it.copy(password = intent.value) }
            is EntryEditIntent.UrlChanged -> _state.update { it.copy(url = intent.value) }
            is EntryEditIntent.NotesChanged -> _state.update { it.copy(notes = intent.value) }
            EntryEditIntent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            EntryEditIntent.Save -> save()
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving || current.isLoading) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val entry = PasswordEntry(
                id = current.id,
                title = current.title,
                username = current.username,
                password = current.password,
                url = current.url,
                notes = current.notes,
            )
            when (val result = saveEntry(entry)) {
                is Outcome.Success -> _effects.send(EntryEditEffect.Saved)
                is Outcome.Failure -> _state.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }

    companion object {
        const val ARG_ID = "id"
    }
}
