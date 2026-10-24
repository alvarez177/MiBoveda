package com.salvarez.miboveda.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.salvarez.miboveda.R

/** Shows a recovery key in readable rows and lets the user copy it. */
@Composable
fun RecoveryKeyPanel(recoveryKey: String, onCopy: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = recoveryKey.split('-').chunked(GROUPS_PER_ROW).joinToString("\n") { it.joinToString("-") },
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
        OutlinedButton(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.copy_recovery_key))
        }
    }
}

private const val GROUPS_PER_ROW = 4
