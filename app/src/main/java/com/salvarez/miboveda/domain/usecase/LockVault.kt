package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.repository.VaultRepository
import javax.inject.Inject

class LockVault @Inject constructor(
    private val vaultRepository: VaultRepository,
) {
    suspend operator fun invoke() = vaultRepository.lock()
}
