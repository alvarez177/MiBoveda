package com.salvarez.miboveda.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing

/**
 * Durations and curves shared by every animation, so the app moves in one consistent way.
 * Compose already follows the system "animation scale" setting (0 turns animations off), so
 * nothing here needs to check it.
 */
object Motion {
    const val Short = 150
    const val Medium = 300
    const val Long = 450

    /** Default for things that move on screen. */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** For things that arrive: starts fast and settles. */
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** For things that leave: starts slow and ends fast. */
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}
