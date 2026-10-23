package com.salvarez.miboveda.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.salvarez.miboveda.domain.model.PasswordEntry
import java.time.Instant

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val updatedAtMillis: Long,
) {
    // Keeps the secret out of logs and error messages.
    override fun toString(): String = "EntryEntity(id=$id)"
}

fun EntryEntity.toDomain() = PasswordEntry(
    id = id,
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    updatedAt = Instant.ofEpochMilli(updatedAtMillis),
)

fun PasswordEntry.toEntity() = EntryEntity(
    id = id,
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    updatedAtMillis = updatedAt.toEpochMilli(),
)
