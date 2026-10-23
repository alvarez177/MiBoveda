package com.salvarez.miboveda.data.crypto

import java.security.SecureRandom

const val KEY_SIZE_BYTES = 32
const val SALT_SIZE_BYTES = 16

private val secureRandom = SecureRandom()

fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also(secureRandom::nextBytes)
