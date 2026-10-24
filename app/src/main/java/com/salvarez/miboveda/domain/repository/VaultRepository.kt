package com.salvarez.miboveda.domain.repository

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.RecoveryKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VaultRepository {
    val isUnlocked: StateFlow<Boolean>

    /** True once a recovery key exists for the vault. */
    val hasRecoveryKey: Flow<Boolean>

    suspend fun exists(): Outcome<Boolean>

    /**
     * Creates the vault, leaves it unlocked and returns its recovery key, which is not stored anywhere in clear.
     * The caller owns [masterPassword] and may clear it afterwards.
     */
    suspend fun create(masterPassword: CharArray): Outcome<RecoveryKey>

    /** Unlocks an existing vault. The caller owns [masterPassword] and may clear it afterwards. */
    suspend fun unlock(masterPassword: CharArray): Outcome<Unit>

    /** Closes the database and forgets the keys. */
    suspend fun lock()

    /** Makes a new recovery key (the previous one stops working). Needs the master password. */
    suspend fun generateRecoveryKey(masterPassword: CharArray): Outcome<RecoveryKey>

    /** Sets a new master password using the recovery key and unlocks the vault; the data is kept. */
    suspend fun recover(recoveryKey: String, newMasterPassword: CharArray): Outcome<Unit>
}
