package com.salvarez.miboveda.data.db

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Owns the encrypted database. It only exists while the vault is unlocked:
 * [open] builds it with the database key and [close] closes it.
 */
@Singleton
class VaultSession @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var database: VaultDatabase? = null

    val isOpen: Boolean get() = database != null

    /**
     * Opens the database and checks [key] right away (Room would otherwise open it lazily).
     * Throws when [key] is wrong. The caller keeps ownership of [key]; a copy is used internally.
     */
    suspend fun open(key: ByteArray) = withContext(Dispatchers.IO) {
        close()
        // SQLCipher clears the passphrase it is given once the database is open, so hand it a copy.
        val factory = SupportOpenHelperFactory(key.copyOf())
        val db = Room.databaseBuilder(context, VaultDatabase::class.java, databaseFile().absolutePath)
            .openHelperFactory(factory)
            .build()
        try {
            db.openHelper.writableDatabase
        } catch (e: Exception) {
            db.close()
            throw e
        }
        database = db
    }

    /** Throws [VaultLockedException] when the vault is locked. */
    fun entryDao(): EntryDao = (database ?: throw VaultLockedException()).entryDao()

    fun close() {
        database?.close()
        database = null
    }

    fun databaseFile(): File = context.getDatabasePath(DATABASE_NAME)

    private companion object {
        const val DATABASE_NAME = "vault.db"

        init {
            System.loadLibrary("sqlcipher")
        }
    }
}

class VaultLockedException : IllegalStateException("Vault is locked")
