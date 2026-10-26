package com.salvarez.miboveda.presentation.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.salvarez.miboveda.presentation.common.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun transparentBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
    titleContentColor = MaterialTheme.colorScheme.onBackground,
    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
    actionIconContentColor = MaterialTheme.colorScheme.primary,
)

/** A see-through top bar, so the vault backdrop shows behind it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = { if (onBack != null) BackButton(onBack) },
        actions = actions,
        colors = transparentBarColors(),
    )
}

/** The large, collapsing variant used on the main list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultLargeTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    actions: @Composable RowScope.() -> Unit = {},
) {
    LargeTopAppBar(
        title = { Text(title) },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = transparentBarColors(),
    )
}
