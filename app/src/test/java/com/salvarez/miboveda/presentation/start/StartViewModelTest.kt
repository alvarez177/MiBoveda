package com.salvarez.miboveda.presentation.start

import com.salvarez.miboveda.domain.Outcome
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.usecase.FakeVaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun stateFor(repository: FakeVaultRepository) = StartViewModel(repository).state.value

    @Test
    fun `no vault goes to onboarding`() =
        assertEquals(StartState.NoVault, stateFor(FakeVaultRepository(existsResult = Outcome.Success(false))))

    @Test
    fun `existing locked vault goes to unlock`() =
        assertEquals(StartState.VaultLocked, stateFor(FakeVaultRepository(existsResult = Outcome.Success(true))))

    @Test
    fun `existing unlocked vault skips unlock`() = assertEquals(
        StartState.VaultUnlocked,
        stateFor(FakeVaultRepository(existsResult = Outcome.Success(true), unlocked = true)),
    )

    @Test
    fun `storage errors are reported`() = assertEquals(
        StartState.Failed(VaultError.StorageFailure),
        stateFor(FakeVaultRepository(existsResult = Outcome.Failure(VaultError.StorageFailure))),
    )
}
