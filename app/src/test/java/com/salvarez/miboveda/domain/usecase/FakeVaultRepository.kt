package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.RecoveryKey
import com.salvarez.miboveda.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeVaultRepository(
    private val existsResult: Outcome<Boolean> = Outcome.Success(false),
    private val createResult: Outcome<RecoveryKey> = Outcome.Success(SAMPLE_RECOVERY_KEY),
    private val unlockResult: Outcome<Unit> = Outcome.Success(Unit),
    private val generateResult: Outcome<RecoveryKey> = Outcome.Success(SAMPLE_RECOVERY_KEY),
    private val recoverResult: Outcome<Unit> = Outcome.Success(Unit),
    unlocked: Boolean = false,
    hasRecoveryKey: Boolean = false,
) : VaultRepository {
    private val unlockedState = MutableStateFlow(unlocked)
    private val recoveryState = MutableStateFlow(hasRecoveryKey)

    var createCalls = 0
        private set
    var unlockCalls = 0
        private set
    var lockCalls = 0
        private set
    var recoverCalls = 0
        private set
    val generatePasswords = mutableListOf<String>()

    override val isUnlocked: StateFlow<Boolean> = unlockedState
    override val hasRecoveryKey: Flow<Boolean> = recoveryState

    override suspend fun exists(): Outcome<Boolean> = existsResult

    override suspend fun create(masterPassword: CharArray): Outcome<RecoveryKey> {
        createCalls++
        return createResult
    }

    override suspend fun unlock(masterPassword: CharArray): Outcome<Unit> {
        unlockCalls++
        if (unlockResult is Outcome.Success) unlockedState.value = true
        return unlockResult
    }

    override suspend fun lock() {
        lockCalls++
        unlockedState.value = false
    }

    override suspend fun generateRecoveryKey(masterPassword: CharArray): Outcome<RecoveryKey> {
        // Copy now: the caller clears the array right after the call.
        generatePasswords += String(masterPassword)
        if (generateResult is Outcome.Success) recoveryState.value = true
        return generateResult
    }

    override suspend fun recover(recoveryKey: String, newMasterPassword: CharArray): Outcome<Unit> {
        recoverCalls++
        if (recoverResult is Outcome.Success) unlockedState.value = true
        return recoverResult
    }

    companion object {
        val SAMPLE_RECOVERY_KEY = RecoveryKey("ABCD-EFGH-IJKL-MNOP-QRST-UVWX-YZ23-4567-ABCD-EFGH-IJKL-MNOP-QRST")
    }
}
