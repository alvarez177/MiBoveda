package com.salvarez.miboveda.data.backup

import com.salvarez.miboveda.data.crypto.KEY_SIZE_BYTES
import com.salvarez.miboveda.data.crypto.KdfParams
import com.salvarez.miboveda.data.crypto.SALT_SIZE_BYTES
import com.salvarez.miboveda.data.crypto.secureRandomBytes
import com.salvarez.miboveda.domain.model.PasswordEntry
import java.time.Instant
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    private val codec = BackupCodec()
    private val key = secureRandomBytes(KEY_SIZE_BYTES)
    private val salt = secureRandomBytes(SALT_SIZE_BYTES)
    private val params = KdfParams(iterations = 3, memoryKib = 65_536, parallelism = 1)
    private val entries = listOf(
        PasswordEntry(id = 1, title = "Banco ñandú", username = "ana", password = "s3cret-€-密码", url = "https://banco.example", notes = "x".repeat(100_000), updatedAt = Instant.ofEpochMilli(1_700_000_000_123)),
        PasswordEntry(id = 2, title = "Correo", username = "", password = "p", url = "", notes = "", updatedAt = Instant.ofEpochMilli(5)),
    )

    private fun encode() = codec.encode(entries, key, salt, params)

    private fun roundTrip(bytes: ByteArray, withKey: ByteArray = key) = codec.decode(bytes, codec.readHeader(bytes), withKey)

    private fun List<PasswordEntry>.withoutIds() = map { it.copy(id = PasswordEntry.NEW_ID) }

    @Test
    fun `entries survive encoding and decoding`() {
        assertEquals(entries.withoutIds(), roundTrip(encode()))
    }

    @Test
    fun `an empty vault produces a valid backup`() {
        val bytes = codec.encode(emptyList(), key, salt, params)
        assertEquals(emptyList<PasswordEntry>(), roundTrip(bytes))
    }

    @Test
    fun `the header exposes the key derivation settings`() {
        val header = codec.readHeader(encode())
        assertEquals(params, header.kdfParams)
        assertArrayEquals(salt, header.salt)
    }

    @Test
    fun `the file contains no readable data`() {
        val text = String(encode(), Charsets.ISO_8859_1)
        assertFalse(text.contains("s3cret"))
        assertFalse(text.contains("Banco"))
        assertFalse(text.contains("banco.example"))
    }

    @Test
    fun `two backups of the same data differ`() {
        assertFalse(encode().contentEquals(encode()))
    }

    @Test
    fun `a wrong key cannot open the backup`() {
        assertThrows(AEADBadTagException::class.java) { roundTrip(encode(), secureRandomBytes(KEY_SIZE_BYTES)) }
    }

    @Test
    fun `changing the data is detected`() {
        val bytes = encode()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        assertThrows(AEADBadTagException::class.java) { roundTrip(bytes) }
    }

    @Test
    fun `changing the key derivation settings in the header is detected`() {
        val bytes = encode()
        // Byte 16 is the lowest byte of the iterations count (8 magic + 4 version + 1 kdf id + 4 iterations - 1).
        bytes[16] = (bytes[16].toInt() xor 1).toByte()
        assertThrows(AEADBadTagException::class.java) { roundTrip(bytes) }
    }

    @Test
    fun `a file that is not a backup is rejected`() {
        assertThrows(BackupFormatException::class.java) { codec.readHeader("this is not a backup file".toByteArray()) }
        assertThrows(BackupFormatException::class.java) { codec.readHeader(ByteArray(0)) }
    }

    @Test
    fun `an unknown version is rejected`() {
        val bytes = encode()
        bytes[11] = 9
        assertThrows(BackupFormatException::class.java) { codec.readHeader(bytes) }
    }

    @Test
    fun `a truncated file is rejected`() {
        val bytes = encode()
        assertThrows(BackupFormatException::class.java) { codec.readHeader(bytes.copyOf(20)) }
        assertThrows(BackupFormatException::class.java) { roundTrip(bytes.copyOf(codec.readHeader(bytes).size + 5)) }
    }

    @Test
    fun `a header asking for absurd key derivation costs is rejected before any work is done`() {
        listOf(
            KdfParams(iterations = 1_000, memoryKib = 65_536, parallelism = 1),
            KdfParams(iterations = 3, memoryKib = 4_000_000, parallelism = 1),
            KdfParams(iterations = 3, memoryKib = 65_536, parallelism = 500),
            KdfParams(iterations = 0, memoryKib = 65_536, parallelism = 1),
        ).forEach { bad ->
            val bytes = codec.encode(entries, key, salt, bad)
            assertThrows(BackupFormatException::class.java) { codec.readHeader(bytes) }
        }
    }

    @Test
    fun `a very short salt is rejected`() {
        val bytes = codec.encode(entries, key, ByteArray(2), params)
        assertThrows(BackupFormatException::class.java) { codec.readHeader(bytes) }
    }
}
