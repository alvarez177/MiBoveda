package com.salvarez.miboveda.presentation.common

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/** Shows the system biometric prompt for a [Cipher] and reports the result. */
class BiometricAuthenticator(private val activity: FragmentActivity) {

    /**
     * [onFailure] receives true when the user simply dismissed the prompt (no error should be shown).
     */
    fun authenticate(
        cipher: Cipher,
        title: String,
        negativeButtonText: String,
        onSuccess: (Cipher) -> Unit,
        onFailure: (dismissedByUser: Boolean) -> Unit,
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val approved = result.cryptoObject?.cipher
                if (approved != null) onSuccess(approved) else onFailure(false)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFailure(errorCode in DISMISSED_CODES)
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            .authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    private companion object {
        val DISMISSED_CODES = setOf(
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED,
        )
    }
}

@Composable
fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    val context = LocalContext.current
    return remember(context) { BiometricAuthenticator(context.findFragmentActivity()) }
}

private fun Context.findFragmentActivity(): FragmentActivity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    error("A FragmentActivity is required to show the biometric prompt")
}
