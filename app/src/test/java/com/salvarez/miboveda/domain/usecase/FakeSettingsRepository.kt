package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(timeout: AutoLockTimeout = AutoLockTimeout.DEFAULT) : SettingsRepository {
    private val state = MutableStateFlow(timeout)

    override val autoLockTimeout: Flow<AutoLockTimeout> = state

    override suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        state.value = timeout
    }
}
