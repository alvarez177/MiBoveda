package com.salvarez.miboveda.domain.repository

import com.salvarez.miboveda.domain.Outcome
import kotlinx.coroutines.flow.StateFlow

interface VaultRepository {
    val isUnlocked: StateFlow<Boolean>

    suspend fun exists(): Outcome<Boolean>

    /** Creates the vault and leaves it unlocked. The caller owns [masterPassword] and may clear it afterwards. */
    suspend fun create(masterPassword: CharArray): Outcome<Unit>

    /** Unlocks an existing vault. The caller owns [masterPassword] and may clear it afterwards. */
    suspend fun unlock(masterPassword: CharArray): Outcome<Unit>

    /** Closes the database and forgets the keys. */
    suspend fun lock()
}
