package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeVaultRepository(
    private val existsResult: Outcome<Boolean> = Outcome.Success(false),
    private val createResult: Outcome<Unit> = Outcome.Success(Unit),
    private val unlockResult: Outcome<Unit> = Outcome.Success(Unit),
    unlocked: Boolean = false,
) : VaultRepository {
    private val unlockedState = MutableStateFlow(unlocked)

    var createCalls = 0
        private set
    var unlockCalls = 0
        private set
    var lockCalls = 0
        private set

    override val isUnlocked: StateFlow<Boolean> = unlockedState

    override suspend fun exists(): Outcome<Boolean> = existsResult

    override suspend fun create(masterPassword: CharArray): Outcome<Unit> {
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
}
