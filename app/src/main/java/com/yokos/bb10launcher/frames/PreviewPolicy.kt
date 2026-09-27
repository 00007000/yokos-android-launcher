package com.yokos.bb10launcher.frames

import kotlin.math.roundToInt

/** Pure rules for Active Frame preview pictures, unit-tested on the JVM. */
object PreviewPolicy {
    /** Previews are stored this wide; plenty for a half-screen card. */
    const val WIDTH = 360

    /** How long an app must be on screen before the first picture, so it has finished drawing. */
    const val FIRST_CAPTURE_DELAY_MILLIS = 1_200L

    /** Refresh while the app stays open, so the picture is recent when you leave it. */
    const val CAPTURE_INTERVAL_MILLIS = 6_000L

    /** Size to scale a [width]×[height] screenshot to, keeping its aspect ratio. */
    fun targetSize(width: Int, height: Int, maxWidth: Int = WIDTH): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 0 to 0
        if (width <= maxWidth) return width to height
        return maxWidth to (height.toLong() * maxWidth / width).toInt().coerceAtLeast(1)
    }

    /**
     * True when a picture is (almost) entirely black, which is what Android returns for protected
     * screens such as banking apps. Those are not saved.
     */
    fun isBlank(pixels: IntArray): Boolean {
        if (pixels.isEmpty()) return true
        return pixels.all { argb ->
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b).roundToInt() <= BLANK_LUMA
        }
    }

    private const val BLANK_LUMA = 8
}
