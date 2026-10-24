package com.salvarez.miboveda.domain.model

enum class BiometricStatus {
    /** The device has no usable strong biometrics (none enrolled or no hardware). */
    UNAVAILABLE,

    /** Biometrics can be used but the user has not turned the feature on. */
    AVAILABLE,

    ENABLED,
}
