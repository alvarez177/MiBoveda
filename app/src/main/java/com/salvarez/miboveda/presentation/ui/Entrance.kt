package com.salvarez.miboveda.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.salvarez.miboveda.presentation.theme.Motion
import kotlinx.coroutines.delay

/**
 * Makes an element rise and fade in, [index] steps after the first one, so a group enters as a cascade.
 * With [enabled] false (already shown before) it appears at once.
 */
fun Modifier.cascadeEntrance(index: Int, enabled: Boolean = true, stepMillis: Int = 55): Modifier = composed {
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (enabled) {
            delay((index.coerceAtMost(8) * stepMillis).toLong())
            progress.animateTo(1f, tween(Motion.Long, easing = Motion.EmphasizedDecelerate))
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 36.dp.toPx()
    }
}

@Composable
internal fun rememberShownIds(): MutableSet<Long> = remember { mutableSetOf() }


