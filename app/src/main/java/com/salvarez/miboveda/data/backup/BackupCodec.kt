package com.salvarez.miboveda.data.backup

import com.salvarez.miboveda.data.crypto.KdfParams
import com.salvarez.miboveda.data.crypto.secureRandomBytes
import com.salvarez.miboveda.domain.model.PasswordEntry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** The file is not a MiBoveda backup, is from a newer version, or is damaged. */
class BackupFormatException(message: String) : Exception(message)

/** Everything in the clear part of the file: what is needed to derive the key again. */
class BackupHeader(val kdfParams: KdfParams, val salt: ByteArray, internal val size: Int)

/**
 * Reads and writes the backup file format (see docs/BACKUP_FORMAT.md). The key is derived elsewhere; the
 * header is authenticated together with the data, so changing the KDF settings or the salt breaks the file.
 */
class BackupCodec {

    /** [key] must be a 256-bit key derived from the master password with [kdfParams] and [salt]. */
    fun encode(entries: List<PasswordEntry>, key: ByteArray, salt: ByteArray, kdfParams: KdfParams): ByteArray {
        val header = ByteArrayOutputStream().also { out ->
            DataOutputStream(out).apply {
                write(MAGIC)
                writeInt(FORMAT_VERSION)
                writeByte(KDF_ARGON2ID.toInt())
                writeInt(kdfParams.iterations)
                writeInt(kdfParams.memoryKib)
                writeInt(kdfParams.parallelism)
                writeByte(salt.size)
                write(salt)
            }
        }.toByteArray()
        val iv = secureRandomBytes(IV_SIZE_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_SIZE_BITS, iv))
            updateAAD(header)
        }
        val plain = serialize(entries)
        try {
            return header + iv + cipher.doFinal(plain)
        } finally {
            plain.fill(0)
        }
    }

    fun readHeader(bytes: ByteArray): BackupHeader {
        try {
            val input = DataInputStream(ByteArrayInputStream(bytes))
            val magic = ByteArray(MAGIC.size).also(input::readFully)
            if (!magic.contentEquals(MAGIC)) throw BackupFormatException("Not a MiBoveda backup")
            val version = input.readInt()
            if (version != FORMAT_VERSION) throw BackupFormatException("Unsupported backup version $version")
            if (input.readByte() != KDF_ARGON2ID) throw BackupFormatException("Unknown key derivation")
            val params = KdfParams(input.readInt(), input.readInt(), input.readInt())
            // A tampered file must not be able to ask for an absurd amount of memory or time.
            if (params.iterations !in 1..MAX_ITERATIONS || params.memoryKib !in MIN_MEMORY_KIB..MAX_MEMORY_KIB ||
                params.parallelism !in 1..MAX_PARALLELISM
            ) {
                throw BackupFormatException("Unreasonable key derivation settings")
            }
            val saltSize = input.readUnsignedByte()
            if (saltSize < MIN_SALT_SIZE) throw BackupFormatException("The salt is too short")
            val salt = ByteArray(saltSize).also(input::readFully)
            return BackupHeader(params, salt, size = bytes.size - input.available())
        } catch (e: EOFException) {
            throw BackupFormatException("The backup is truncated")
        }
    }

    /** Throws [javax.crypto.AEADBadTagException] when [key] is wrong or the file was modified. */
    fun decode(bytes: ByteArray, header: BackupHeader, key: ByteArray): List<PasswordEntry> {
        if (bytes.size < header.size + IV_SIZE_BYTES + TAG_SIZE_BITS / 8) throw BackupFormatException("The backup is truncated")
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_SIZE_BITS, bytes, header.size, IV_SIZE_BYTES))
            updateAAD(bytes, 0, header.size)
        }
        val plain = cipher.doFinal(bytes, header.size + IV_SIZE_BYTES, bytes.size - header.size - IV_SIZE_BYTES)
        try {
            return deserialize(plain)
        } finally {
            plain.fill(0)
        }
    }

    private fun serialize(entries: List<PasswordEntry>): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeInt(entries.size)
            entries.forEach { entry ->
                writeText(entry.title)
                writeText(entry.username)
                writeText(entry.password)
                writeText(entry.url)
                writeText(entry.notes)
                writeLong(entry.updatedAt.toEpochMilli())
            }
        }
        return out.toByteArray()
    }

    private fun deserialize(plain: ByteArray): List<PasswordEntry> {
        try {
            val input = DataInputStream(ByteArrayInputStream(plain))
            return List(input.readInt()) {
                PasswordEntry(
                    title = input.readText(),
                    username = input.readText(),
                    password = input.readText(),
                    url = input.readText(),
                    notes = input.readText(),
                    updatedAt = Instant.ofEpochMilli(input.readLong()),
                )
            }
        } catch (e: EOFException) {
            throw BackupFormatException("The backup content is damaged")
        } catch (e: NegativeArraySizeException) {
            throw BackupFormatException("The backup content is damaged")
        }
    }

    // writeUTF caps strings at 64 KiB, which long notes could exceed, so strings are length-prefixed UTF-8.
    private fun DataOutputStream.writeText(text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readText(): String {
        val size = readInt()
        if (size < 0 || size > available()) throw BackupFormatException("The backup content is damaged")
        return String(ByteArray(size).also(::readFully), Charsets.UTF_8)
    }

    private companion object {
        val MAGIC = "MIBOVEDA".toByteArray(Charsets.US_ASCII)
        const val FORMAT_VERSION = 1
        const val KDF_ARGON2ID: Byte = 1
        const val MAX_ITERATIONS = 20
        const val MIN_MEMORY_KIB = 8
        const val MAX_MEMORY_KIB = 262_144
        const val MAX_PARALLELISM = 16
        const val MIN_SALT_SIZE = 8
        const val IV_SIZE_BYTES = 12
        const val TAG_SIZE_BITS = 128
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
