package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.repository.VaultRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveRecoveryKeyStatus @Inject constructor(
    private val vaultRepository: VaultRepository,
) {
    operator fun invoke(): Flow<Boolean> = vaultRepository.hasRecoveryKey
}
