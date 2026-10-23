package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.repository.BiometricRepository
import javax.crypto.Cipher
import javax.inject.Inject

class UnlockWithBiometric @Inject constructor(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(cipher: Cipher): Outcome<Unit> = biometricRepository.unlock(cipher)
}
