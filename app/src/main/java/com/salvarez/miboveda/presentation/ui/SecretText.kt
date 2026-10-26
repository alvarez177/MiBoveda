package com.salvarez.miboveda.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

private const val MASK = "••••••••••"
private const val SCRAMBLE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789#\$%&*"
private const val STEPS = 14
private const val STEP_MILLIS = 28L

/**
 * Shows a secret as dots, or, when [revealed], "decrypts" it: random characters flicker and settle on the
 * real ones from left to right. The mask has a fixed length, so it does not hint at how long the secret is.
 */
@Composable
fun SecretText(
    secret: String,
    revealed: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    var shown by remember { mutableStateOf(if (revealed) secret else MASK) }
    LaunchedEffect(secret, revealed) {
        if (!revealed) {
            shown = MASK
            return@LaunchedEffect
        }
        for (step in 1..STEPS) {
            val settled = secret.length * step / STEPS
            shown = buildString {
                secret.forEachIndexed { index, char ->
                    append(if (index < settled || char == ' ') char else SCRAMBLE_ALPHABET.random())
                }
            }
            delay(STEP_MILLIS)
        }
        shown = secret
    }
    Text(text = shown, style = style, modifier = modifier)
}
