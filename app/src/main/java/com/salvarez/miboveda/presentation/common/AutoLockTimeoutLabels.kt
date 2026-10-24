package com.salvarez.miboveda.presentation.common

import androidx.annotation.StringRes
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.model.AutoLockTimeout

@StringRes
fun AutoLockTimeout.labelRes(): Int = when (this) {
    AutoLockTimeout.IMMEDIATE -> R.string.auto_lock_immediate
    AutoLockTimeout.THIRTY_SECONDS -> R.string.auto_lock_30_seconds
    AutoLockTimeout.ONE_MINUTE -> R.string.auto_lock_1_minute
    AutoLockTimeout.FIVE_MINUTES -> R.string.auto_lock_5_minutes
}
