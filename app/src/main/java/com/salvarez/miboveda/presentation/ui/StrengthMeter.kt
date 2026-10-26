package com.salvarez.miboveda.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.salvarez.miboveda.R
import com.salvarez.miboveda.domain.model.PasswordStrength
import com.salvarez.miboveda.presentation.theme.Motion

/** Three segments that light up one by one, changing color from red to amber to turquoise, with the label sliding in. */
@Composable
fun StrengthMeter(strength: PasswordStrength, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val filled = when (strength) {
        PasswordStrength.WEAK -> 1
        PasswordStrength.MEDIUM -> 2
        PasswordStrength.STRONG -> 3
    }
    val color by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.WEAK -> scheme.error
            PasswordStrength.MEDIUM -> scheme.tertiary
            PasswordStrength.STRONG -> scheme.primary
        },
        animationSpec = tween(Motion.Medium),
        label = "strength-color",
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index ->
                val segment by animateColorAsState(
                    targetValue = if (index < filled) color else scheme.outlineVariant,
                    animationSpec = tween(Motion.Medium, delayMillis = index * 60),
                    label = "segment",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(segment, RoundedCornerShape(3.dp)),
                )
            }
        }
        AnimatedContent(
            targetState = strength,
            transitionSpec = {
                (slideInVertically(tween(Motion.Medium, easing = Motion.EmphasizedDecelerate)) { it / 2 } + fadeIn(tween(Motion.Medium))) togetherWith
                    (slideOutVertically(tween(Motion.Short)) { -it / 2 } + fadeOut(tween(Motion.Short)))
            },
            label = "strength-label",
        ) { value ->
            Text(
                text = stringResource(
                    when (value) {
                        PasswordStrength.WEAK -> R.string.strength_weak
                        PasswordStrength.MEDIUM -> R.string.strength_medium
                        PasswordStrength.STRONG -> R.string.strength_strong
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}
