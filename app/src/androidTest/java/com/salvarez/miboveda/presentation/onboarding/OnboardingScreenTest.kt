package com.salvarez.miboveda.presentation.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.RecoveryKey
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

    @Test
    fun showsTheRecoveryKeyAndOnlyContinuesOnceItIsSaved() {
        val intents = mutableListOf<OnboardingIntent>()
        var saved by mutableStateOf(false)
        composeRule.setContent {
            MiBovedaTheme {
                OnboardingScreen(
                    state = OnboardingState(recoveryKey = RecoveryKey("ABCD-EFGH-IJKL-MNOP"), isRecoverySaved = saved, isRecoveryRevealed = true),
                    onIntent = { intents += it },
                )
            }
        }
        composeRule.onNodeWithText("Guarda tu clave de recuperación").assertIsDisplayed()
        composeRule.onNodeWithText("ABCD-EFGH-IJKL-MNOP").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()

        saved = true
        composeRule.onNodeWithText("Continuar").assertIsEnabled().performClick()
        composeRule.onNodeWithText("Copiar clave").performClick()

        assertEquals(listOf(OnboardingIntent.ContinueAfterRecovery, OnboardingIntent.CopyRecoveryKey), intents)
    }

    @Test
    fun offersToRestoreFromABackup() {
        val intents = mutableListOf<OnboardingIntent>()
        composeRule.setContent {
            MiBovedaTheme { OnboardingScreen(state = OnboardingState(), onIntent = { intents += it }) }
        }
        composeRule.onNodeWithText("Restaurar desde una copia de seguridad").performScrollTo().performClick()

        assertEquals(listOf<OnboardingIntent>(OnboardingIntent.RestoreClicked), intents)
    }

    @Test
    fun theRestoreFormAsksOnlyForTheBackupPassword() {
        composeRule.setContent {
            MiBovedaTheme {
                OnboardingScreen(state = OnboardingState(restoreFileUri = "content://files/backup"), onIntent = {})
            }
        }
        composeRule.onNodeWithText("Restaura tu bóveda").assertIsDisplayed()
        composeRule.onNodeWithText("Contraseña maestra de la copia").assertIsDisplayed()
        composeRule.onNodeWithText("Restaurar bóveda").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theRecoveryKeyStartsCoveredAndTappingTheVeilAsksToReveal() {
        val intents = mutableListOf<OnboardingIntent>()
        composeRule.setContent {
            MiBovedaTheme {
                OnboardingScreen(
                    state = OnboardingState(recoveryKey = RecoveryKey("ABCD-EFGH-IJKL-MNOP")),
                    onIntent = { intents += it },
                )
            }
        }
        composeRule.onNodeWithText("Toca para mostrar tu clave").assertIsDisplayed().performClick()

        assertEquals(listOf<OnboardingIntent>(OnboardingIntent.RevealRecoveryKey), intents)
    }
}
