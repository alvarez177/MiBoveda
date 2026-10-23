package com.salvarez.miboveda.data.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.nio.ByteBuffer
import java.nio.CharBuffer
import javax.inject.Inject

class Argon2KeyDerivation @Inject constructor() : KeyDerivation {
    private val argon2 = Argon2Kt()

    override fun derive(password: CharArray, salt: ByteArray, params: KdfParams): ByteArray {
        val passwordBytes = Charsets.UTF_8.encode(CharBuffer.wrap(password)).toByteArray()
        try {
            return argon2.hash(
                mode = Argon2Mode.ARGON2_ID,
                password = passwordBytes,
                salt = salt,
                tCostInIterations = params.iterations,
                mCostInKibibyte = params.memoryKib,
                parallelism = params.parallelism,
                hashLengthInBytes = KEY_SIZE_BYTES,
            ).rawHashAsByteArray()
        } finally {
            passwordBytes.fill(0)
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray =
        ByteArray(remaining()).also { get(it); clear(); array().fill(0) }
}
