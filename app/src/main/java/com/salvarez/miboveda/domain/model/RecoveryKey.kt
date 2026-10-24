package com.salvarez.miboveda.domain.model

/** The text the user must keep safe, e.g. `ABCD-EFGH-...`. It is shown once and never stored in clear. */
@JvmInline
value class RecoveryKey(val value: String) {
    // Keeps the secret out of logs.
    override fun toString(): String = "RecoveryKey"
}
