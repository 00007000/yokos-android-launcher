package com.yokos.bb10launcher.overlay

import kotlin.math.abs
import kotlin.math.roundToInt

enum class PeekEdge { Left, Right }

enum class PeekLength(val fraction: Float) { Third(1f / 3f), Half(0.5f), Full(1f) }

enum class PeekPosition { Top, Center, Bottom }

/** Where the peek strip sits. Default: upper half of the left edge, clear of the back gesture's busiest area. */
data class PeekConfig(
    val edge: PeekEdge = PeekEdge.Left,
    val length: PeekLength = PeekLength.Half,
    val position: PeekPosition = PeekPosition.Top,
) {
    /** The strip's top and height in pixels, inside the usable vertical range [top, bottom). */
    fun span(top: Int, bottom: Int): Pair<Int, Int> {
        val available = (bottom - top).coerceAtLeast(0)
        val height = (available * length.fraction).roundToInt()
        val start = when (position) {
            PeekPosition.Top -> top
            PeekPosition.Center -> top + (available - height) / 2
            PeekPosition.Bottom -> bottom - height
        }
        return start to height
    }
}

/** Pure rules for the peek drag, so they can be unit-tested. */
object PeekGesture {
    /** Share of the panel that must be revealed for a release to open the Hub. */
    const val COMMIT_FRACTION = 0.4f

    /** Panel width as a share of the screen. */
    const val PANEL_FRACTION = 0.85f

    /** How far the panel is revealed (0..1) after dragging [dx] pixels in from [edge]. */
    fun progress(dx: Float, edge: PeekEdge, panelWidth: Float): Float {
        if (panelWidth <= 0f) return 0f
        val inward = if (edge == PeekEdge.Left) dx else -dx
        return (inward / panelWidth).coerceIn(0f, 1f)
    }

    /** Open the Hub if dragged far enough, or flung inward fast enough; an outward fling cancels. */
    fun shouldCommit(progress: Float, velocityX: Float, edge: PeekEdge, flingVelocity: Float): Boolean {
        val inwardVelocity = if (edge == PeekEdge.Left) velocityX else -velocityX
        return when {
            abs(inwardVelocity) >= flingVelocity -> inwardVelocity > 0
            else -> progress >= COMMIT_FRACTION
        }
    }
}
