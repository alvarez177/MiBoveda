package com.salvarez.miboveda.data.crypto

/** Turns a master password into a key. Kept as an interface because the real one needs native code. */
interface KeyDerivation {
    /** Returns a [KEY_SIZE_BYTES]-byte key. The caller owns [password] and may clear it afterwards. */
    fun derive(password: CharArray, salt: ByteArray, params: KdfParams): ByteArray
}
