package com.salvarez.miboveda.domain

import com.salvarez.miboveda.domain.model.PasswordEntry
import org.junit.Assert.assertFalse
import org.junit.Test

class PasswordEntryTest {
    @Test
    fun `toString does not leak the password or username`() {
        val entry = PasswordEntry(title = "Bank", username = "ana", password = "s3cret-value")
        val text = entry.toString()
        assertFalse(text.contains("s3cret-value"))
        assertFalse(text.contains("ana"))
    }
}
