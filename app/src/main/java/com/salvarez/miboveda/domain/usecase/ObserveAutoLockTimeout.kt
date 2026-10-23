package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAutoLockTimeout @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<AutoLockTimeout> = settingsRepository.autoLockTimeout
}
