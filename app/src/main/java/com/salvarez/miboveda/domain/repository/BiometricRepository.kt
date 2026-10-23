package com.salvarez.miboveda.domain.repository

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.model.BiometricStatus
import javax.crypto.Cipher
import kotlinx.coroutines.flow.Flow

/**
 * Biometric unlock is only a convenience: it wraps the database key with a hardware-backed key that
 * needs a fingerprint or face. The master password always keeps working.
 *
 * Both flows are two steps because the system prompt must authorize the [Cipher] in between:
 * `prepare…` returns a cipher for the UI to show to the prompt, and the matching second step uses it.
 */
interface BiometricRepository {
    val status: Flow<BiometricStatus>

    /** Checks [masterPassword] and returns a cipher that still needs the user's biometric approval. */
    suspend fun prepareEnrollment(masterPassword: CharArray): Outcome<Cipher>

    /** [cipher] must have been approved by the biometric prompt. The caller may clear [masterPassword] afterwards. */
    suspend fun completeEnrollment(cipher: Cipher, masterPassword: CharArray): Outcome<Unit>

    suspend fun disable()

    /** Fails with `BiometricInvalidated` (and turns the feature off) when the enrolled biometrics changed. */
    suspend fun prepareUnlock(): Outcome<Cipher>

    /** [cipher] must have been approved by the biometric prompt. */
    suspend fun unlock(cipher: Cipher): Outcome<Unit>
}
