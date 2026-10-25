package com.salvarez.miboveda.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.domain.model.ImportSummary
import com.salvarez.miboveda.domain.model.RecoveryKey
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: SettingsState, onIntent: (SettingsIntent) -> Unit = {}) = composeRule.setContent {
        MiBovedaTheme { SettingsScreen(state, onIntent, onBack = {}) }
    }

    @Test
    fun theSwitchIsDisabledWhenBiometricsAreUnavailable() {
        show(SettingsState(biometricStatus = BiometricStatus.UNAVAILABLE))

        composeRule.onNodeWithText("Registra una huella", substring = true).assertIsDisplayed()
        composeRule.onNode(isToggleable()).assertIsNotEnabled()
    }

    @Test
    fun turningItOnSendsTheToggleIntent() {
        val intents = mutableListOf<SettingsIntent>()
        show(SettingsState(biometricStatus = BiometricStatus.AVAILABLE), intents::add)

        composeRule.onNode(isToggleable()).assertIsEnabled().performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.ToggleBiometric(enable = true)), intents)
    }

    @Test
    fun choosingAnAutoLockTimeoutSendsTheIntent() {
        val intents = mutableListOf<SettingsIntent>()
        show(SettingsState(autoLockTimeout = AutoLockTimeout.IMMEDIATE), intents::add)

        composeRule.onNodeWithText("Después de 5 minutos").performScrollTo().performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.SelectAutoLockTimeout(AutoLockTimeout.FIVE_MINUTES)), intents)
    }

    @Test
    fun askForThePasswordInADialog() {
        show(SettingsState(biometricStatus = BiometricStatus.AVAILABLE, isPasswordDialogVisible = true))

        composeRule.onNodeWithText("Confirma tu contraseña maestra").assertIsDisplayed()
    }

    @Test
    fun offersToGenerateARecoveryKeyWhenThereIsNone() {
        val intents = mutableListOf<SettingsIntent>()
        show(SettingsState(hasRecoveryKey = false), intents::add)

        composeRule.onNodeWithText("Todavía no tienes clave de recuperación.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Generar clave").performScrollTo().performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.RequestRecoveryKey), intents)
    }

    @Test
    fun showsTheNewRecoveryKeyInADialog() {
        show(SettingsState(hasRecoveryKey = true, newRecoveryKey = RecoveryKey("ABCD-EFGH-IJKL-MNOP")))

        composeRule.onNodeWithText("Tu clave de recuperación").assertIsDisplayed()
        composeRule.onNodeWithText("ABCD-EFGH-IJKL-MNOP").assertIsDisplayed()
    }

    @Test
    fun theBackupSectionExplainsAndSendsTheRequest() {
        val intents = mutableListOf<SettingsIntent>()
        show(SettingsState(), intents::add)

        composeRule.onNodeWithText("Copia de seguridad").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Exportar copia cifrada").performScrollTo().performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.RequestBackup), intents)
    }

    @Test
    fun confirmsWhenTheBackupWasSaved() {
        show(SettingsState(isBackupSaved = true))

        composeRule.onNodeWithText("Copia guardada correctamente.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theImportButtonSendsTheRequestAndTheSummaryIsShown() {
        val intents = mutableListOf<SettingsIntent>()
        show(SettingsState(importSummary = ImportSummary(added = 3, skipped = 2)), intents::add)

        composeRule.onNodeWithText("Importación terminada: 3 nuevas, 2 ya estaban.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Importar copia de seguridad").performScrollTo().performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.RequestImport), intents)
    }
}
