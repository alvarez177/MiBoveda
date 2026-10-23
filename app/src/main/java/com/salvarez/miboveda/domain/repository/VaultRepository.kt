package com.salvarez.miboveda.domain.repository

import com.salvarez.miboveda.domain.Outcome
import kotlinx.coroutines.flow.StateFlow

interface VaultRepository {
    val isUnlocked: StateFlow<Boolean>

    suspend fun exists(): Outcome<Boolean>

    /** The caller owns [masterPassword] and may clear it once the call returns. */
    suspend fun create(masterPassword: CharArray): Outcome<Unit>

    /** The caller owns [masterPassword] and may clear it once the call returns. */
    suspend fun unlock(masterPassword: CharArray): Outcome<Unit>

    suspend fun lock()
}
