package com.salvarez.miboveda.presentation.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheTitleAndTheMismatchError() {
        composeRule.setContent {
            MiBovedaTheme {
                OnboardingScreen(state = OnboardingState(error = VaultError.MasterPasswordMismatch), onIntent = {})
            }
        }
        composeRule.onNodeWithText("Crea tu bóveda").assertIsDisplayed()
        composeRule.onNodeWithText("Las contraseñas no coinciden.").assertIsDisplayed()
    }

    @Test
    fun submitButtonSendsTheSubmitIntent() {
        val intents = mutableListOf<OnboardingIntent>()
        composeRule.setContent {
            MiBovedaTheme { OnboardingScreen(state = OnboardingState(), onIntent = { intents += it }) }
        }
        composeRule.onNodeWithText("Crear bóveda").performClick()
        assertEquals(listOf<OnboardingIntent>(OnboardingIntent.Submit), intents)
    }
}
