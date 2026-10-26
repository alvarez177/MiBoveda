package com.salvarez.miboveda.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The backdrop of every screen: a deep gradient with two soft glows. It stays still while screens slide
 * over it, which is what makes transitions feel like moving inside one place.
 */
@Composable
fun VaultBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = isVaultDark()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(scheme.background, scheme.surfaceContainerLow)))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(scheme.primary.copy(alpha = if (dark) 0.20f else 0.13f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.10f),
                        radius = size.width * 0.95f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(scheme.secondary.copy(alpha = if (dark) 0.14f else 0.09f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.92f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
        content = {
            // Without this, text would fall back to black on the dark backdrop.
            CompositionLocalProvider(LocalContentColor provides scheme.onBackground) { content() }
        },
    )
}

/** A [Scaffold] that lets [VaultBackground] show through. */
@Composable
fun VaultScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = topBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}
