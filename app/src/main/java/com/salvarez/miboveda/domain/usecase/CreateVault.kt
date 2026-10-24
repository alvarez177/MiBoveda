package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.RecoveryKey
import com.salvarez.miboveda.domain.model.validateNewMasterPassword
import com.salvarez.miboveda.domain.repository.VaultRepository
import javax.inject.Inject

class CreateVault @Inject constructor(
    private val vaultRepository: VaultRepository,
) {
    suspend operator fun invoke(password: CharArray, confirmation: CharArray): Outcome<RecoveryKey> =
        validateNewMasterPassword(password, confirmation)?.let { Outcome.Failure(it) }
            ?: vaultRepository.create(password)
}
