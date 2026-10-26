package com.salvarez.miboveda.screenshots

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.model.BiometricStatus
import com.salvarez.miboveda.domain.model.ImportSummary
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.model.RecoveryKey
import com.salvarez.miboveda.presentation.detail.EntryDetailScreen
import com.salvarez.miboveda.presentation.detail.EntryDetailState
import com.salvarez.miboveda.presentation.edit.EntryEditScreen
import com.salvarez.miboveda.presentation.edit.EntryEditState
import com.salvarez.miboveda.presentation.list.EntryListScreen
import com.salvarez.miboveda.presentation.list.EntryListState
import com.salvarez.miboveda.presentation.onboarding.OnboardingScreen
import com.salvarez.miboveda.presentation.onboarding.OnboardingState
import com.salvarez.miboveda.presentation.recovery.RecoveryScreen
import com.salvarez.miboveda.presentation.recovery.RecoveryState
import com.salvarez.miboveda.presentation.settings.SettingsScreen
import com.salvarez.miboveda.presentation.settings.SettingsState
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import com.salvarez.miboveda.presentation.ui.VaultBackground
import com.salvarez.miboveda.presentation.unlock.UnlockScreen
import com.salvarez.miboveda.presentation.unlock.UnlockState
import java.io.File
import java.time.Instant
import org.junit.Rule
import org.junit.Test

/**
 * Renders every screen with sample data in light and dark and saves the pictures, so a design change can
 * be reviewed without a phone. It never touches the real vault. Pull the files with:
 * `adb pull /sdcard/Android/data/com.salvarez.miboveda/files/screenshots`
 * (run the test with `adb shell am instrument`, because Gradle removes the app, and its files, afterwards).
 *
 * Some shots freeze the animation clock part-way, to catch an animation in the middle of its movement.
 */
class ScreenshotsTest {
    @get:Rule
    val rule = createComposeRule()

    private var dark by mutableStateOf(false)
    private var epoch by mutableIntStateOf(0)
    private var screen by mutableStateOf<@Composable () -> Unit>({})

    private val outputDir = File(
        InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
        "screenshots",
    ).also { it.mkdirs() }

    private class Shot(
        val name: String,
        /** Milliseconds of animation to let pass before the picture; 0 waits until everything settles. */
        val frozenAtMillis: Long = 0,
        val prepare: () -> Unit = {},
        val content: @Composable () -> Unit,
    )

    private val sampleEntries = listOf(
        PasswordEntry(id = 1, title = "Banco Nacional", username = "ana.perez@correo.com", password = "x", updatedAt = Instant.now()),
        PasswordEntry(id = 2, title = "Correo", username = "ana.perez", password = "x"),
        PasswordEntry(id = 3, title = "GitHub", username = "ana-dev", password = "x"),
        PasswordEntry(id = 4, title = "Netflix", username = "familia.perez@correo.com", password = "x"),
        PasswordEntry(id = 5, title = "Wifi de casa", username = "", password = "x"),
        PasswordEntry(id = 6, title = "Spotify", username = "ana.perez", password = "x"),
    )
    private val sampleEntry = PasswordEntry(
        id = 1,
        title = "Banco Nacional",
        username = "ana.perez@correo.com",
        password = "T9#vLq2!mZ8x",
        url = "https://www.banconacional.example",
        notes = "Cuenta principal. Pregunta secreta: primer colegio.",
    )
    private val sampleKey = RecoveryKey("Y3RY-NLI3-6GID-TSZO-FGMF-ZBWX-NM54-PCYZ-L7XD-IBVB-HGAV-IQCE-4NFQ")

