package com.salvarez.miboveda.domain.repository

import com.salvarez.miboveda.domain.model.AutoLockTimeout
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Emits [AutoLockTimeout.DEFAULT] when nothing was chosen yet or the stored value cannot be read. */
    val autoLockTimeout: Flow<AutoLockTimeout>

    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout)
}
