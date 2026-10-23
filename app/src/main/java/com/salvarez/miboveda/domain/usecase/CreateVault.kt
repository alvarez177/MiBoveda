package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.MIN_MASTER_PASSWORD_LENGTH
import com.salvarez.miboveda.domain.repository.VaultRepository
import javax.inject.Inject

class CreateVault @Inject constructor(
    private val vaultRepository: VaultRepository,
) {
    suspend operator fun invoke(password: CharArray, confirmation: CharArray): Outcome<Unit> = when {
        password.size < MIN_MASTER_PASSWORD_LENGTH -> Outcome.Failure(VaultError.MasterPasswordTooShort)
        !password.contentEquals(confirmation) -> Outcome.Failure(VaultError.MasterPasswordMismatch)
        else -> vaultRepository.create(password)
    }
}
