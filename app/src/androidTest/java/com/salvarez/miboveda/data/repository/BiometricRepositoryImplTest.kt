package com.salvarez.miboveda.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.salvarez.miboveda.data.VaultLockState
import com.salvarez.miboveda.data.crypto.AesGcmKeyWrapper
import com.salvarez.miboveda.data.crypto.Argon2KeyDerivation
import com.salvarez.miboveda.data.crypto.BiometricAvailability
import com.salvarez.miboveda.data.crypto.BiometricKeyStore
import com.salvarez.miboveda.data.db.EntryEntity
import com.salvarez.miboveda.data.db.VaultSession
import com.salvarez.miboveda.data.settings.BiometricBlobStore
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Runs the whole wrap/unwrap flow with a Keystore key that does not need a fingerprint and a fake
 * availability check (the prompt itself cannot be driven from a test). The real key requirements
 * are covered by manual testing.
 */
class BiometricRepositoryImplTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val session = VaultSession(context)
    private val lockState = VaultLockState()
    private val derivation = Argon2KeyDerivation()
    private val wrapper = AesGcmKeyWrapper()
    // A DataStore stays active for its file, so every test instance needs its own file.
    private val prefsFile = File(context.cacheDir, "bio_repo_test_${UUID.randomUUID()}.preferences_pb")
    private val dataStore = PreferenceDataStoreFactory.create { prefsFile }
    private val headerStore = VaultHeaderStore(dataStore)
    private val blobStore = BiometricBlobStore(dataStore)
    private val unwrapper = DekUnwrapper(headerStore, derivation, wrapper)
    private val keyStore = BiometricKeyStore(requireUserAuthentication = false)
    private val vault = VaultRepositoryImpl(headerStore, derivation, wrapper, unwrapper, session, lockState)
    private val biometrics = BiometricRepositoryImpl(BiometricAvailability { true }, keyStore, blobStore, unwrapper, session, lockState)
    private val password = "a long enough password".toCharArray()

    @Before
    fun setUp() = cleanUp()

    @After
    fun tearDown() {
        session.close()
        cleanUp()
    }

    private suspend fun enroll() {
        val cipher = (biometrics.prepareEnrollment(password.copyOf()) as Outcome.Success).value
        assertEquals(Outcome.Success(Unit), biometrics.completeEnrollment(cipher, password.copyOf()))
    }

    @Test
    fun enrollingThenUnlockingReopensTheSameVault() = runTest {
        vault.create(password.copyOf())
        session.entryDao().upsert(EntryEntity(0, "Bank", "ana", "s3cret", "", "", 1L))
        enroll()
        assertTrue(blobStore.isEnabled.first())
        vault.lock()
        assertFalse(lockState.isUnlocked.value)

        val cipher = (biometrics.prepareUnlock() as Outcome.Success).value
        assertEquals(Outcome.Success(Unit), biometrics.unlock(cipher))

        assertTrue(lockState.isUnlocked.value)
        assertEquals(listOf("Bank"), session.entryDao().observeAll().first().map { it.title })
    }

    @Test
    fun enrollingWithAWrongPasswordFailsBeforeTouchingAnything() = runTest {
        vault.create(password.copyOf())

        val result = biometrics.prepareEnrollment("another wrong password".toCharArray())

        assertEquals(Outcome.Failure(VaultError.WrongMasterPassword), result)
        assertNull(blobStore.read())
    }

    @Test
    fun theStoredBlobDoesNotContainTheRawKey() = runTest {
        vault.create(password.copyOf())
        val dek = (unwrapper.unwrap(password.copyOf()) as Outcome.Success).value
        enroll()

        val blob = blobStore.read()
        assertNotNull(blob)
        assertFalse(String(blob!!, Charsets.ISO_8859_1).contains(String(dek, Charsets.ISO_8859_1)))
        assertEquals(BiometricKeyStore.IV_SIZE_BYTES + dek.size + 16, blob.size)
    }

    @Test
    fun disablingRemovesTheStoredKeyAndTheMasterPasswordStillWorks() = runTest {
        vault.create(password.copyOf())
        enroll()
        vault.lock()

        biometrics.disable()

        assertFalse(blobStore.isEnabled.first())
        assertEquals(Outcome.Failure(VaultError.BiometricUnavailable), biometrics.prepareUnlock())
        assertEquals(Outcome.Success(Unit), vault.unlock(password.copyOf()))
    }

    @Test
    fun unlockingWithoutEnrollingFails() = runTest {
        vault.create(password.copyOf())
        vault.lock()

        assertEquals(Outcome.Failure(VaultError.BiometricUnavailable), biometrics.prepareUnlock())
    }

    private fun cleanUp() {
        keyStore.deleteKey()
        prefsFile.delete()
        val db = session.databaseFile()
        listOf("", "-wal", "-shm", "-journal").forEach { File(db.path + it).delete() }
    }
}
