package com.salvarez.miboveda.data.repository

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.salvarez.miboveda.data.VaultLockState
import com.salvarez.miboveda.data.crypto.BiometricAvailability
import com.salvarez.miboveda.data.crypto.BiometricKeyStore
import com.salvarez.miboveda.data.db.VaultSession
import com.salvarez.miboveda.data.outcomeOf
import com.salvarez.miboveda.data.settings.BiometricBlobStore
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.domain.repository.BiometricRepository
import javax.crypto.Cipher
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BiometricRepositoryImpl @Inject constructor(
    private val availability: BiometricAvailability,
    private val keyStore: BiometricKeyStore,
    private val blobStore: BiometricBlobStore,
    private val dekUnwrapper: DekUnwrapper,
    private val session: VaultSession,
    private val lockState: VaultLockState,
) : BiometricRepository {

    override val status: Flow<BiometricStatus> = blobStore.isEnabled.map { enabled ->
        when {
            !availability.isAvailable() -> BiometricStatus.UNAVAILABLE
            enabled -> BiometricStatus.ENABLED
            else -> BiometricStatus.AVAILABLE
        }
    }

    override suspend fun prepareEnrollment(masterPassword: CharArray): Outcome<Cipher> {
        if (!availability.isAvailable()) return Outcome.Failure(VaultError.BiometricUnavailable)
        when (val dek = dekUnwrapper.unwrap(masterPassword)) {
            is Outcome.Failure -> return dek
            // Only the password check matters here; the key is unwrapped again once the user approves.
            is Outcome.Success -> dek.value.fill(0)
        }
        return outcomeOf(mapError = { VaultError.BiometricUnavailable }) { keyStore.encryptCipher() }
    }

    override suspend fun completeEnrollment(cipher: Cipher, masterPassword: CharArray): Outcome<Unit> {
        val dek = when (val result = dekUnwrapper.unwrap(masterPassword)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value
        }
        return outcomeOf {
            try {
                blobStore.save(cipher.iv + cipher.doFinal(dek))
            } finally {
                dek.fill(0)
            }
        }
    }

    override suspend fun disable() {
        keyStore.deleteKey()
        blobStore.clear()
    }

    override suspend fun prepareUnlock(): Outcome<Cipher> {
        val blob = try {
            blobStore.read()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Outcome.Failure(VaultError.StorageFailure)
        } ?: return Outcome.Failure(VaultError.BiometricUnavailable)
        return try {
            Outcome.Success(keyStore.decryptCipher(blob.copyOfRange(0, BiometricKeyStore.IV_SIZE_BYTES)))
        } catch (e: KeyPermanentlyInvalidatedException) {
            // The enrolled biometrics changed: the key can never be used again, so the feature turns itself off.
            disable()
            Outcome.Failure(VaultError.BiometricInvalidated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Outcome.Failure(VaultError.BiometricUnavailable)
        }
    }

    override suspend fun unlock(cipher: Cipher): Outcome<Unit> = outcomeOf {
        val blob = blobStore.read() ?: error("Biometric key is not stored")
        val dek = cipher.doFinal(blob, BiometricKeyStore.IV_SIZE_BYTES, blob.size - BiometricKeyStore.IV_SIZE_BYTES)
        try {
            session.open(dek)
        } finally {
            dek.fill(0)
        }
        lockState.set(true)
    }
}
