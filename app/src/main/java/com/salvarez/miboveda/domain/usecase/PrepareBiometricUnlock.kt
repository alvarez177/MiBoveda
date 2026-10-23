package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.repository.BiometricRepository
import javax.crypto.Cipher
import javax.inject.Inject

class PrepareBiometricUnlock @Inject constructor(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(): Outcome<Cipher> = biometricRepository.prepareUnlock()
}
