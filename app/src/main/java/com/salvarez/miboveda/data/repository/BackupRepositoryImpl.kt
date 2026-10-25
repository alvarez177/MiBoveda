package com.salvarez.miboveda.data.repository

import android.content.Context
import android.net.Uri
import com.salvarez.miboveda.data.backup.BackupCodec
import com.salvarez.miboveda.data.backup.BackupFormatException
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
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.crypto.BadPaddingException
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

    override suspend fun read(masterPassword: CharArray, sourceUri: String): Outcome<List<PasswordEntry>> =
        outcomeOf(mapError = ::readError) {
            val bytes = withContext(Dispatchers.IO) {
                val stream = context.contentResolver.openInputStream(Uri.parse(sourceUri))
                    ?: throw IOException("Cannot open the backup")
                stream.use { it.readUpTo(MAX_BACKUP_BYTES) }
            }
            val header = codec.readHeader(bytes)
            val key = withContext(Dispatchers.Default) { keyDerivation.derive(masterPassword, header.salt, header.kdfParams) }
            try {
                codec.decode(bytes, header, key)
            } finally {
                key.fill(0)
            }
        }

    // A wrong password and an altered file fail the same check, so they share one message.
    private fun readError(e: Exception): VaultError = when (e) {
        is BackupFormatException -> VaultError.CorruptBackup
        is BadPaddingException -> VaultError.WrongBackupPassword
        else -> VaultError.StorageFailure
    }

    private fun InputStream.readUpTo(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > limit) throw BackupFormatException("The file is too large to be a backup")
        }
        return out.toByteArray()
    }

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

    private companion object {
        // Far more than any real vault, but small enough that a wrong file cannot fill the memory.
        const val MAX_BACKUP_BYTES = 32 * 1024 * 1024
    }
}
