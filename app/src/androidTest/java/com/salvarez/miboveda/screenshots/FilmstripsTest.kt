package com.salvarez.miboveda.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.salvarez.miboveda.domain.error.VaultError
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.domain.model.RecoveryKey
import com.salvarez.miboveda.presentation.detail.EntryDetailScreen
import com.salvarez.miboveda.presentation.detail.EntryDetailState
import com.salvarez.miboveda.presentation.list.EntryListScreen
import com.salvarez.miboveda.presentation.list.EntryListState
import com.salvarez.miboveda.presentation.onboarding.OnboardingScreen
import com.salvarez.miboveda.presentation.onboarding.OnboardingState
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import com.salvarez.miboveda.presentation.ui.VaultBackground
import com.salvarez.miboveda.presentation.unlock.UnlockScreen
import com.salvarez.miboveda.presentation.unlock.UnlockState
import java.io.File
import org.junit.Rule
import org.junit.Test

/**
 * Draws a film strip for each animation: the same screen at several moments, side by side, with the time
 * under each frame. The animation clock is driven by hand, so the strips are exact and repeatable.
 * Pull with `adb pull /sdcard/Android/data/com.salvarez.miboveda/files/filmstrips`.
 */
class FilmstripsTest {
    @get:Rule
    val rule = createComposeRule()

    private val outputDir = File(
        InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
        "filmstrips",
    ).also { it.mkdirs() }

    private val noOp: (Any) -> Unit = {}

    private var screen by mutableStateOf<@Composable () -> Unit>({})

    private fun start() {
        rule.setContent { MiBovedaTheme(darkTheme = true) { VaultBackground { screen() } } }
    }

    /** Shows [content], lets it settle, then calls [change] and captures the screen at each of [times] (ms after the change). */
    private fun strip(
        name: String,
        times: List<Long>,
        cropTop: Float = 0f,
        cropBottom: Float = 1f,
        before: () -> Unit = {},
        change: () -> Unit,
    ) {
        before()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        change()
        var elapsed = 0L
        val frames = times.map { time ->
            rule.mainClock.advanceTimeBy(time - elapsed)
            elapsed = time
            val full = rule.onRoot().captureToImage().asAndroidBitmap()
            val top = (full.height * cropTop).toInt()
            val height = (full.height * (cropBottom - cropTop)).toInt()
            "$time ms" to Bitmap.createBitmap(full, 0, top, full.width, height)
        }
        rule.mainClock.autoAdvance = true
        save(name, frames)
    }

    private fun save(name: String, frames: List<Pair<String, Bitmap>>) {
        val frameWidth = 330
        val gap = 14
        val labelHeight = 56
        val scale = frameWidth.toFloat() / frames.first().second.width
        val frameHeight = (frames.first().second.height * scale).toInt()
        val sheet = Bitmap.createBitmap(
            frames.size * (frameWidth + gap) + gap,
            frameHeight + labelHeight + gap * 2,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.parseColor("#05080F"))
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9FB0CC")
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        frames.forEachIndexed { index, (label, bitmap) ->
            val left = gap + index * (frameWidth + gap)
            canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), Rect(left, gap, left + frameWidth, gap + frameHeight), bitmapPaint)
            canvas.drawText(label, left + frameWidth / 2f, gap + frameHeight + 38f, text)
        }
        File(outputDir, "$name.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun drawAllStrips() {
        var open by mutableStateOf(false)
        var error by mutableStateOf<VaultError?>(null)
        var password by mutableStateOf("")
        var revealed by mutableStateOf(false)
        var showEntries by mutableStateOf(false)
        var step by mutableStateOf(false)
        var copied by mutableStateOf(0)
        start()

        // 1. The padlock opens: the dial turns a full turn and the shackle lifts.
        strip(
            name = "01-candado-abre",
            times = listOf(16, 120, 240, 360, 480, 640, 900),
            cropTop = 0f, cropBottom = 0.29f,
            before = { screen = { UnlockScreen(UnlockState(), noOp, {}, isOpen = open) } },
            change = { open = true },
        )
        // 2. A wrong password: the padlock shakes sideways and turns red.
        strip(
            name = "02-error-sacudida",
            times = listOf(16, 60, 105, 150, 195, 240, 300, 380),
            cropTop = 0f, cropBottom = 0.29f,
            before = { screen = { UnlockScreen(UnlockState(password = "x", error = error), noOp, {}) } },
            change = { error = VaultError.WrongMasterPassword },
        )
        // 3. The strength meter fills and changes color as the password improves.
        strip(
            name = "03-medidor-seguridad",
            times = listOf(16, 150, 300, 450),
            cropTop = 0.30f, cropBottom = 0.58f,
            before = {
                screen = { OnboardingScreen(OnboardingState(password = password, confirmation = password), noOp) }
                password = "hola1234"
            },
            change = { password = "MiClaveSegura2026Larga" },
        )
        // 4. The entries of the list rise in one after another.
        strip(
            name = "04-lista-cascada",
            times = listOf(16, 120, 240, 360, 520, 800),
            before = {
                screen = {
                    EntryListScreen(
                        EntryListState(
                            entries = if (showEntries) {
                                listOf("Banco Nacional", "Correo", "GitHub", "Netflix", "Spotify", "Wifi de casa").mapIndexed { i, t ->
                                    PasswordEntry(id = i + 1L, title = t, username = "ana.perez@correo.com", password = "x")
                                }
                            } else {
                                emptyList()
                            },
                            isLoading = false,
                        ),
                        {}, {}, {},
                    )
                }
            },
            change = { showEntries = true },
        )
        // 5. A secret is "decrypted": random characters flicker and settle from left to right.
        strip(
            name = "05-clave-se-descifra",
            times = listOf(16, 70, 140, 210, 280, 350, 450),
            cropTop = 0.38f, cropBottom = 0.52f,
            before = {
                screen = {
                    EntryDetailScreen(
                        EntryDetailState(
                            entry = PasswordEntry(id = 1, title = "Banco Nacional", username = "ana.perez@correo.com", password = "T9#vLq2!mZ8x"),
                            isLoading = false,
                            isPasswordVisible = revealed,
                        ),
                        noOp, {}, {},
                    )
                }
            },
            change = { revealed = true },
        )
        // 6. The "copied" confirmation slides up, draws its check, and leaves.
        strip(
            name = "06-aviso-copiado",
            times = listOf(16, 150, 300, 500, 1500, 3000, 3300, 3600),
            cropTop = 0.62f, cropBottom = 1f,
            before = {
                screen = {
                    EntryDetailScreen(
                        EntryDetailState(
                            entry = PasswordEntry(id = 1, title = "Banco Nacional", username = "ana.perez@correo.com", password = "x"),
                            isLoading = false,
                        ),
                        noOp, {}, {}, copiedSignal = copied,
                    )
                }
            },
            change = { copied = 1 },
        )
        // 7. The steps of creating a vault slide into each other.
        strip(
            name = "07-pasos-deslizan",
            times = listOf(16, 90, 180, 280, 400, 600),
            before = {
                screen = {
                    OnboardingScreen(
                        if (step) {
                            OnboardingState(recoveryKey = RecoveryKey("Y3RY-NLI3-6GID-TSZO-FGMF-ZBWX-NM54-PCYZ-L7XD-IBVB-HGAV-IQCE-4NFQ"))
                        } else {
                            OnboardingState(password = "MiClaveSegura2026Larga", confirmation = "MiClaveSegura2026Larga")
                        },
                        noOp,
                    )
                }
            },
            change = { step = true },
        )
    }
}
