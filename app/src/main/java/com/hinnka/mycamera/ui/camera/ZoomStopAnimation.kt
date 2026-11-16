package com.hinnka.mycamera.ui.camera

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

private const val MinZoomStopAnimationDurationMillis = 280
private const val MaxZoomStopAnimationDurationMillis = 520
private const val ZoomStopAnimationMillisPerOctave = 100f

/**
 * Symmetric S-shaped cubic Bezier used for every focal-length (zoom stop) transition.
 *
 * Control points P1 = (0.78, 0) and P2 = (0.26, 1): the zoom starts very gently, accelerates
 * through an almost vertical middle section and lands softly on the target ratio. Because the
 * motion is concentrated in the middle of the timeline, the durations above are slightly longer
 * than with the previous FastOutSlowIn curve so the steep part never looks like a jump.
 */
internal val ZoomStopEasing: Easing = CubicBezierEasing(0.78f, 0f, 0.26f, 1f)

internal fun interpolateZoomRatio(
    startZoom: Float,
    targetZoom: Float,
    progress: Float
): Float {
    require(startZoom.isFinite() && startZoom > 0f)
    require(targetZoom.isFinite() && targetZoom > 0f)
    val fraction = progress.coerceIn(0f, 1f)
    val startLog = ln(startZoom.toDouble())
    val targetLog = ln(targetZoom.toDouble())
    return exp(startLog + (targetLog - startLog) * fraction).toFloat()
}

internal fun resolveZoomStopAnimationDurationMillis(
    startZoom: Float,
    targetZoom: Float
): Int {
    require(startZoom.isFinite() && startZoom > 0f)
    require(targetZoom.isFinite() && targetZoom > 0f)
    val octaveDistance = abs(ln((targetZoom / startZoom).toDouble()) / ln(2.0))
    return (MinZoomStopAnimationDurationMillis +
        octaveDistance * ZoomStopAnimationMillisPerOctave)
        .roundToInt()
        .coerceIn(
            MinZoomStopAnimationDurationMillis,
            MaxZoomStopAnimationDurationMillis
        )
}
