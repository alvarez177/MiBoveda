package com.salvarez.miboveda.data.repository

import com.salvarez.miboveda.data.VaultLockState
import com.salvarez.miboveda.data.crypto.AesGcmKeyWrapper
import com.salvarez.miboveda.data.crypto.KEY_SIZE_BYTES
import com.salvarez.miboveda.data.crypto.KdfParams
import com.salvarez.miboveda.data.crypto.KeyDerivation
import com.salvarez.miboveda.data.crypto.SALT_SIZE_BYTES
import com.salvarez.miboveda.data.crypto.secureRandomBytes
import com.salvarez.miboveda.data.db.VaultSession
import com.salvarez.miboveda.data.outcomeOf
import com.salvarez.miboveda.data.settings.VaultHeader
import com.salvarez.miboveda.data.settings.VaultHeaderStore
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.repository.VaultRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
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

    override suspend fun create(masterPassword: CharArray): Outcome<Unit> {
        when (val existing = exists()) {
            is Outcome.Failure -> return existing
            is Outcome.Success -> if (existing.value) return Outcome.Failure(VaultError.VaultAlreadyExists)
        }
        return outcomeOf {
            val params = KdfParams.DEFAULT
            val salt = secureRandomBytes(SALT_SIZE_BYTES)
            val dek = secureRandomBytes(KEY_SIZE_BYTES)
            val kek = withContext(Dispatchers.Default) { keyDerivation.derive(masterPassword, salt, params) }
            try {
                headerStore.save(VaultHeader(VaultHeader.CURRENT_VERSION, params, salt, keyWrapper.wrap(kek, dek)))
                session.open(dek)
            } finally {
                kek.fill(0)
                dek.fill(0)
            }
            lockState.set(true)
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
}
