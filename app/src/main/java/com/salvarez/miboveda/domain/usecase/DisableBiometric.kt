package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.repository.BiometricRepository
import javax.inject.Inject

class DisableBiometric @Inject constructor(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke() = biometricRepository.disable()
}
