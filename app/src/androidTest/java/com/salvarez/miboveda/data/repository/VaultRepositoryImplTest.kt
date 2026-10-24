package com.salvarez.miboveda.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.salvarez.miboveda.data.VaultLockState
import com.salvarez.miboveda.data.crypto.AesGcmKeyWrapper
import com.salvarez.miboveda.data.crypto.Argon2KeyDerivation
import com.salvarez.miboveda.data.crypto.KEY_SIZE_BYTES
import com.salvarez.miboveda.data.crypto.RecoveryKeyCodec
import com.salvarez.miboveda.data.crypto.secureRandomBytes
import com.salvarez.miboveda.data.db.EntryEntity
import com.salvarez.miboveda.data.db.VaultSession
import com.salvarez.miboveda.data.settings.VaultHeader
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

        assertTrue(repository.create(password) is Outcome.Success)

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

    @Test
    fun createReturnsARecoveryKeyThatIsStoredWrapped() = runTest {
        val key = (repository.create(password.copyOf()) as Outcome.Success).value

        assertEquals(13, key.value.split('-').size)
        assertNotNull(headerStore.read()!!.recoveryWrappedDek)
        assertTrue(repository.hasRecoveryKey.first())
        assertFalse(headerStore.read()!!.recoveryWrappedDek!!.contentEquals(key.value.toByteArray()))
    }

    @Test
    fun recoveringSetsANewPasswordAndKeepsTheData() = runTest {
        val key = (repository.create(password.copyOf()) as Outcome.Success).value
        session.entryDao().upsert(EntryEntity(0, "Bank", "ana", "s3cret", "", "", 1L))
        repository.lock()
        val newPassword = "a brand new password".toCharArray()

        assertEquals(Outcome.Success(Unit), repository.recover(key.value, newPassword.copyOf()))

        assertTrue(repository.isUnlocked.value)
        assertEquals(listOf("Bank"), session.entryDao().observeAll().first().map { it.title })
        repository.lock()
        assertEquals(Outcome.Failure(VaultError.WrongMasterPassword), repository.unlock(password.copyOf()))
        assertEquals(Outcome.Success(Unit), repository.unlock(newPassword))
    }

    @Test
    fun recoveringWithAWrongOrMalformedKeyFails() = runTest {
        repository.create(password.copyOf())
        repository.lock()
        val otherKey = RecoveryKeyCodec.encode(secureRandomBytes(KEY_SIZE_BYTES))

        assertEquals(Outcome.Failure(VaultError.InvalidRecoveryKey), repository.recover(otherKey, password.copyOf()))
        assertEquals(Outcome.Failure(VaultError.InvalidRecoveryKey), repository.recover("not a key", password.copyOf()))
        assertFalse(repository.isUnlocked.value)
    }

    @Test
    fun theRecoveryKeyStillWorksAfterRecovering() = runTest {
        val key = (repository.create(password.copyOf()) as Outcome.Success).value
        repository.lock()
        repository.recover(key.value, "a brand new password".toCharArray())
        repository.lock()

        assertEquals(Outcome.Success(Unit), repository.recover(key.value, "yet another password!".toCharArray()))
    }

    @Test
    fun generatingANewKeyInvalidatesTheOldOne() = runTest {
        val oldKey = (repository.create(password.copyOf()) as Outcome.Success).value
        val newKey = (repository.generateRecoveryKey(password.copyOf()) as Outcome.Success).value
        repository.lock()

        assertFalse(oldKey == newKey)
        assertEquals(
            Outcome.Failure(VaultError.InvalidRecoveryKey),
            repository.recover(oldKey.value, "a brand new password".toCharArray()),
        )
        assertEquals(Outcome.Success(Unit), repository.recover(newKey.value, "a brand new password".toCharArray()))
    }

    @Test
    fun generatingAKeyNeedsTheRightPassword() = runTest {
        repository.create(password.copyOf())

        assertEquals(
            Outcome.Failure(VaultError.WrongMasterPassword),
            repository.generateRecoveryKey("another wrong password".toCharArray()),
        )
    }

    @Test
    fun aVaultWithoutARecoveryKeyCanGetOneLater() = runTest {
        repository.create(password.copyOf())
        val header = headerStore.read()!!
        headerStore.save(VaultHeader(header.version, header.kdfParams, header.salt, header.wrappedDek, null))
        assertFalse(repository.hasRecoveryKey.first())
        repository.lock()

        val someKey = RecoveryKeyCodec.encode(secureRandomBytes(KEY_SIZE_BYTES))
        assertEquals(Outcome.Failure(VaultError.NoRecoveryKey), repository.recover(someKey, password.copyOf()))

        repository.unlock(password.copyOf())
        val key = (repository.generateRecoveryKey(password.copyOf()) as Outcome.Success).value
        repository.lock()
        assertEquals(Outcome.Success(Unit), repository.recover(key.value, "a brand new password".toCharArray()))
    }

    private fun cleanUp() {
        prefsFile.delete()
        val db = session.databaseFile()
        listOf("", "-wal", "-shm", "-journal").forEach { File(db.path + it).delete() }
    }
}
