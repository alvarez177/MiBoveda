package com.salvarez.miboveda.presentation.lock

/**
 * Opening a system screen (the file picker, for example) sends the app to the background, which would
 * lock the vault in the middle of what the user is doing. Calling this just before gives that one trip a
 * grace period; it is still bounded, so a user who wanders off is locked out anyway.
 */
fun interface ExternalActivityGuard {
    fun allowExternalActivity()
}
