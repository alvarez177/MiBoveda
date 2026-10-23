package com.salvarez.miboveda.data.db

import com.salvarez.miboveda.domain.model.PasswordEntry
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EntryMappingTest {
    private val entry = PasswordEntry(
        id = 7,
        title = "Bank",
        username = "ana",
        password = "s3cret-value",
        url = "https://bank.example",
        notes = "main account",
        updatedAt = Instant.ofEpochMilli(1_700_000_000_000),
    )

    @Test
    fun `entry survives a round trip through the entity`() {
        assertEquals(entry, entry.toEntity().toDomain())
    }

    @Test
    fun `entity toString does not leak the password`() {
        assertFalse(entry.toEntity().toString().contains("s3cret-value"))
    }
}
