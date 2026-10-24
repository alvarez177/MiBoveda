package com.salvarez.miboveda.data.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RecoveryKeyCodecTest {
    @Test
    fun `a key survives encoding and decoding`() {
        repeat(50) {
            val bytes = secureRandomBytes(KEY_SIZE_BYTES)
            assertArrayEquals(bytes, RecoveryKeyCodec.decode(RecoveryKeyCodec.encode(bytes)))
        }
    }

    @Test
    fun `the text is made of thirteen groups of four letters or digits`() {
        val text = RecoveryKeyCodec.encode(secureRandomBytes(KEY_SIZE_BYTES))
        val groups = text.split('-')

        assertEquals(13, groups.size)
        assertEquals(true, groups.all { it.length == 4 && it.all { c -> c in 'A'..'Z' || c in '2'..'7' } })
    }

    @Test
    fun `decoding ignores case, dashes and spaces`() {
        val bytes = secureRandomBytes(KEY_SIZE_BYTES)
        val text = RecoveryKeyCodec.encode(bytes)

        assertArrayEquals(bytes, RecoveryKeyCodec.decode(text.lowercase()))
        assertArrayEquals(bytes, RecoveryKeyCodec.decode(text.replace('-', ' ')))
        assertArrayEquals(bytes, RecoveryKeyCodec.decode(text.replace("-", "")))
    }

    @Test
    fun `decoding rejects characters outside the alphabet`() {
        val text = RecoveryKeyCodec.encode(secureRandomBytes(KEY_SIZE_BYTES))
        assertNull(RecoveryKeyCodec.decode("0" + text.drop(1)))
        assertNull(RecoveryKeyCodec.decode(text.dropLast(1) + "1"))
    }

    @Test
    fun `decoding rejects the wrong length`() {
        val text = RecoveryKeyCodec.encode(secureRandomBytes(KEY_SIZE_BYTES))
        assertNull(RecoveryKeyCodec.decode(text.dropLast(1)))
        assertNull(RecoveryKeyCodec.decode(text + "A"))
        assertNull(RecoveryKeyCodec.decode(""))
    }

    @Test
    fun `decoding rejects a last character whose padding bits are not zero`() {
        val text = RecoveryKeyCodec.encode(ByteArray(KEY_SIZE_BYTES))
        // The last character only carries one real bit, so only A and Q can be valid.
        assertNotNull(RecoveryKeyCodec.decode(text.dropLast(1) + "A"))
        assertNotNull(RecoveryKeyCodec.decode(text.dropLast(1) + "Q"))
        assertNull(RecoveryKeyCodec.decode(text.dropLast(1) + "B"))
    }
}
