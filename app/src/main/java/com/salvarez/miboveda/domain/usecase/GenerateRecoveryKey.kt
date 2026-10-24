package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.RecoveryKey
import com.salvarez.miboveda.domain.repository.VaultRepository
import javax.inject.Inject

class GenerateRecoveryKey @Inject constructor(
    private val vaultRepository: VaultRepository,
) {
    suspend operator fun invoke(masterPassword: CharArray): Outcome<RecoveryKey> =
        vaultRepository.generateRecoveryKey(masterPassword)
}
