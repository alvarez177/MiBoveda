package com.salvarez.miboveda.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.usecase.ObserveEntries
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class EntryListState(
    val entries: List<PasswordEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: VaultError? = null,
)

/** The list only displays data; navigation (add, open) is handled by the screen callbacks. */
@HiltViewModel
class EntryListViewModel @Inject constructor(
    observeEntries: ObserveEntries,
) : ViewModel() {

    val state: StateFlow<EntryListState> = observeEntries()
        .map { outcome ->
            when (outcome) {
                is Outcome.Success -> EntryListState(entries = outcome.value, isLoading = false)
                is Outcome.Failure -> EntryListState(isLoading = false, error = outcome.error)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EntryListState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
