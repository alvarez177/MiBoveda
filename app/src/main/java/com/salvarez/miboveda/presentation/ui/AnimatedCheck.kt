package com.salvarez.miboveda.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salvarez.miboveda.presentation.theme.Motion

/** A circle that fills in and a check mark that is drawn stroke by stroke. Plays once when it appears. */
@Composable
fun AnimatedCheck(modifier: Modifier = Modifier, size: Dp = 28.dp, color: Color = MaterialTheme.colorScheme.primary) {
    val fill = remember { Animatable(0f) }
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        fill.animateTo(1f, tween(Motion.Medium, easing = Motion.EmphasizedDecelerate))
        draw.animateTo(1f, tween(Motion.Medium, easing = Motion.Emphasized))
    }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawCircle(color = color.copy(alpha = 0.16f * fill.value), radius = s / 2 * fill.value)
        drawCircle(color = color.copy(alpha = fill.value), radius = s / 2 * fill.value, style = Stroke(s * 0.07f))
        val check = Path().apply {
            moveTo(s * 0.28f, s * 0.52f)
            lineTo(s * 0.44f, s * 0.67f)
            lineTo(s * 0.73f, s * 0.36f)
        }
        val measure = PathMeasure().apply { setPath(check, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * draw.value, partial, true)
        drawPath(partial, color = color, style = Stroke(s * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
