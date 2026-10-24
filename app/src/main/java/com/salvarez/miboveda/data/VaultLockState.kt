package com.salvarez.miboveda.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Single source of truth for "is the vault open", shared by every way of unlocking it. */
@Singleton
class VaultLockState @Inject constructor() {
    private val state = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = state.asStateFlow()

    fun set(unlocked: Boolean) {
        state.value = unlocked
    }
}
