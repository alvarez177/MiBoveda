package com.salvarez.miboveda.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.salvarez.miboveda.data.VaultLockState
import com.salvarez.miboveda.data.crypto.AesGcmKeyWrapper
import com.salvarez.miboveda.data.crypto.Argon2KeyDerivation
import com.salvarez.miboveda.data.db.EntryEntity
import com.salvarez.miboveda.data.db.VaultSession
import com.salvarez.miboveda.data.settings.VaultHeaderStore
import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultRepositoryImplTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val session = VaultSession(context)
    private val derivation = Argon2KeyDerivation()
    private val wrapper = AesGcmKeyWrapper()
    // A DataStore stays active for its file, so every test instance needs its own file.
    private val prefsFile = File(context.cacheDir, "vault_repo_test_${UUID.randomUUID()}.preferences_pb")
    private val headerStore = VaultHeaderStore(PreferenceDataStoreFactory.create { prefsFile })
    private val repository = VaultRepositoryImpl(
        headerStore, derivation, wrapper, DekUnwrapper(headerStore, derivation, wrapper), session, VaultLockState(),
    )
    private val password = "a long enough password".toCharArray()

    @Before
    fun setUp() = cleanUp()

    @After
    fun tearDown() {
        session.close()
        cleanUp()
    }

    @Test
    fun createLeavesAnUnlockedVaultAndStoresAHeader() = runTest {
        assertEquals(Outcome.Success(false), repository.exists())

        assertEquals(Outcome.Success(Unit), repository.create(password))

        assertEquals(Outcome.Success(true), repository.exists())
        assertTrue(repository.isUnlocked.value)
        assertTrue(session.isOpen)
    }

    @Test
    fun theMasterPasswordCanRecoverTheKeyThatOpensTheDatabase() = runTest {
        repository.create(password)
        session.close()

        val header = headerStore.read()
        assertNotNull(header)
        val kek = derivation.derive(password, header!!.salt, header.kdfParams)
        val dek = wrapper.unwrap(kek, header.wrappedDek)

        session.open(dek)
        assertTrue(session.isOpen)
    }

    @Test
    fun aWrongPasswordCannotUnwrapTheKey() = runTest {
        repository.create(password)
        val header = headerStore.read()!!
        val kek = derivation.derive("another wrong password".toCharArray(), header.salt, header.kdfParams)

        val failed = runCatching { wrapper.unwrap(kek, header.wrappedDek) }.isFailure
        assertTrue(failed)
    }

    @Test
    fun creatingTwiceFails() = runTest {
        repository.create(password)
        assertEquals(Outcome.Failure(VaultError.VaultAlreadyExists), repository.create(password))
    }

    @Test
    fun theCallerPasswordIsNotModified() = runTest {
        repository.create(password)
        assertEquals("a long enough password", String(password))
        assertFalse(password.all { it == '\u0000' })
    }

    @Test
    fun unlockingAfterLockingReopensTheSameData() = runTest {
        repository.create(password)
        session.entryDao().upsert(
            EntryEntity(0, "Bank", "ana", "s3cret", "", "", 1L),
        )
        repository.lock()
        assertFalse(repository.isUnlocked.value)
        assertFalse(session.isOpen)

        assertEquals(Outcome.Success(Unit), repository.unlock(password.copyOf()))

        assertTrue(repository.isUnlocked.value)
        assertEquals(listOf("Bank"), session.entryDao().observeAll().first().map { it.title })
    }

    @Test
    fun unlockingWithAWrongPasswordFailsAndStaysLocked() = runTest {
        repository.create(password)
        repository.lock()

        val result = repository.unlock("another wrong password".toCharArray())

        assertEquals(Outcome.Failure(VaultError.WrongMasterPassword), result)
        assertFalse(repository.isUnlocked.value)
        assertFalse(session.isOpen)
    }

    @Test
    fun unlockingWithoutAVaultFails() = runTest {
        assertEquals(Outcome.Failure(VaultError.VaultNotCreated), repository.unlock(password))
    }

    private fun cleanUp() {
        prefsFile.delete()
        val db = session.databaseFile()
        listOf("", "-wal", "-shm", "-journal").forEach { File(db.path + it).delete() }
    }
}
