package com.salvarez.miboveda.presentation.common

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Copies secrets to the clipboard and removes them again after a while. */
interface SensitiveClipboard {
    fun copy(label: String, text: String)
}

const val CLIPBOARD_CLEAR_DELAY_MILLIS = 30_000L

class AndroidSensitiveClipboard(
    context: Context,
    private val scope: CoroutineScope,
    private val clearDelayMillis: Long = CLIPBOARD_CLEAR_DELAY_MILLIS,
) : SensitiveClipboard {
    private val clipboard = context.getSystemService(ClipboardManager::class.java)
    private var clearJob: Job? = null

    override fun copy(label: String, text: String) {
        val clip = ClipData.newPlainText(label, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Asks the system not to show the copied value in previews.
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(clearDelayMillis)
            clear()
        }
    }

    private fun clear() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
