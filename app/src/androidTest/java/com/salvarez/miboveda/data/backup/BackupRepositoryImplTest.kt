package com.salvarez.miboveda.data.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.salvarez.miboveda.data.VaultLockState
import com.salvarez.miboveda.data.crypto.AesGcmKeyWrapper
import com.salvarez.miboveda.data.crypto.Argon2KeyDerivation
import com.salvarez.miboveda.data.db.EntryEntity
import com.salvarez.miboveda.data.db.VaultSession
import com.salvarez.miboveda.data.repository.BackupRepositoryImpl
import com.salvarez.miboveda.data.repository.DekUnwrapper
import com.salvarez.miboveda.data.repository.VaultRepositoryImpl
import com.salvarez.miboveda.data.settings.VaultHeaderStore
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import java.io.File
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRepositoryImplTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val session = VaultSession(context)
    private val derivation = Argon2KeyDerivation()
    private val wrapper = AesGcmKeyWrapper()
    // A DataStore stays active for its file, so every test instance needs its own file.
    private val prefsFile = File(context.cacheDir, "backup_test_${UUID.randomUUID()}.preferences_pb")
    private val headerStore = VaultHeaderStore(PreferenceDataStoreFactory.create { prefsFile })
    private val unwrapper = DekUnwrapper(headerStore, derivation, wrapper)
    private val vault = VaultRepositoryImpl(headerStore, derivation, wrapper, unwrapper, session, VaultLockState())
    private val codec = BackupCodec()
    private val backups = BackupRepositoryImpl(context, unwrapper, derivation, session, codec)
    private val backupFile = File(context.cacheDir, "test_${UUID.randomUUID()}.backup")
    private val destination = Uri.fromFile(backupFile).toString()
    private val password = "a long enough password".toCharArray()

    @Before
    fun setUp() = cleanUp()

    @After
    fun tearDown() {
        session.close()
        cleanUp()
    }

    private suspend fun createVaultWithEntries() {
        vault.create(password.copyOf())
        session.entryDao().upsert(EntryEntity(0, "Bank", "ana", "s3cret-value", "https://bank.example", "notes", 1L))
        session.entryDao().upsert(EntryEntity(0, "Mail", "ana@example.com", "other-secret", "", "", 2L))
    }

    @Test
    fun theBackupOpensWithTheMasterPasswordAndHoldsEveryEntry() = runTest {
        createVaultWithEntries()

        assertEquals(Outcome.Success(Unit), backups.export(password.copyOf(), destination))

        val bytes = backupFile.readBytes()
        val header = codec.readHeader(bytes)
        val key = derivation.derive(password, header.salt, header.kdfParams)
        val entries = codec.decode(bytes, header, key)
        assertEquals(listOf("Bank", "Mail"), entries.map { it.title })
        assertEquals(listOf("s3cret-value", "other-secret"), entries.map { it.password })
    }

    @Test
    fun theFileContainsNoReadableData() = runTest {
        createVaultWithEntries()
        backups.export(password.copyOf(), destination)

        val text = String(backupFile.readBytes(), Charsets.ISO_8859_1)
        assertFalse(text.contains("s3cret-value"))
        assertFalse(text.contains("Bank"))
    }

    @Test
    fun aWrongPasswordWritesNothing() = runTest {
        createVaultWithEntries()

        val result = backups.export("another wrong password".toCharArray(), destination)

        assertEquals(Outcome.Failure(VaultError.WrongMasterPassword), result)
        assertFalse(backupFile.exists())
    }

    @Test
    fun aLockedVaultWritesNothing() = runTest {
        createVaultWithEntries()
        session.close()

        assertEquals(Outcome.Failure(VaultError.VaultLocked), backups.export(password.copyOf(), destination))
        assertFalse(backupFile.exists())
    }

    @Test
    fun exportingTwiceReplacesTheFile() = runTest {
        createVaultWithEntries()
        backups.export(password.copyOf(), destination)
        val first = backupFile.length()
        backups.export(password.copyOf(), destination)

        assertTrue(backupFile.exists())
        assertEquals(first, backupFile.length())
    }

    private fun cleanUp() {
        backupFile.delete()
        prefsFile.delete()
        val db = session.databaseFile()
        listOf("", "-wal", "-shm", "-journal").forEach { File(db.path + it).delete() }
    }
}
