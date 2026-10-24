package com.salvarez.miboveda.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.salvarez.miboveda.domain.model.BiometricStatus
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
    fun askForThePasswordInADialog() {
        show(SettingsState(biometricStatus = BiometricStatus.AVAILABLE, isPasswordDialogVisible = true))

        composeRule.onNodeWithText("Confirma tu contraseña maestra").assertIsDisplayed()
    }
}
