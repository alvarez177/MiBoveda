package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.repository.SettingsRepository
import javax.inject.Inject

class SetAutoLockTimeout @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(timeout: AutoLockTimeout) = settingsRepository.setAutoLockTimeout(timeout)
}
