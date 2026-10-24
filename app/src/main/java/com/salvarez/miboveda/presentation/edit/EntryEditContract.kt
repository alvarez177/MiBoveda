package com.salvarez.miboveda.presentation.edit

import com.salvarez.miboveda.domain.error.VaultError

data class EntryEditState(
    val id: Long = 0,
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: VaultError? = null,
) {
    val isNew: Boolean get() = id == 0L

    // Keeps the password out of logs.
    override fun toString(): String = "EntryEditState(id=$id, isLoading=$isLoading, isSaving=$isSaving, error=$error)"
}

sealed interface EntryEditIntent {
    data class TitleChanged(val value: String) : EntryEditIntent
    data class UsernameChanged(val value: String) : EntryEditIntent
    data class PasswordChanged(val value: String) : EntryEditIntent {
        override fun toString(): String = "PasswordChanged"
    }
    data class UrlChanged(val value: String) : EntryEditIntent
    data class NotesChanged(val value: String) : EntryEditIntent
    data object TogglePasswordVisibility : EntryEditIntent
    data object Save : EntryEditIntent
}

sealed interface EntryEditEffect {
    data object Saved : EntryEditEffect
}
