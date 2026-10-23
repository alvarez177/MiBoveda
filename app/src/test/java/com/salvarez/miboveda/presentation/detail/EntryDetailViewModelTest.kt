package com.salvarez.miboveda.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.usecase.DeleteEntry
import com.salvarez.miboveda.domain.usecase.FakeEntryRepository
import com.salvarez.miboveda.domain.usecase.ObserveEntry
import com.salvarez.miboveda.presentation.common.SensitiveClipboard
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

class EntryDetailViewModelTest {
    private class FakeClipboard : SensitiveClipboard {
        val copies = mutableListOf<Pair<String, String>>()
        override fun copy(label: String, text: String) {
            copies += label to text
        }
    }

    private val entry = PasswordEntry(id = 1, title = "Bank", username = "ana", password = "s3cret")
    private val repository = FakeEntryRepository(listOf(entry))
    private val clipboard = FakeClipboard()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(id: Long = 1) = EntryDetailViewModel(
        SavedStateHandle(mapOf(EntryDetailViewModel.ARG_ID to id)),
        ObserveEntry(repository),
        DeleteEntry(repository),
        clipboard,
    )

    @Test
    fun `loads the entry`() = runTest {
        val vm = viewModel()
        assertEquals(entry, vm.state.value.entry)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `reports a missing entry`() = runTest {
        val vm = viewModel(id = 99)
        assertEquals(VaultError.EntryNotFound, vm.state.value.error)
    }

    @Test
    fun `password is hidden until toggled`() = runTest {
        val vm = viewModel()
        assertFalse(vm.state.value.isPasswordVisible)
        vm.onIntent(EntryDetailIntent.TogglePasswordVisibility)
        assertTrue(vm.state.value.isPasswordVisible)
    }

    @Test
    fun `copying sends the value to the clipboard and emits Copied`() = runTest {
        val vm = viewModel()
        vm.onIntent(EntryDetailIntent.CopyPassword)

        assertEquals(listOf("password" to "s3cret"), clipboard.copies)
        assertEquals(EntryDetailEffect.Copied, vm.effects.first())
    }

    @Test
    fun `deleting needs confirmation and then emits Deleted`() = runTest {
        val vm = viewModel()
        vm.onIntent(EntryDetailIntent.RequestDelete)
        assertTrue(vm.state.value.isDeleteDialogVisible)
        assertEquals(1, repository.current.size)

        vm.onIntent(EntryDetailIntent.ConfirmDelete)

        assertEquals(EntryDetailEffect.Deleted, vm.effects.first())
        assertEquals(0, repository.current.size)
        assertFalse(vm.state.value.isDeleteDialogVisible)
    }
}
