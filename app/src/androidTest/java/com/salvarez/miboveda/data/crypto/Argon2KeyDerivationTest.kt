package com.salvarez.miboveda.data.crypto

import android.os.SystemClock
import android.util.Log
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class Argon2KeyDerivationTest {
    private val derivation = Argon2KeyDerivation()
    private val salt = ByteArray(SALT_SIZE_BYTES) { it.toByte() }
    private val params = KdfParams.DEFAULT

    @Test
    fun derivesAKeyOfTheExpectedSize() {
        assertEquals(KEY_SIZE_BYTES, derivation.derive("correct horse".toCharArray(), salt, params).size)
    }

    @Test
    fun sameInputsGiveTheSameKey() {
        val first = derivation.derive("correct horse".toCharArray(), salt, params)
        val second = derivation.derive("correct horse".toCharArray(), salt, params)
        assertArrayEquals(first, second)
    }

    @Test
    fun differentPasswordOrSaltGiveDifferentKeys() {
        val base = derivation.derive("correct horse".toCharArray(), salt, params)
        val otherPassword = derivation.derive("correct horsf".toCharArray(), salt, params)
        val otherSalt = derivation.derive("correct horse".toCharArray(), ByteArray(SALT_SIZE_BYTES) { 9 }, params)
        assertFalse(base.contentEquals(otherPassword))
        assertFalse(base.contentEquals(otherSalt))
    }

    @Test
    fun doesNotModifyTheCallerPassword() {
        val password = "correct horse".toCharArray()
        derivation.derive(password, salt, params)
        assertEquals("correct horse", String(password))
    }

    @Test
    fun logsDerivationTimeForCalibration() {
        val start = SystemClock.elapsedRealtime()
        derivation.derive("calibration".toCharArray(), salt, params)
        Log.i("KdfCalibration", "Argon2id $params took ${SystemClock.elapsedRealtime() - start} ms")
    }
}
