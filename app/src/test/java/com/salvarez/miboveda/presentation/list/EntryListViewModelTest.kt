package com.salvarez.miboveda.presentation.list

import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.usecase.FakeEntryRepository
import com.salvarez.miboveda.domain.usecase.ObserveEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryListViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `exposes the stored entries`() = runTest {
        val repository = FakeEntryRepository(listOf(PasswordEntry(id = 1, title = "Bank", username = "ana", password = "x")))
        val vm = EntryListViewModel(ObserveEntries(repository))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }

        assertFalse(vm.state.value.isLoading)
        assertEquals(listOf("Bank"), vm.state.value.entries.map { it.title })
    }
}
