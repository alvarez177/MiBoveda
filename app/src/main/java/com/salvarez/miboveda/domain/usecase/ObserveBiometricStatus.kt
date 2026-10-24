package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.domain.repository.BiometricRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveBiometricStatus @Inject constructor(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(): Flow<BiometricStatus> = biometricRepository.status
}
