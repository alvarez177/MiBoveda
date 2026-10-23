package com.salvarez.miboveda.domain.usecase

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveEntryTest {
    private val repository = FakeEntryRepository()
    private val saveEntry = SaveEntry(repository)

    @Test
    fun `rejects a blank title`() = runTest {
        val result = saveEntry(PasswordEntry(title = "   ", username = "ana", password = "x"))
        assertEquals(Outcome.Failure(VaultError.EntryTitleRequired), result)
        assertEquals(0, repository.current.size)
    }

    @Test
    fun `saves the entry with a trimmed title`() = runTest {
        saveEntry(PasswordEntry(title = "  Bank  ", username = "ana", password = "x"))
        assertEquals("Bank", repository.current.single().title)
    }
}
