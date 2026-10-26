package com.salvarez.miboveda.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.salvarez.miboveda.presentation.theme.Motion

private val ButtonShape = RoundedCornerShape(18.dp)

/** The main action of a screen: brand gradient, springy press, and a spinner that replaces the label while loading. */
@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val brand = brandBrush()
    val disabled = scheme.onSurface.copy(alpha = 0.12f)
    val content by animateColorAsState(if (enabled) scheme.onPrimary else scheme.onSurface.copy(alpha = 0.38f), tween(Motion.Short), label = "content")
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .heightIn(min = 56.dp)
            .clip(ButtonShape)
            .background(if (enabled) brand else Brush.linearGradient(listOf(disabled, disabled)))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = {
                (fadeIn(tween(Motion.Short)) + scaleIn(tween(Motion.Short), initialScale = 0.7f)) togetherWith
                    (fadeOut(tween(Motion.Short)) + scaleOut(tween(Motion.Short), targetScale = 0.7f))
            },
            label = "loading",
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = content, strokeWidth = 2.5.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                    Text(text, color = content, style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize))
                }
            }
        }
    }
}

/** A quieter action next to the main one: outline in the brand color, same springy press. */
@Composable
fun VaultOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val color = if (enabled) scheme.primary else scheme.onSurface.copy(alpha = 0.38f)
    val borderColor = if (enabled) scheme.primary.copy(alpha = 0.55f) else scheme.onSurface.copy(alpha = 0.12f)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .heightIn(min = 52.dp)
            .clip(ButtonShape)
            .border(BorderStroke(1.5.dp, borderColor), ButtonShape)
            .background(if (pressed) scheme.primary.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text, color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A text-only action, such as "forgot your password?". */
@Composable
fun VaultTextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) scheme.primary else scheme.onSurface.copy(alpha = 0.38f),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}
