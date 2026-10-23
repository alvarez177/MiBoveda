package com.salvarez.miboveda.presentation.detail

import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry

data class EntryDetailState(
    val entry: PasswordEntry? = null,
    val isLoading: Boolean = true,
    val error: VaultError? = null,
    val isPasswordVisible: Boolean = false,
    val isDeleteDialogVisible: Boolean = false,
)

sealed interface EntryDetailIntent {
    data object TogglePasswordVisibility : EntryDetailIntent
    data object CopyUsername : EntryDetailIntent
    data object CopyPassword : EntryDetailIntent
    data object RequestDelete : EntryDetailIntent
    data object DismissDelete : EntryDetailIntent
    data object ConfirmDelete : EntryDetailIntent
}

sealed interface EntryDetailEffect {
    data object Copied : EntryDetailEffect
    data object Deleted : EntryDetailEffect
}
