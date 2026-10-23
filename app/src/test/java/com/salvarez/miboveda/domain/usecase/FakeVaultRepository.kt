package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeVaultRepository(
    private val createResult: Outcome<Unit> = Outcome.Success(Unit),
) : VaultRepository {
    var createCalls = 0
        private set

    override val isUnlocked: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun exists(): Outcome<Boolean> = Outcome.Success(false)

    override suspend fun create(masterPassword: CharArray): Outcome<Unit> {
        createCalls++
        return createResult
    }
}
