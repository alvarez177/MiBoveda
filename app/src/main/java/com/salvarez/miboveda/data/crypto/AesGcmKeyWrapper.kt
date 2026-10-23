package com.salvarez.miboveda.data.crypto

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * Wraps a key with another key using AES-256-GCM from the platform JCA.
 * Output layout: 12-byte IV followed by ciphertext and 16-byte tag.
 */
class AesGcmKeyWrapper @Inject constructor() {

    fun wrap(wrappingKey: ByteArray, keyToWrap: ByteArray): ByteArray {
        val iv = secureRandomBytes(IV_SIZE_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(wrappingKey, ALGORITHM), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return iv + cipher.doFinal(keyToWrap)
    }

    /** Throws [javax.crypto.AEADBadTagException] when [wrappingKey] is wrong or [wrapped] was modified. */
    fun unwrap(wrappingKey: ByteArray, wrapped: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_SIZE_BITS, wrapped, 0, IV_SIZE_BYTES)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(wrappingKey, ALGORITHM), spec)
        return cipher.doFinal(wrapped, IV_SIZE_BYTES, wrapped.size - IV_SIZE_BYTES)
    }

    private companion object {
        const val ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE_BYTES = 12
        const val TAG_SIZE_BITS = 128
    }
}
