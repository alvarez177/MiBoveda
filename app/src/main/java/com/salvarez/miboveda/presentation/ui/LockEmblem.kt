package com.salvarez.miboveda.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salvarez.miboveda.presentation.theme.Motion
import kotlin.math.cos
import kotlin.math.sin

/**
 * The emblem of the app: a vault dial with a padlock in the middle.
 *
 * - It breathes (a soft glow) while it waits.
 * - [isOpen] turns the dial a full turn and lifts the shackle.
 * - Each increase of [shakeSignal] shakes it sideways, for a wrong password.
 * - [isError] tints it red.
 */
@Composable
fun LockEmblem(
    isOpen: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    shakeSignal: Int = 0,
    isError: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val lift by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(Motion.Long, easing = Motion.Emphasized),
        label = "shackle",
    )
    val dial = remember { Animatable(0f) }
    LaunchedEffect(isOpen) {
        if (isOpen) dial.animateTo(dial.value + 360f, tween(Motion.Long + 250, easing = Motion.Emphasized))
    }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeSignal) {
        if (shakeSignal > 0) {
            repeat(3) {
                shake.animateTo(14f, tween(45))
                shake.animateTo(-14f, tween(45))
            }
            shake.animateTo(0f, tween(45))
        }
    }
    val pulse by rememberInfiniteTransition(label = "breathing").animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val accent by animateColorAsState(if (isError) scheme.error else scheme.primary, tween(Motion.Medium), label = "accent")
    val brand = brandBrush()
    val plateTop = scheme.surfaceContainerHigh
    val plateBottom = scheme.surfaceContainerLowest
    val tick = scheme.outline
    val keyhole = scheme.background

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { translationX = shake.value * density }
            .clearAndSetSemantics { },
    ) {
        val s = this.size.minDimension
        val c = center

        // Soft glow behind everything.
        drawCircle(
            brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.38f * pulse), Color.Transparent), center = c, radius = s * 0.62f),
            radius = s * 0.62f,
            center = c,
        )

        // Dial ring with tick marks; it is the part that spins.
        rotate(dial.value, c) {
            val outer = s * 0.485f
            for (i in 0 until 60) {
                val major = i % 5 == 0
                val length = if (major) s * 0.05f else s * 0.026f
                val angle = Math.toRadians(i * 6.0)
                val start = Offset(c.x + (outer - length) * cos(angle).toFloat(), c.y + (outer - length) * sin(angle).toFloat())
                val end = Offset(c.x + outer * cos(angle).toFloat(), c.y + outer * sin(angle).toFloat())
                drawLine(
                    color = if (major) accent.copy(alpha = 0.9f) else tick.copy(alpha = 0.7f),
                    start = start,
                    end = end,
                    strokeWidth = if (major) s * 0.014f else s * 0.008f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // The plate the padlock sits on.
        drawCircle(
            brush = Brush.linearGradient(listOf(plateTop, plateBottom), start = Offset.Zero, end = Offset(s, s)),
            radius = s * 0.36f,
            center = c,
        )
        drawCircle(color = accent.copy(alpha = 0.55f), radius = s * 0.36f, center = c, style = Stroke(s * 0.012f))

        // Padlock.
        val bodyW = s * 0.30f
        val bodyH = s * 0.22f
        val bodyLeft = c.x - bodyW / 2
        val bodyTop = c.y - bodyH * 0.05f
        val radius = bodyW * 0.31f
        val shackleTop = bodyTop - s * 0.17f
        val shackle = Path().apply {
            moveTo(c.x - radius, bodyTop)
            lineTo(c.x - radius, shackleTop + radius)
            arcTo(Rect(c.x - radius, shackleTop, c.x + radius, shackleTop + 2 * radius), 180f, 180f, false)
            lineTo(c.x + radius, bodyTop)
        }
        withTransform({
            translate(top = -s * 0.05f * lift)
            rotate(degrees = -26f * lift, pivot = Offset(c.x - radius, bodyTop))
        }) {
            drawPath(shackle, color = accent, style = Stroke(s * 0.05f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawRoundRect(
            brush = brand,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(s * 0.05f),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), startY = bodyTop, endY = bodyTop + bodyH * 0.6f),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(s * 0.05f),
        )
        // Keyhole.
        val holeCenter = Offset(c.x, bodyTop + bodyH * 0.42f)
        drawCircle(color = keyhole, radius = s * 0.027f, center = holeCenter)
        drawRoundRect(
            color = keyhole,
            topLeft = Offset(c.x - s * 0.007f, holeCenter.y),
            size = Size(s * 0.014f, s * 0.05f),
            cornerRadius = CornerRadius(s * 0.007f),
        )
    }
}
