package com.salvarez.miboveda.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun MiBovedaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) VaultDarkColors else VaultLightColors,
        typography = Typography,
        shapes = VaultShapes,
        content = content,
    )
}
