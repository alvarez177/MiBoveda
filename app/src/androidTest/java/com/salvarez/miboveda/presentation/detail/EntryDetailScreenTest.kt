package com.salvarez.miboveda.presentation.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import org.junit.Rule
import org.junit.Test

class EntryDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val entry = PasswordEntry(id = 1, title = "Bank", username = "ana", password = "s3cret-value")

    private fun show(state: EntryDetailState) = composeRule.setContent {
        MiBovedaTheme {
            EntryDetailScreen(state, SnackbarHostState(), onIntent = {}, onBack = {}, onEdit = {})
        }
    }

    @Test
    fun hidesThePasswordByDefault() {
        show(EntryDetailState(entry = entry, isLoading = false))
        composeRule.onNodeWithText("ana").assertIsDisplayed()
        composeRule.onNodeWithText("s3cret-value").assertDoesNotExist()
    }

    @Test
    fun showsThePasswordWhenVisible() {
        show(EntryDetailState(entry = entry, isLoading = false, isPasswordVisible = true))
        composeRule.onNodeWithText("s3cret-value").assertIsDisplayed()
    }

    @Test
    fun asksForConfirmationBeforeDeleting() {
        show(EntryDetailState(entry = entry, isLoading = false, isDeleteDialogVisible = true))
        composeRule.onNodeWithText("¿Eliminar esta contraseña?").assertIsDisplayed()
    }
}
