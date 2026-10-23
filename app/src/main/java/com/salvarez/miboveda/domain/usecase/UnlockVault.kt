package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.repository.VaultRepository
import javax.inject.Inject

class UnlockVault @Inject constructor(
    private val vaultRepository: VaultRepository,
) {
    suspend operator fun invoke(password: CharArray): Outcome<Unit> = vaultRepository.unlock(password)
}
