package com.salvarez.miboveda.data.crypto

/** Argon2id cost parameters. They are stored with the vault so they can change in the future. */
data class KdfParams(
    val iterations: Int,
    val memoryKib: Int,
    val parallelism: Int,
) {
    companion object {
        // OWASP-recommended Argon2id profile: 19 MiB, 2 iterations is the minimum; we use more memory.
        val DEFAULT = KdfParams(iterations = 3, memoryKib = 65_536, parallelism = 1)
    }
}
