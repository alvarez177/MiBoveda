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
) {
    private var pendingLock: Job? = null
    private var backgroundedAt: Long? = null

    fun onAppBackgrounded() {
        if (!vaultRepository.isUnlocked.value) return
        backgroundedAt = elapsedRealtimeMillis()
        pendingLock?.cancel()
        pendingLock = scope.launch {
            delay(observeTimeout().first().millis)
            lock()
        }
    }

    fun onAppForegrounded() {
        pendingLock?.cancel()
        val since = backgroundedAt ?: return
        backgroundedAt = null
        scope.launch {
            if (elapsedRealtimeMillis() - since >= observeTimeout().first().millis) lock()
        }
    }

    private suspend fun lock() {
        backgroundedAt = null
        clipboard.clear()
        lockVault()
    }
}
