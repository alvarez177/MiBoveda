package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.domain.repository.BiometricRepository
import javax.crypto.Cipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBiometricRepository(
    initialStatus: BiometricStatus = BiometricStatus.UNAVAILABLE,
) : BiometricRepository {
    /** Any cipher works as a token: the real one is only exercised on a device. */
    val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")

    private val statusState = MutableStateFlow(initialStatus)

    var prepareEnrollmentResult: Outcome<Cipher> = Outcome.Success(cipher)
    var completeEnrollmentResult: Outcome<Unit> = Outcome.Success(Unit)
    var prepareUnlockResult: Outcome<Cipher> = Outcome.Success(cipher)
    var unlockResult: Outcome<Unit> = Outcome.Success(Unit)

    val enrollmentPasswords = mutableListOf<String>()
    var disableCalls = 0
        private set

    override val status: Flow<BiometricStatus> = statusState

    override suspend fun prepareEnrollment(masterPassword: CharArray): Outcome<Cipher> = prepareEnrollmentResult

    override suspend fun completeEnrollment(cipher: Cipher, masterPassword: CharArray): Outcome<Unit> {
        // Copy now: the caller clears the array right after the call.
        enrollmentPasswords += String(masterPassword)
        if (completeEnrollmentResult is Outcome.Success) statusState.value = BiometricStatus.ENABLED
        return completeEnrollmentResult
    }

    override suspend fun disable() {
        disableCalls++
        statusState.value = BiometricStatus.AVAILABLE
    }

    override suspend fun prepareUnlock(): Outcome<Cipher> = prepareUnlockResult

    override suspend fun unlock(cipher: Cipher): Outcome<Unit> = unlockResult
}
