package com.salvarez.miboveda.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** True when the active color scheme is the dark one. */
@Composable
fun isVaultDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** The brand gradient: turquoise into sky blue. Used for the main button, the padlock and avatars. */
@Composable
fun brandBrush(): Brush {
    val dark = isVaultDark()
    return Brush.linearGradient(
        if (dark) listOf(Color(0xFF2DD4BF), Color(0xFF38BDF8)) else listOf(Color(0xFF0F766E), Color(0xFF0369A1)),
    )
}

/** Gradient pairs for avatars; one is picked from the entry title so the same entry always looks the same. */
val AvatarGradients: List<List<Color>> = listOf(
    listOf(Color(0xFF2DD4BF), Color(0xFF0EA5E9)),
    listOf(Color(0xFF60A5FA), Color(0xFF6366F1)),
    listOf(Color(0xFFA78BFA), Color(0xFFEC4899)),
    listOf(Color(0xFFFBBF24), Color(0xFFF97316)),
    listOf(Color(0xFF4ADE80), Color(0xFF14B8A6)),
    listOf(Color(0xFFFB7185), Color(0xFFF59E0B)),
)
