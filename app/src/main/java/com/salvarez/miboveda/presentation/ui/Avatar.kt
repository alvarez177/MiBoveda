package com.salvarez.miboveda.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A round badge with the first letter of [title] over a gradient picked from the title, so each entry has its own color. */
@Composable
fun Avatar(title: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val colors = AvatarGradients[(title.lowercase().hashCode().and(Int.MAX_VALUE)) % AvatarGradients.size]
    Box(
        modifier = modifier
            .size(size)
            .background(Brush.linearGradient(colors), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.42f).sp),
        )
    }
}
