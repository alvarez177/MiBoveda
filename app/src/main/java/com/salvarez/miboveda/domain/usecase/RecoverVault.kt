package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.validateNewMasterPassword
import com.salvarez.miboveda.domain.repository.VaultRepository
import javax.inject.Inject

class RecoverVault @Inject constructor(
    private val vaultRepository: VaultRepository,
) {
    suspend operator fun invoke(
        recoveryKey: String,
        newPassword: CharArray,
        confirmation: CharArray,
    ): Outcome<Unit> =
        validateNewMasterPassword(newPassword, confirmation)?.let { Outcome.Failure(it) }
            ?: vaultRepository.recover(recoveryKey, newPassword)
}
