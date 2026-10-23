package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.repository.BiometricRepository
import javax.crypto.Cipher
import javax.inject.Inject

class PrepareBiometricEnrollment @Inject constructor(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(masterPassword: CharArray): Outcome<Cipher> =
        biometricRepository.prepareEnrollment(masterPassword)
}
