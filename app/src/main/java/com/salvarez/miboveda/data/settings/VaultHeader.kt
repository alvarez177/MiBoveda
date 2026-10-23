package com.salvarez.miboveda.data.settings

import com.salvarez.miboveda.data.crypto.KdfParams

/** What is needed to unlock the vault: KDF settings, salt and the database key wrapped with the master-password key. */
class VaultHeader(
    val version: Int,
    val kdfParams: KdfParams,
    val salt: ByteArray,
    val wrappedDek: ByteArray,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
