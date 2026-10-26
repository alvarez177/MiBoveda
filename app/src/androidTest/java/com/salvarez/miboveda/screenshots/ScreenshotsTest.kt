package com.salvarez.miboveda.screenshots

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.AutoLockTimeout
import com.salvarez.miboveda.domain.model.BiometricStatus
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
 */
class ScreenshotsTest {
    @get:Rule
    val rule = createComposeRule()

    private var dark by mutableStateOf(false)
    private var screen by mutableStateOf<@Composable () -> Unit>({})

    private val outputDir = File(
        InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
        "screenshots",
    ).also { it.mkdirs() }

    private val sampleEntries = listOf(
        PasswordEntry(id = 1, title = "Banco Nacional", username = "ana.perez@correo.com", password = "x", updatedAt = Instant.now()),
        PasswordEntry(id = 2, title = "Correo", username = "ana.perez", password = "x"),
        PasswordEntry(id = 3, title = "GitHub", username = "ana-dev", password = "x"),
        PasswordEntry(id = 4, title = "Netflix", username = "familia.perez@correo.com", password = "x"),
        PasswordEntry(id = 5, title = "Wifi de casa", username = "", password = "x"),
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
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { screen() }
            }
        }
        val noOp: (Any) -> Unit = {}
        val shots: List<Pair<String, @Composable () -> Unit>> = listOf(
            "01-onboarding" to { OnboardingScreen(OnboardingState(), noOp) },
            "02-onboarding-contrasena" to {
                OnboardingScreen(OnboardingState(password = "MiClaveSegura2026!", confirmation = "MiClaveSegura2026!"), noOp)
            },
            "03-clave-de-recuperacion" to { OnboardingScreen(OnboardingState(recoveryKey = sampleKey, isRecoverySaved = true), noOp) },
            "04-restaurar" to { OnboardingScreen(OnboardingState(restoreFileUri = "content://x"), noOp) },
            "05-desbloqueo" to { UnlockScreen(UnlockState(), noOp, onForgotPassword = {}) },
            "06-desbloqueo-error" to {
                UnlockScreen(UnlockState(password = "equivocada", error = VaultError.WrongMasterPassword), noOp, onForgotPassword = {})
            },
            "07-desbloqueo-biometria" to {
                UnlockScreen(UnlockState(biometricStatus = BiometricStatus.ENABLED), noOp, onForgotPassword = {})
            },
            "08-recuperar" to { RecoveryScreen(RecoveryState(), noOp, onBack = {}) },
            "09-lista-vacia" to { EntryListScreen(EntryListState(isLoading = false), {}, {}, {}) },
            "10-lista" to { EntryListScreen(EntryListState(entries = sampleEntries, isLoading = false), {}, {}, {}) },
            "11-detalle" to {
                EntryDetailScreen(EntryDetailState(entry = sampleEntry, isLoading = false), SnackbarHostState(), noOp, {}, {})
            },
            "12-detalle-visible" to {
                EntryDetailScreen(
                    EntryDetailState(entry = sampleEntry, isLoading = false, isPasswordVisible = true),
                    SnackbarHostState(), noOp, {}, {},
                )
            },
            "13-editar-nueva" to { EntryEditScreen(EntryEditState(), noOp, {}) },
            "14-editar" to {
                EntryEditScreen(
                    EntryEditState(
                        id = 1, title = sampleEntry.title, username = sampleEntry.username, password = sampleEntry.password,
                        url = sampleEntry.url, notes = sampleEntry.notes,
                    ),
                    noOp, {},
                )
            },
            "15-ajustes" to {
                SettingsScreen(
                    SettingsState(biometricStatus = BiometricStatus.AVAILABLE, autoLockTimeout = AutoLockTimeout.ONE_MINUTE, hasRecoveryKey = true),
                    noOp, {},
                )
            },
        )
        for ((isDark, label) in listOf(false to "claro", true to "oscuro")) {
            dark = isDark
            for ((name, content) in shots) {
                screen = content
                rule.waitForIdle()
                save("$name-$label", rule.onRoot().captureToImage().asAndroidBitmap())
            }
        }
    }

    private fun save(name: String, bitmap: Bitmap) {
        File(outputDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
