package com.salvarez.miboveda.data.repository

import com.salvarez.miboveda.data.crypto.AesGcmKeyWrapper
import com.salvarez.miboveda.data.crypto.KeyDerivation
import com.salvarez.miboveda.data.outcomeOf
import com.salvarez.miboveda.data.settings.VaultHeaderStore
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import javax.crypto.BadPaddingException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Recovers the database key from the master password. */
class DekUnwrapper @Inject constructor(
    private val headerStore: VaultHeaderStore,
    private val keyDerivation: KeyDerivation,
    private val keyWrapper: AesGcmKeyWrapper,
) {
    /** The caller owns the returned key and must clear it. */
    suspend fun unwrap(masterPassword: CharArray): Outcome<ByteArray> {
        val header = when (val result = outcomeOf { headerStore.read() }) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value ?: return Outcome.Failure(VaultError.VaultNotCreated)
        }
        return outcomeOf(mapError = ::unwrapError) {
            val kek = withContext(Dispatchers.Default) {
                keyDerivation.derive(masterPassword, header.salt, header.kdfParams)
            }
            try {
                keyWrapper.unwrap(kek, header.wrappedDek)
            } finally {
                kek.fill(0)
            }
        }
    }

    // A wrong master password shows up as an authentication failure when unwrapping the database key.
    private fun unwrapError(e: Exception): VaultError =
        if (e is BadPaddingException) VaultError.WrongMasterPassword else VaultError.StorageFailure
}
