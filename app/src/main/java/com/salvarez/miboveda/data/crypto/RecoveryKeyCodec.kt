package com.salvarez.miboveda.data.crypto

/**
 * Turns the 32 random bytes of a recovery key into text people can write down (Base32, RFC 4648 alphabet,
 * groups of four) and back. The alphabet has no 0, 1, 8 or 9, so it cannot be confused with O, I, B or g.
 */
object RecoveryKeyCodec {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val BITS_PER_CHAR = 5
    private const val GROUP_SIZE = 4

    fun encode(bytes: ByteArray): String {
        val chars = StringBuilder()
        var buffer = 0
        var bits = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8
            while (bits >= BITS_PER_CHAR) {
                chars.append(ALPHABET[(buffer shr (bits - BITS_PER_CHAR)) and 0x1F])
                bits -= BITS_PER_CHAR
            }
        }
        // Leftover bits are padded with zeros on the right.
        if (bits > 0) chars.append(ALPHABET[(buffer shl (BITS_PER_CHAR - bits)) and 0x1F])
        return chars.chunked(GROUP_SIZE).joinToString("-")
    }

    /** Accepts any case, with or without dashes and spaces. Returns null when [text] is not a valid recovery key. */
    fun decode(text: String): ByteArray? {
        val clean = text.uppercase().filterNot { it == '-' || it.isWhitespace() }
        if (clean.length != ENCODED_LENGTH) return null
        val bytes = ByteArray(KEY_SIZE_BYTES)
        var buffer = 0
        var bits = 0
        var index = 0
        for (char in clean) {
            val value = ALPHABET.indexOf(char)
            if (value < 0) return null
            buffer = (buffer shl BITS_PER_CHAR) or value
            bits += BITS_PER_CHAR
            if (bits >= 8) {
                bytes[index++] = ((buffer shr (bits - 8)) and 0xFF).toByte()
                bits -= 8
            }
        }
        // The four leftover bits must be the zero padding we wrote; anything else is a typo.
        return if ((buffer and ((1 shl bits) - 1)) == 0) bytes else null
    }

    private const val ENCODED_LENGTH = (KEY_SIZE_BYTES * 8 + BITS_PER_CHAR - 1) / BITS_PER_CHAR
}
