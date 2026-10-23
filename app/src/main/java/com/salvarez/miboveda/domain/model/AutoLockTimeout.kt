package com.salvarez.miboveda.domain.model

/** How long the app may stay in the background before the vault locks itself. */
enum class AutoLockTimeout(val millis: Long) {
    IMMEDIATE(0),
    THIRTY_SECONDS(30_000),
    ONE_MINUTE(60_000),
    FIVE_MINUTES(300_000),
    ;

    companion object {
        val DEFAULT = IMMEDIATE
    }
}
