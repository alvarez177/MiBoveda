package com.salvarez.miboveda.presentation.edit

import androidx.lifecycle.SavedStateHandle
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.usecase.FakeEntryRepository
import com.salvarez.miboveda.domain.usecase.ObserveEntry
import com.salvarez.miboveda.domain.usecase.SaveEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EntryEditViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repository: FakeEntryRepository, id: Long) = EntryEditViewModel(
        SavedStateHandle(mapOf(EntryEditViewModel.ARG_ID to id)),
        ObserveEntry(repository),
        SaveEntry(repository),
    )

    @Test
    fun `a new entry starts empty and is saved`() = runTest {
        val repository = FakeEntryRepository()
        val vm = viewModel(repository, PasswordEntry.NEW_ID)
        assertTrue(vm.state.value.isNew)

        vm.onIntent(EntryEditIntent.TitleChanged("Bank"))
        vm.onIntent(EntryEditIntent.PasswordChanged("s3cret"))
        vm.onIntent(EntryEditIntent.Save)

        assertEquals(EntryEditEffect.Saved, vm.effects.first())
        assertEquals("Bank", repository.current.single().title)
    }

    @Test
    fun `saving without a title shows an error and stores nothing`() = runTest {
        val repository = FakeEntryRepository()
        val vm = viewModel(repository, PasswordEntry.NEW_ID)
        vm.onIntent(EntryEditIntent.Save)

        assertEquals(VaultError.EntryTitleRequired, vm.state.value.error)
        assertFalse(vm.state.value.isSaving)
        assertEquals(0, repository.current.size)
    }

    @Test
    fun `an existing entry is loaded and updated in place`() = runTest {
        val repository = FakeEntryRepository(listOf(PasswordEntry(id = 4, title = "Bank", username = "ana", password = "old")))
        val vm = viewModel(repository, 4)

        assertEquals("Bank", vm.state.value.title)
        assertFalse(vm.state.value.isNew)

        vm.onIntent(EntryEditIntent.PasswordChanged("new"))
        vm.onIntent(EntryEditIntent.Save)

        assertEquals(EntryEditEffect.Saved, vm.effects.first())
        assertEquals("new", repository.current.single().password)
        assertEquals(4L, repository.current.single().id)
    }

    @Test
    fun `state toString does not leak the password`() {
        assertFalse(EntryEditState(password = "s3cret-value").toString().contains("s3cret-value"))
    }
}
