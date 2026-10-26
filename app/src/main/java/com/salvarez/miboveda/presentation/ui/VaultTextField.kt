package com.salvarez.miboveda.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.vector.ImageVector
import com.salvarez.miboveda.R
import com.salvarez.miboveda.presentation.theme.Motion

/** A text field in the vault style: tonal fill, rounded, an icon in front and a brand-colored edge when focused. */
@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        trailingIcon = trailingContent,
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceContainerHigh,
            unfocusedContainerColor = scheme.surfaceContainer,
            focusedBorderColor = scheme.primary,
            unfocusedBorderColor = scheme.outlineVariant,
            focusedLeadingIconColor = scheme.primary,
            focusedLabelColor = scheme.primary,
            cursorColor = scheme.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * A password field. When [onToggleVisibility] is given it shows an eye that morphs between open and
 * closed; the screen decides what [isVisible] means.
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    onToggleVisibility: (() -> Unit)? = null,
    isError: Boolean = false,
    leadingIcon: ImageVector = Icons.Rounded.Lock,
    extraTrailing: @Composable (() -> Unit)? = null,
) {
    VaultTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = leadingIcon,
        isError = isError,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        trailingContent = if (onToggleVisibility != null || extraTrailing != null) {
            {
                extraTrailing?.invoke()
                if (onToggleVisibility != null) {
                    IconButton(onClick = onToggleVisibility) {
                        AnimatedContent(
                            targetState = isVisible,
                            transitionSpec = {
                                (fadeIn(tween(Motion.Short)) + scaleIn(tween(Motion.Short), initialScale = 0.6f)) togetherWith
                                    (fadeOut(tween(Motion.Short)) + scaleOut(tween(Motion.Short), targetScale = 0.6f))
                            },
                            label = "eye",
                        ) { visible ->
                            Icon(
                                imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = stringResource(if (visible) R.string.hide_password else R.string.show_password),
                            )
                        }
                    }
                }
            }
        } else {
            null
        },
    )
}
