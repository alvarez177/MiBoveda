package com.salvarez.miboveda.presentation.lock

import com.salvarez.miboveda.domain.repository.VaultRepository
import com.salvarez.miboveda.domain.usecase.LockVault
import com.salvarez.miboveda.domain.usecase.ObserveAutoLockTimeout
import com.salvarez.miboveda.presentation.common.SensitiveClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Locks the vault when the app has been in the background for the configured time.
 *
 * Two safeguards work together: a timer that fires while the process is alive, and a check against
 * a monotonic clock when the app returns, in case the process was frozen and the timer never fired.
 */
class AutoLockController(
    private val vaultRepository: VaultRepository,
    private val lockVault: LockVault,
    private val observeTimeout: ObserveAutoLockTimeout,
    private val clipboard: SensitiveClipboard,
    private val scope: CoroutineScope,
    private val elapsedRealtimeMillis: () -> Long,
) : ExternalActivityGuard {
    private var pendingLock: Job? = null
    private var backgroundedAt: Long? = null
    private var lockAfterMillis: Long? = null
    private var externalActivityExpected = false

    override fun allowExternalActivity() {
        externalActivityExpected = true
    }

    fun onAppBackgrounded() {
        if (!vaultRepository.isUnlocked.value) return
        backgroundedAt = elapsedRealtimeMillis()
        val withGrace = externalActivityExpected
        externalActivityExpected = false
        pendingLock?.cancel()
        pendingLock = scope.launch {
            val configured = observeTimeout().first().millis
            val waitMillis = if (withGrace) maxOf(configured, EXTERNAL_ACTIVITY_GRACE_MILLIS) else configured
            lockAfterMillis = waitMillis
            delay(waitMillis)
            lock()
        }
    }

    fun onAppForegrounded() {
        externalActivityExpected = false
        pendingLock?.cancel()
        val since = backgroundedAt ?: return
        val knownWait = lockAfterMillis
        backgroundedAt = null
        lockAfterMillis = null
        scope.launch {
            val waitMillis = knownWait ?: observeTimeout().first().millis
            if (elapsedRealtimeMillis() - since >= waitMillis) lock()
        }
    }

    private suspend fun lock() {
        backgroundedAt = null
        lockAfterMillis = null
        clipboard.clear()
        lockVault()
    }

    companion object {
        const val EXTERNAL_ACTIVITY_GRACE_MILLIS = 120_000L
    }
}
