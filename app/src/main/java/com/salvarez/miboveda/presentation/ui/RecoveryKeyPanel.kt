package com.salvarez.miboveda.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salvarez.miboveda.R
import com.salvarez.miboveda.presentation.theme.Motion
import com.salvarez.miboveda.presentation.theme.MonospaceStyle

/**
 * A recovery key shown in readable rows, covered by a veil until the user taps it (so it is not on screen
 * by accident), with a button to copy it.
 */
@Composable
fun RecoveryKeyPanel(
    recoveryKey: String,
    revealed: Boolean,
    onReveal: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyAlpha by animateFloatAsState(if (revealed) 1f else 0f, tween(Motion.Medium), label = "key-alpha")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultCard { KeyBox(recoveryKey, revealed, keyAlpha, onReveal) }
        VaultOutlinedButton(
            text = stringResource(R.string.copy_recovery_key),
            onClick = onCopy,
            icon = Icons.Rounded.ContentCopy,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun KeyBox(recoveryKey: String, revealed: Boolean, keyAlpha: Float, onReveal: () -> Unit) {
    Box {
        Text(
            text = recoveryKey.split('-').chunked(GROUPS_PER_ROW).joinToString("\n") { it.joinToString("-") },
            style = MonospaceStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(20.dp).fillMaxWidth().alpha(keyAlpha),
        )
        AnimatedVisibility(
            visible = !revealed,
            modifier = Modifier.matchParentSize(),
            exit = fadeOut(tween(Motion.Medium)) + scaleOut(tween(Motion.Medium), targetScale = 1.08f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onReveal)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.recovery_reveal_hint),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private const val GROUPS_PER_ROW = 4
