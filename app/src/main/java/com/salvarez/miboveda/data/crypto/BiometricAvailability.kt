package com.salvarez.miboveda.data.crypto

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager

/** Whether the device can show a strong biometric prompt right now. A seam so tests do not need a fingerprint. */
fun interface BiometricAvailability {
    fun isAvailable(): Boolean
}

fun biometricAvailability(context: Context) = BiometricAvailability {
    // Before Android 11 the system cannot say whether a sensor is "strong" (it answers "unknown"), so there
    // we only check that something is enrolled; the Keystore key still demands a biometric for every use.
    val authenticator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    } else {
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    }
    BiometricManager.from(context).canAuthenticate(authenticator) == BiometricManager.BIOMETRIC_SUCCESS
}
