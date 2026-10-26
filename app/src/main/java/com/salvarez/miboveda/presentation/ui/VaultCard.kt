package com.salvarez.miboveda.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** A raised panel with a glowing edge. In light mode it also casts a soft turquoise shadow. */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = isVaultDark()
    val shape = MaterialTheme.shapes.large
    val border = BorderStroke(
        1.dp,
        Brush.linearGradient(listOf(scheme.primary.copy(alpha = if (dark) 0.45f else 0.35f), scheme.outlineVariant, scheme.outlineVariant)),
    )
    val base = modifier
        .fillMaxWidth()
        .then(if (dark) Modifier else Modifier.shadow(10.dp, shape, ambientColor = scheme.primary.copy(alpha = 0.18f), spotColor = scheme.primary.copy(alpha = 0.22f)))
    val color = if (dark) scheme.surfaceContainer.copy(alpha = 0.92f) else scheme.surfaceContainerLowest
    if (onClick != null) {
        Surface(onClick = onClick, modifier = base, shape = shape, color = color, contentColor = scheme.onSurface, border = border) {
            Column(content = content)
        }
    } else {
        Surface(modifier = base, shape = shape, color = color, contentColor = scheme.onSurface, border = border) {
            Column(content = content)
        }
    }
}
