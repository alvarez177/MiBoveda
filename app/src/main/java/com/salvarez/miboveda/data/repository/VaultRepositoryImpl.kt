package com.salvarez.miboveda.data.repository

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
import javax.crypto.BadPaddingException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val headerStore: VaultHeaderStore,
    private val keyDerivation: KeyDerivation,
    private val keyWrapper: AesGcmKeyWrapper,
    private val session: VaultSession,
) : VaultRepository {

    private val unlocked = MutableStateFlow(false)
    override val isUnlocked: StateFlow<Boolean> = unlocked.asStateFlow()

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
            unlocked.value = true
        }
    }

    override suspend fun unlock(masterPassword: CharArray): Outcome<Unit> {
        val header = when (val result = outcomeOf { headerStore.read() }) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value ?: return Outcome.Failure(VaultError.VaultNotCreated)
        }
        return outcomeOf(mapError = ::unlockError) {
            val kek = withContext(Dispatchers.Default) {
                keyDerivation.derive(masterPassword, header.salt, header.kdfParams)
            }
            val dek = try {
                keyWrapper.unwrap(kek, header.wrappedDek)
            } finally {
                kek.fill(0)
            }
            try {
                session.open(dek)
            } finally {
                dek.fill(0)
            }
            unlocked.value = true
        }
    }

    override suspend fun lock() {
        session.close()
        unlocked.value = false
    }

    // A wrong master password shows up as an authentication failure when unwrapping the database key.
    private fun unlockError(e: Exception): VaultError =
        if (e is BadPaddingException) VaultError.WrongMasterPassword else VaultError.StorageFailure
}
