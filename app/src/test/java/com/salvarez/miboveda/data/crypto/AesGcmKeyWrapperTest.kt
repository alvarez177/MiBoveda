package com.salvarez.miboveda.data.crypto

import javax.crypto.AEADBadTagException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

class AesGcmKeyWrapperTest {
    private val wrapper = AesGcmKeyWrapper()

    @Test
    fun `unwrap returns the original key`() {
        val kek = secureRandomBytes(KEY_SIZE_BYTES)
        val dek = secureRandomBytes(KEY_SIZE_BYTES)
        assertArrayEquals(dek, wrapper.unwrap(kek, wrapper.wrap(kek, dek)))
    }

    @Test(expected = AEADBadTagException::class)
    fun `unwrap with a wrong key fails`() {
        val wrapped = wrapper.wrap(secureRandomBytes(KEY_SIZE_BYTES), secureRandomBytes(KEY_SIZE_BYTES))
        wrapper.unwrap(secureRandomBytes(KEY_SIZE_BYTES), wrapped)
    }

    @Test(expected = AEADBadTagException::class)
    fun `unwrap detects tampering`() {
        val kek = secureRandomBytes(KEY_SIZE_BYTES)
        val wrapped = wrapper.wrap(kek, secureRandomBytes(KEY_SIZE_BYTES))
        wrapped[wrapped.lastIndex] = (wrapped.last().toInt() xor 1).toByte()
        wrapper.unwrap(kek, wrapped)
    }

    @Test
    fun `wrapping twice uses a different IV`() {
        val kek = secureRandomBytes(KEY_SIZE_BYTES)
        val dek = secureRandomBytes(KEY_SIZE_BYTES)
        assertFalse(wrapper.wrap(kek, dek).contentEquals(wrapper.wrap(kek, dek)))
    }

    @Test
    fun `random bytes have the requested size and differ`() {
        assertEquals(KEY_SIZE_BYTES, secureRandomBytes(KEY_SIZE_BYTES).size)
        assertFalse(secureRandomBytes(KEY_SIZE_BYTES).contentEquals(secureRandomBytes(KEY_SIZE_BYTES)))
    }
}