    @Test
    fun captureAllScreens() {
        rule.setContent {
            MiBovedaTheme(darkTheme = dark) {
                VaultBackground { key(epoch) { screen() } }
            }
        }
        val noOp: (Any) -> Unit = {}
        val shots = listOf(
            Shot("01-onboarding") { OnboardingScreen(OnboardingState(), noOp) },
            Shot("02-contrasena-debil") { OnboardingScreen(OnboardingState(password = "hola1234", confirmation = "hola"), noOp) },
            Shot("03-contrasena-fuerte") {
                OnboardingScreen(OnboardingState(password = "MiClaveSegura2026!", confirmation = "MiClaveSegura2026!"), noOp)
            },
            Shot("04-clave-tapada") { OnboardingScreen(OnboardingState(recoveryKey = sampleKey), noOp) },
            Shot("05-clave-revelada") {
                OnboardingScreen(OnboardingState(recoveryKey = sampleKey, isRecoveryRevealed = true, isRecoverySaved = true), noOp)
            },
            Shot("06-restaurar") { OnboardingScreen(OnboardingState(restoreFileUri = "content://x"), noOp) },
            Shot("07-desbloqueo") { UnlockScreen(UnlockState(), noOp, onForgotPassword = {}) },
            Shot("08-desbloqueo-error-sacudida", frozenAtMillis = 110) {
                UnlockScreen(UnlockState(password = "equivocada", error = VaultError.WrongMasterPassword), noOp, onForgotPassword = {})
            },
            Shot("09-desbloqueo-abriendo", frozenAtMillis = 360) { UnlockScreen(UnlockState(), noOp, onForgotPassword = {}, isOpen = true) },
            Shot("10-desbloqueo-abierto") { UnlockScreen(UnlockState(), noOp, onForgotPassword = {}, isOpen = true) },
            Shot("11-desbloqueo-biometria") {
                UnlockScreen(UnlockState(biometricStatus = BiometricStatus.ENABLED), noOp, onForgotPassword = {})
            },
            Shot("12-recuperar") { RecoveryScreen(RecoveryState(), noOp, onBack = {}) },
            Shot("13-lista-vacia") { EntryListScreen(EntryListState(isLoading = false), {}, {}, {}) },
            Shot("14-lista-entrando", frozenAtMillis = 230) { EntryListScreen(EntryListState(entries = sampleEntries, isLoading = false), {}, {}, {}) },
            Shot("15-lista") { EntryListScreen(EntryListState(entries = sampleEntries, isLoading = false), {}, {}, {}) },
            Shot("16-detalle") { EntryDetailScreen(EntryDetailState(entry = sampleEntry, isLoading = false), noOp, {}, {}) },
            Shot("17-detalle-clave-visible") {
                EntryDetailScreen(EntryDetailState(entry = sampleEntry, isLoading = false, isPasswordVisible = true), noOp, {}, {})
            },
            Shot("18-detalle-copiado", frozenAtMillis = 700) {
                EntryDetailScreen(EntryDetailState(entry = sampleEntry, isLoading = false), noOp, {}, {}, copiedSignal = 1)
            },
            Shot("19-editar-nueva") { EntryEditScreen(EntryEditState(), noOp, {}) },
            Shot("20-editar") {
                EntryEditScreen(
                    EntryEditState(
                        id = 1, title = sampleEntry.title, username = sampleEntry.username, password = sampleEntry.password,
                        url = sampleEntry.url, notes = sampleEntry.notes,
                    ),
                    noOp, {},
                )
            },
            Shot("21-ajustes") {
                SettingsScreen(
                    SettingsState(biometricStatus = BiometricStatus.AVAILABLE, autoLockTimeout = AutoLockTimeout.ONE_MINUTE, hasRecoveryKey = true),
                    noOp, {},
                )
            },
            Shot(
                "22-ajustes-copias",
                prepare = { rule.onNodeWithText("Importar copia de seguridad").performScrollTo() },
            ) {
                SettingsScreen(
                    SettingsState(
                        biometricStatus = BiometricStatus.AVAILABLE, hasRecoveryKey = true,
                        isBackupSaved = true, importSummary = ImportSummary(added = 3, skipped = 2),
                    ),
                    noOp, {},
                )
            },
        )
        for ((isDark, label) in listOf(false to "claro", true to "oscuro")) {
            dark = isDark
            for (shot in shots) {
                shoot(shot, label)
            }
        }
    }

    private fun shoot(shot: Shot, label: String) {
        val frozen = shot.frozenAtMillis > 0
        rule.mainClock.autoAdvance = !frozen
        screen = shot.content
        epoch++
        if (frozen) {
            rule.mainClock.advanceTimeBy(shot.frozenAtMillis)
        } else {
            rule.waitForIdle()
        }
        shot.prepare()
        if (!frozen) rule.waitForIdle()
        save("${shot.name}-$label", rule.onRoot().captureToImage().asAndroidBitmap())
        rule.mainClock.autoAdvance = true
    }

    private fun save(name: String, bitmap: Bitmap) {
        File(outputDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
