package com.salvarez.miboveda.presentation.unlock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UnlockScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheWrongPasswordError() {
        composeRule.setContent {
            MiBovedaTheme { UnlockScreen(state = UnlockState(error = VaultError.WrongMasterPassword), onIntent = {}) }
        }
        composeRule.onNodeWithText("Desbloquea tu bóveda").assertIsDisplayed()
        composeRule.onNodeWithText("La contraseña maestra es incorrecta.").assertIsDisplayed()
    }

    @Test
    fun unlockButtonSendsTheSubmitIntent() {
        val intents = mutableListOf<UnlockIntent>()
        composeRule.setContent {
            MiBovedaTheme { UnlockScreen(state = UnlockState(), onIntent = { intents += it }) }
        }
        composeRule.onNodeWithText("Desbloquear").performClick()
        assertEquals(listOf<UnlockIntent>(UnlockIntent.Submit), intents)
    }
}
