package com.salvarez.miboveda.data.repository

import android.content.Context
import android.net.Uri
import com.salvarez.miboveda.data.backup.BackupCodec
import com.salvarez.miboveda.data.crypto.KdfParams
import com.salvarez.miboveda.data.crypto.KeyDerivation
import com.salvarez.miboveda.data.crypto.SALT_SIZE_BYTES
import com.salvarez.miboveda.data.crypto.secureRandomBytes
import com.salvarez.miboveda.data.db.EntryEntity
import com.salvarez.miboveda.data.db.VaultLockedException
import com.salvarez.miboveda.data.db.VaultSession
import com.salvarez.miboveda.data.db.toDomain
import com.salvarez.miboveda.data.outcomeOf
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dekUnwrapper: DekUnwrapper,
    private val keyDerivation: KeyDerivation,
    private val session: VaultSession,
    private val codec: BackupCodec,
) : BackupRepository {

    override suspend fun export(masterPassword: CharArray, destinationUri: String): Outcome<Unit> {
        // The backup is locked with this password, so first make sure it is really the vault's password.
        when (val check = dekUnwrapper.unwrap(masterPassword)) {
            is Outcome.Failure -> return check
            is Outcome.Success -> check.value.fill(0)
        }
        return outcomeOf(mapError = { if (it is VaultLockedException) VaultError.VaultLocked else VaultError.StorageFailure }) {
            val entries = session.entryDao().getAll().map(EntryEntity::toDomain)
            val params = KdfParams.DEFAULT
            val salt = secureRandomBytes(SALT_SIZE_BYTES)
            val key = withContext(Dispatchers.Default) { keyDerivation.derive(masterPassword, salt, params) }
            val bytes = try {
                codec.encode(entries, key, salt, params)
            } finally {
                key.fill(0)
            }
            // One single write: a half-written file is never left behind by an earlier failure.
            withContext(Dispatchers.IO) {
                val stream = context.contentResolver.openOutputStream(Uri.parse(destinationUri), "wt")
                    ?: throw IOException("Cannot open the destination")
                stream.use { it.write(bytes) }
            }
        }
    }
}
