package com.salvarez.miboveda.presentation.lock

import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.usecase.FakeSettingsRepository
import com.salvarez.miboveda.domain.usecase.FakeVaultRepository
import com.salvarez.miboveda.domain.usecase.LockVault
import com.salvarez.miboveda.domain.usecase.ObserveAutoLockTimeout
import com.salvarez.miboveda.presentation.common.SensitiveClipboard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoLockControllerTest {
    private class FakeClipboard : SensitiveClipboard {
        var clears = 0
        override fun copy(label: String, text: String) = Unit
        override fun clear() {
            clears++
        }
    }

    private val scope = TestScope()
    private var now = 0L
    private val clipboard = FakeClipboard()

    private fun controller(repository: FakeVaultRepository, timeout: AutoLockTimeout) = AutoLockController(
        vaultRepository = repository,
        lockVault = LockVault(repository),
        observeTimeout = ObserveAutoLockTimeout(FakeSettingsRepository(timeout)),
        clipboard = clipboard,
        scope = scope,
        elapsedRealtimeMillis = { now },
    )

    @Test
    fun `immediate timeout locks as soon as the app goes to the background`() {
        val repository = FakeVaultRepository(unlocked = true)
        controller(repository, AutoLockTimeout.IMMEDIATE).onAppBackgrounded()
        scope.runCurrent()

        assertFalse(repository.isUnlocked.value)
        assertEquals(1, clipboard.clears)
    }

    @Test
    fun `a timeout locks only once it has elapsed`() {
        val repository = FakeVaultRepository(unlocked = true)
        controller(repository, AutoLockTimeout.THIRTY_SECONDS).onAppBackgrounded()

        scope.advanceTimeBy(29_000)
        scope.runCurrent()
        assertTrue(repository.isUnlocked.value)

        scope.advanceTimeBy(1_000)
        scope.runCurrent()
        assertFalse(repository.isUnlocked.value)
    }

    @Test
    fun `coming back in time cancels the pending lock`() {
        val repository = FakeVaultRepository(unlocked = true)
        val controller = controller(repository, AutoLockTimeout.THIRTY_SECONDS)
        controller.onAppBackgrounded()
        scope.advanceTimeBy(10_000)
        now += 10_000
        controller.onAppForegrounded()
        scope.advanceTimeBy(60_000)
        scope.runCurrent()

        assertTrue(repository.isUnlocked.value)
        assertEquals(0, repository.lockCalls)
    }

    @Test
    fun `returning after the timeout locks even if the timer never fired`() {
        val repository = FakeVaultRepository(unlocked = true)
        val controller = controller(repository, AutoLockTimeout.THIRTY_SECONDS)
        controller.onAppBackgrounded()
        // The process was frozen: the clock moved on but the coroutine timer did not.
        now += 45_000
        controller.onAppForegrounded()
        scope.runCurrent()

        assertFalse(repository.isUnlocked.value)
    }

    @Test
    fun `nothing happens when the vault is already locked`() {
        val repository = FakeVaultRepository(unlocked = false)
        val controller = controller(repository, AutoLockTimeout.IMMEDIATE)
        controller.onAppBackgrounded()
        scope.runCurrent()
        controller.onAppForegrounded()
        scope.runCurrent()

        assertEquals(0, repository.lockCalls)
        assertEquals(0, clipboard.clears)
    }

    @Test
    fun `a trip to a system screen gets a grace period even with an immediate timeout`() {
        val repository = FakeVaultRepository(unlocked = true)
        val controller = controller(repository, AutoLockTimeout.IMMEDIATE)
        controller.allowExternalActivity()
        controller.onAppBackgrounded()

        scope.advanceTimeBy(AutoLockController.EXTERNAL_ACTIVITY_GRACE_MILLIS - 1_000)
        scope.runCurrent()
        assertTrue(repository.isUnlocked.value)

        scope.advanceTimeBy(1_000)
        scope.runCurrent()
        assertFalse(repository.isUnlocked.value)
    }

    @Test
    fun `coming back from the system screen in time keeps the vault open`() {
        val repository = FakeVaultRepository(unlocked = true)
        val controller = controller(repository, AutoLockTimeout.IMMEDIATE)
        controller.allowExternalActivity()
        controller.onAppBackgrounded()
        scope.runCurrent()
        now += 20_000
        controller.onAppForegrounded()
        scope.runCurrent()

        assertTrue(repository.isUnlocked.value)
    }

    @Test
    fun `staying away too long from the system screen still locks`() {
        val repository = FakeVaultRepository(unlocked = true)
        val controller = controller(repository, AutoLockTimeout.IMMEDIATE)
        controller.allowExternalActivity()
        controller.onAppBackgrounded()
        scope.runCurrent()
        now += AutoLockController.EXTERNAL_ACTIVITY_GRACE_MILLIS + 1
        controller.onAppForegrounded()
        scope.runCurrent()

        assertFalse(repository.isUnlocked.value)
    }

    @Test
    fun `the grace period is only for one trip`() {
        val repository = FakeVaultRepository(unlocked = true)
        val controller = controller(repository, AutoLockTimeout.IMMEDIATE)
        controller.allowExternalActivity()
        controller.onAppBackgrounded()
        scope.runCurrent()
        controller.onAppForegrounded()
        scope.runCurrent()

        controller.onAppBackgrounded()
        scope.runCurrent()

        assertFalse(repository.isUnlocked.value)
    }

    @Test
    fun `the grace period is dropped if the app never left`() {
        val repository = FakeVaultRepository(unlocked = true)
        val controller = controller(repository, AutoLockTimeout.IMMEDIATE)
        controller.allowExternalActivity()
        controller.onAppForegrounded()
        controller.onAppBackgrounded()
        scope.runCurrent()

        assertFalse(repository.isUnlocked.value)
    }
}
