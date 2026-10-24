package com.salvarez.miboveda.data.repository

import com.salvarez.miboveda.data.VaultLockState
import com.salvarez.miboveda.data.crypto.AesGcmKeyWrapper
import com.salvarez.miboveda.data.crypto.KEY_SIZE_BYTES
import com.salvarez.miboveda.data.crypto.KdfParams
import com.salvarez.miboveda.data.crypto.KeyDerivation
import com.salvarez.miboveda.data.crypto.RecoveryKeyCodec
import com.salvarez.miboveda.data.crypto.SALT_SIZE_BYTES
import com.salvarez.miboveda.data.crypto.secureRandomBytes
import com.salvarez.miboveda.data.db.VaultSession
import com.salvarez.miboveda.data.outcomeOf
import com.salvarez.miboveda.data.settings.VaultHeader
import com.salvarez.miboveda.data.settings.VaultHeaderStore
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.RecoveryKey
import com.salvarez.miboveda.domain.repository.VaultRepository
import javax.crypto.BadPaddingException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val headerStore: VaultHeaderStore,
    private val keyDerivation: KeyDerivation,
    private val keyWrapper: AesGcmKeyWrapper,
    private val dekUnwrapper: DekUnwrapper,
    private val session: VaultSession,
    private val lockState: VaultLockState,
) : VaultRepository {

    override val isUnlocked: StateFlow<Boolean> = lockState.isUnlocked

    override suspend fun exists(): Outcome<Boolean> = outcomeOf { headerStore.read() != null }

    override val hasRecoveryKey: Flow<Boolean> = headerStore.hasRecoveryKey

    override suspend fun create(masterPassword: CharArray): Outcome<RecoveryKey> {
        when (val existing = exists()) {
            is Outcome.Failure -> return existing
            is Outcome.Success -> if (existing.value) return Outcome.Failure(VaultError.VaultAlreadyExists)
        }
        return outcomeOf {
            val params = KdfParams.DEFAULT
            val salt = secureRandomBytes(SALT_SIZE_BYTES)
            val dek = secureRandomBytes(KEY_SIZE_BYTES)
            val recoveryBytes = secureRandomBytes(KEY_SIZE_BYTES)
            val kek = withContext(Dispatchers.Default) { keyDerivation.derive(masterPassword, salt, params) }
            try {
                val header = VaultHeader(
                    version = VaultHeader.CURRENT_VERSION,
                    kdfParams = params,
                    salt = salt,
                    wrappedDek = keyWrapper.wrap(kek, dek),
                    recoveryWrappedDek = keyWrapper.wrap(recoveryBytes, dek),
                )
                headerStore.save(header)
                session.open(dek)
                lockState.set(true)
                RecoveryKey(RecoveryKeyCodec.encode(recoveryBytes))
            } finally {
                kek.fill(0)
                dek.fill(0)
                recoveryBytes.fill(0)
            }
        }
    }

    override suspend fun unlock(masterPassword: CharArray): Outcome<Unit> {
        val dek = when (val result = dekUnwrapper.unwrap(masterPassword)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value
        }
        return outcomeOf {
            try {
                session.open(dek)
            } finally {
                dek.fill(0)
            }
            lockState.set(true)
        }
    }

    override suspend fun lock() {
        session.close()
        lockState.set(false)
    }

    override suspend fun generateRecoveryKey(masterPassword: CharArray): Outcome<RecoveryKey> {
        val dek = when (val result = dekUnwrapper.unwrap(masterPassword)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value
        }
        return outcomeOf {
            val recoveryBytes = secureRandomBytes(KEY_SIZE_BYTES)
            try {
                headerStore.saveRecovery(keyWrapper.wrap(recoveryBytes, dek))
                RecoveryKey(RecoveryKeyCodec.encode(recoveryBytes))
            } finally {
                dek.fill(0)
                recoveryBytes.fill(0)
            }
        }
    }

    override suspend fun recover(recoveryKey: String, newMasterPassword: CharArray): Outcome<Unit> {
        val recoveryBytes = RecoveryKeyCodec.decode(recoveryKey) ?: return Outcome.Failure(VaultError.InvalidRecoveryKey)
        return try {
            val header = when (val result = outcomeOf { headerStore.read() }) {
                is Outcome.Failure -> return result
                is Outcome.Success -> result.value ?: return Outcome.Failure(VaultError.VaultNotCreated)
            }
            val recoveryWrapped = header.recoveryWrappedDek ?: return Outcome.Failure(VaultError.NoRecoveryKey)
            outcomeOf(mapError = { if (it is BadPaddingException) VaultError.InvalidRecoveryKey else VaultError.StorageFailure }) {
                val dek = keyWrapper.unwrap(recoveryBytes, recoveryWrapped)
                val params = KdfParams.DEFAULT
                val salt = secureRandomBytes(SALT_SIZE_BYTES)
                val kek = withContext(Dispatchers.Default) { keyDerivation.derive(newMasterPassword, salt, params) }
                try {
                    // The database key does not change, so the data and the recovery key stay valid.
                    headerStore.save(
                        VaultHeader(header.version, params, salt, keyWrapper.wrap(kek, dek), header.recoveryWrappedDek),
                    )
                    session.open(dek)
                    lockState.set(true)
                } finally {
                    kek.fill(0)
                    dek.fill(0)
                }
            }
        } finally {
            recoveryBytes.fill(0)
        }
    }
}
