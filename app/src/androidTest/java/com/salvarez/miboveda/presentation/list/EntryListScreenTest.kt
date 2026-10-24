package com.salvarez.miboveda.presentation.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EntryListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheEmptyMessage() {
        composeRule.setContent {
            MiBovedaTheme { EntryListScreen(EntryListState(isLoading = false), onAdd = {}, onEntryClick = {}, onOpenSettings = {}) }
        }
        composeRule.onNodeWithText("Aún no tienes contraseñas guardadas.", substring = true).assertIsDisplayed()
    }

    @Test
    fun clickingAnEntryOpensItAndAddIsAvailable() {
        val opened = mutableListOf<Long>()
        var added = 0
        val state = EntryListState(
            entries = listOf(PasswordEntry(id = 7, title = "Bank", username = "ana", password = "x")),
            isLoading = false,
        )
        composeRule.setContent {
            MiBovedaTheme { EntryListScreen(state, onAdd = { added++ }, onEntryClick = { opened += it }, onOpenSettings = {}) }
        }
        composeRule.onNodeWithText("Bank").performClick()
        composeRule.onNodeWithText("Añadir").performClick()

        assertEquals(listOf(7L), opened)
        assertEquals(1, added)
    }
}
