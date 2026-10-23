package com.salvarez.miboveda.domain.model

import java.time.Instant

data class PasswordEntry(
    val id: Long = NEW_ID,
    val title: String,
    val username: String,
    val password: String,
    val url: String = "",
    val notes: String = "",
    val updatedAt: Instant = Instant.now(),
) {
    // Keeps the secret out of logs and error messages.
    override fun toString(): String = "PasswordEntry(id=$id, title=$title)"

    companion object {
        const val NEW_ID = 0L
    }
}
