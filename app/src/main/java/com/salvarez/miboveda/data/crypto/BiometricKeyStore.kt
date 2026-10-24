package com.salvarez.miboveda.data.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed AES key that can only be used right after a strong biometric check.
 * [requireUserAuthentication] is only turned off by tests, which cannot touch a fingerprint sensor.
 */
class BiometricKeyStore(private val requireUserAuthentication: Boolean = true) {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    /** Replaces any previous key with a new one and returns a cipher ready to encrypt (once approved). */
    fun encryptCipher(): Cipher {
        deleteKey()
        generateKey()
        return Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
    }

    /** Throws [KeyPermanentlyInvalidatedException] when the enrolled biometrics changed since the key was made. */
    fun decryptCipher(iv: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        }

    fun deleteKey() {
        if (keyStore.containsAlias(ALIAS)) keyStore.deleteEntry(ALIAS)
    }

    private fun key(): SecretKey = keyStore.getKey(ALIAS, null) as? SecretKey
        ?: throw IllegalStateException("Biometric key does not exist")

    @Suppress("DEPRECATION")
    private fun generateKey() {
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BYTES * 8)
            .apply {
                if (requireUserAuthentication) {
                    setUserAuthenticationRequired(true)
                    setInvalidatedByBiometricEnrollment(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                    } else {
                        // -1 means: authenticate for every single use.
                        setUserAuthenticationValidityDurationSeconds(-1)
                    }
                }
            }
            .build()
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(spec)
            generateKey()
        }
    }

    companion object {
        /** Size of the IV Android Keystore picks for AES-GCM; it is stored in front of the ciphertext. */
        const val IV_SIZE_BYTES = 12

        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val ALIAS = "miboveda_biometric_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_SIZE_BITS = 128
    }
}
