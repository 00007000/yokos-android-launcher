package com.yokos.bb10launcher.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import androidx.annotation.RequiresApi
import com.yokos.bb10launcher.frames.FramePreviews
import com.yokos.bb10launcher.frames.PreviewPolicy
import java.util.concurrent.Executors

/**
 * Takes a small still picture of the app on screen (Android 11+) so its Active Frame can show it.
 * Only created on Android 11+; on older versions frames keep showing the app icon.
 * One picture shortly after the app opens, then a refresh every few seconds while it stays open,
 * so the picture is recent when the user leaves. Pictures stay in the app's cache on this phone.
 */
class FrameCapturer(
    private val service: AccessibilityService,
    private val previews: FramePreviews,
    private val isLaunchable: (String) -> Boolean,
    private val canCapture: () -> Boolean,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var current: String? = null

    private val tick = object : Runnable {
        override fun run() {
            // Screenshots from an accessibility service need Android 11.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) capture()
            handler.postDelayed(this, PreviewPolicy.CAPTURE_INTERVAL_MILLIS)
        }
    }

    /** [packageName] now fills the screen; null (or a non-launchable app) stops capturing. */
    fun onForeground(packageName: String?) {
        val target = packageName?.takeIf(isLaunchable)
        if (target == current) return
        current = target
        handler.removeCallbacks(tick)
        if (target != null) handler.postDelayed(tick, PreviewPolicy.FIRST_CAPTURE_DELAY_MILLIS)
    }

    fun stop() {
        current = null
        handler.removeCallbacks(tick)
    }

    fun release() {
        stop()
        worker.shutdown()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun capture() {
        val pkg = current ?: return
        if (!canCapture()) return
        service.takeScreenshot(
            Display.DEFAULT_DISPLAY,
            service.mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val buffer = result.hardwareBuffer
                    // The user may have switched apps while the picture was taken.
                    if (current != pkg) {
                        buffer.close()
                        return
                    }
                    worker.execute {
                        try {
                            val hardware = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace) ?: return@execute
                            val full = hardware.copy(Bitmap.Config.ARGB_8888, false)
                            hardware.recycle()
                            val (width, height) = PreviewPolicy.targetSize(full.width, full.height)
                            val small = Bitmap.createScaledBitmap(full, width, height, true)
                            if (small !== full) full.recycle()
                            if (!PreviewPolicy.isBlank(sample(small))) previews.save(pkg, small)
                            small.recycle()
                        } catch (_: RuntimeException) {
                            // A failed picture just keeps the previous one.
                        } finally {
                            buffer.close()
                        }
                    }
                }

                override fun onFailure(errorCode: Int) = Unit
            },
        )
    }

    /** Every 8th pixel of every 8th row: enough to spot an all-black (protected) screen. */
    private fun sample(bitmap: Bitmap): IntArray {
        val row = IntArray(bitmap.width)
        val out = ArrayList<Int>()
        for (y in 0 until bitmap.height step SAMPLE_STEP) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (x in row.indices step SAMPLE_STEP) out += row[x]
        }
        return out.toIntArray()
    }

    private companion object {
        const val SAMPLE_STEP = 8
    }
}
