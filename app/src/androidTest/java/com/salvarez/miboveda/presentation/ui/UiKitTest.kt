package com.salvarez.miboveda.presentation.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.salvarez.miboveda.domain.model.PasswordStrength
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiKitTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aSecretShowsOnlyDotsUntilItIsRevealed() {
        composeRule.setContent { MiBovedaTheme { SecretText(secret = "s3cret-value", revealed = false) } }

        composeRule.onNodeWithText("••••••••••").assertIsDisplayed()
        composeRule.onNodeWithText("s3cret-value").assertDoesNotExist()
    }

    @Test
    fun aRevealedSecretEndsUpReadable() {
        composeRule.setContent { MiBovedaTheme { VaultBackground { SecretText(secret = "s3cret-value", revealed = true) } } }
        // The characters flicker for a moment before settling.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("s3cret-value").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("s3cret-value").assertExists()
    }

    @Test
    fun theStrengthMeterNamesEachLevel() {
        var strength by mutableStateOf(PasswordStrength.WEAK)
        composeRule.setContent { MiBovedaTheme { StrengthMeter(strength) } }
        listOf(
            PasswordStrength.WEAK to "Seguridad: débil",
            PasswordStrength.MEDIUM to "Seguridad: media",
            PasswordStrength.STRONG to "Seguridad: fuerte",
        ).forEach { (level, label) ->
            strength = level
            composeRule.waitForIdle()
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun theMainButtonClicksWhenEnabled() {
        var clicks = 0
        composeRule.setContent { MiBovedaTheme { VaultButton("Entrar", onClick = { clicks++ }) } }

        composeRule.onNodeWithText("Entrar").assertIsEnabled().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun aDisabledButtonDoesNotClick() {
        var clicks = 0
        composeRule.setContent { MiBovedaTheme { VaultButton("Entrar", onClick = { clicks++ }, enabled = false) } }

        composeRule.onNodeWithText("Entrar").assertIsNotEnabled()
        assertEquals(0, clicks)
    }
}
