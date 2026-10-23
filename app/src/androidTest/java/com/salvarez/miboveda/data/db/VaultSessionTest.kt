package com.salvarez.miboveda.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultSessionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val session = VaultSession(context)
    private val key = ByteArray(32) { it.toByte() }
    private val entry = EntryEntity(
        id = 0,
        title = "Bank",
        username = "ana",
        password = "s3cret-value",
        url = "",
        notes = "",
        updatedAtMillis = Instant.now().toEpochMilli(),
    )

    @Before
    fun setUp() = deleteDatabaseFiles()

    @After
    fun tearDown() {
        session.close()
        deleteDatabaseFiles()
    }

    @Test
    fun savedEntriesSurviveClosingAndReopeningWithTheSameKey() = runTest {
        session.open(key)
        session.entryDao().upsert(entry)
        session.close()

        session.open(key)
        val entries = session.entryDao().observeAll().first()
        assertEquals(listOf("Bank"), entries.map { it.title })
        assertEquals("s3cret-value", entries.single().password)
    }

    @Test
    fun openingWithAWrongKeyFailsAndLeavesTheVaultLocked() = runTest {
        session.open(key)
        session.entryDao().upsert(entry)
        session.close()

        assertTrue(runCatching { session.open(ByteArray(32) { 1 }) }.isFailure)
        assertFalse(session.isOpen)
    }

    @Test
    fun databaseFileDoesNotContainPlaintext() = runTest {
        session.open(key)
        session.entryDao().upsert(entry)
        session.close()

        val bytes = session.databaseFile().readBytes()
        assertFalse(String(bytes, Charsets.ISO_8859_1).startsWith("SQLite format 3"))
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("s3cret-value"))
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("Bank"))
    }

    @Test
    fun accessingTheDaoWhileLockedFails() {
        assertFalse(session.isOpen)
        assertThrows(IllegalStateException::class.java) { session.entryDao() }
    }

    @Test
    fun theCallerKeyIsNotCleared() = runTest {
        val before = key.copyOf()
        session.open(key)
        assertTrue(before.contentEquals(key))
    }

    private fun deleteDatabaseFiles() {
        val file = session.databaseFile()
        listOf("", "-wal", "-shm", "-journal").forEach { java.io.File(file.path + it).delete() }
    }
}
