package com.yokos.bb10launcher.overlay

import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yokos.bb10launcher.R
import com.yokos.bb10launcher.launcher.LauncherActivity
import com.yokos.bb10launcher.launcherApp
import com.yokos.bb10launcher.ui.theme.Bb10Theme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * BB10 "peek" from any app. Draws a thin touch strip on one screen edge; dragging in from it
 * slides a compact Hub over the current app, and releasing far enough opens the full Hub.
 *
 * It only listens for window changes (to hide itself over the launcher and the lock screen) and
 * never reads the text or controls of other apps. On Android 11+ it also saves a small picture
 * of the app on screen for its Active Frame, unless the user turns that off.
 */
class PeekOverlayService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private val scope = MainScope()
    private val owner = OverlayLifecycleOwner()
    private val progress = mutableFloatStateOf(0f)

    private var config = PeekConfig()
    private var strip: View? = null
    private var stripAdded = false
    private var panel: ComposeView? = null
    private var animator: ValueAnimator? = null
    private var overLauncher = false
    private var immersive = false
    private var screenOff = false
    private var previewsEnabled = true
    private var capturer: FrameCapturer? = null

    /** Screen off / unlock: the lock screen doesn't always report a window change. */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOff = true
                    capturer?.stop()
                    animator?.cancel()
                    removePanel()
                }
                Intent.ACTION_USER_PRESENT -> screenOff = false
                Intent.ACTION_SCREEN_ON -> if (!keyguardLocked()) screenOff = false
            }
            updateStripVisibility()
        }
    }

    override fun onServiceConnected() {
        instance = this
        windowManager = getSystemService(WindowManager::class.java)
        owner.start()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        scope.launch {
            launcherApp.settings.peekConfig.collect {
                config = it
                layoutStrip()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            capturer = FrameCapturer(
                service = this,
                previews = launcherApp.framePreviews,
                isLaunchable = { it in launcherApp.apps.launchablePackages() },
                canCapture = { previewsEnabled && panel == null && !overLauncher && !isLocked() },
            )
            scope.launch {
                launcherApp.settings.framePreviews.collect { enabled ->
                    previewsEnabled = enabled
                    if (!enabled) capturer?.stop()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // System UI (shade, lock screen) doesn't count as leaving the current app.
        if (pkg != SYSTEM_UI) overLauncher = pkg == packageName
        updateStripVisibility()
        when {
            // Don't picture the shade or the lock screen as part of an app.
            pkg == SYSTEM_UI -> capturer?.stop()
            // Dialogs and the keyboard sit on top of the same app; only full-screen windows switch apps.
            event.isFullScreen -> capturer?.onForeground(pkg.takeIf { it != packageName && previewsEnabled })
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        capturer?.release()
        runCatching { unregisterReceiver(screenReceiver) }
        animator?.cancel()
        removePanel()
        if (stripAdded) strip?.let { runCatching { windowManager.removeView(it) } }
        strip = null
        stripAdded = false
        owner.destroy()
        scope.cancel()
        super.onDestroy()
    }

    private fun screenSize(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = resources.displayMetrics
            metrics.widthPixels to metrics.heightPixels
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    /** Adds the strip, or moves it after a settings change. */
    private fun layoutStrip() {
        val (_, height) = screenSize()
        val (top, stripHeight) = config.span(top = dp(STATUS_MARGIN_DP), bottom = height - dp(NAV_MARGIN_DP))
        val params = WindowManager.LayoutParams(
            dp(STRIP_WIDTH_DP),
            stripHeight,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or if (config.edge == PeekEdge.Left) Gravity.LEFT else Gravity.RIGHT
            y = top
        }
        val view = strip ?: createStrip().also { strip = it }
        view.background = handleDrawable()
        if (stripAdded) {
            windowManager.updateViewLayout(view, params)
        } else {
            windowManager.addView(view, params)
            stripAdded = true
        }
        view.post { excludeFromSystemGestures(view) }
        updateStripVisibility()
    }

    /** A faint line on the screen edge so the strip can be found. */
    private fun handleDrawable() = GradientDrawable(
        if (config.edge == PeekEdge.Left) GradientDrawable.Orientation.LEFT_RIGHT else GradientDrawable.Orientation.RIGHT_LEFT,
        intArrayOf(Color.argb(70, 0, 168, 223), Color.TRANSPARENT),
    )

    /** Asks gesture navigation to leave the strip to us (Android caps this at 200dp per edge). */
    private fun excludeFromSystemGestures(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.systemGestureExclusionRects = listOf(Rect(0, 0, view.width, view.height))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createStrip(): View = View(this).apply {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        val flingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity * FLING_MULTIPLIER.toFloat()
        var downX = 0f
        var dragging = false
        var tracker: VelocityTracker? = null

        setOnTouchListener { _, event ->
            val panelWidth = screenSize().first * PeekGesture.PANEL_FRACTION
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Never show the Hub over the lock screen.
                    if (isLocked()) return@setOnTouchListener false
                    downX = event.rawX
                    dragging = false
                    tracker = VelocityTracker.obtain().also { it.addMovement(event) }
                }
                MotionEvent.ACTION_MOVE -> {
                    tracker?.addMovement(event)
                    val dx = event.rawX - downX
                    if (!dragging && abs(dx) > slop) {
                        dragging = true
                        showPanel()
                    }
                    if (dragging) progress.floatValue = PeekGesture.progress(dx, config.edge, panelWidth)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    tracker?.addMovement(event)
                    tracker?.computeCurrentVelocity(1000)
                    val velocity = tracker?.xVelocity ?: 0f
                    tracker?.recycle()
                    tracker = null
                    if (dragging) {
                        val commit = event.actionMasked == MotionEvent.ACTION_UP &&
                            PeekGesture.shouldCommit(progress.floatValue, velocity, config.edge, flingVelocity)
                        if (commit) openHub() else retract()
                    }
                    dragging = false
                }
            }
            true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setOnApplyWindowInsetsListener { view, insets ->
                immersive = !insets.isVisible(WindowInsets.Type.statusBars())
                updateStripVisibility()
                view.onApplyWindowInsets(insets)
            }
        }
    }

    private fun keyguardLocked(): Boolean =
        getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true

    private fun isLocked(): Boolean = screenOff || keyguardLocked()

    private fun updateStripVisibility() {
        strip?.visibility = if (overLauncher || immersive || isLocked()) View.GONE else View.VISIBLE
    }

    private fun showPanel() {
        animator?.cancel()
        if (panel != null) return
        val view = ComposeView(this).apply {
            owner.attachTo(this)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val state by launcherApp.hub.state.collectAsStateWithLifecycle()
                Bb10Theme {
                    PeekPanel(progress = { progress.floatValue }, edge = config.edge, state = state)
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        windowManager.addView(view, params)
        panel = view
    }

    private fun removePanel() {
        panel?.let { runCatching { windowManager.removeView(it) } }
        panel = null
        progress.floatValue = 0f
    }

    private fun animateTo(target: Float, onEnd: () -> Unit) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(progress.floatValue, target).apply {
            duration = SETTLE_MILLIS
            interpolator = DecelerateInterpolator()
            addUpdateListener { progress.floatValue = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled) onEnd()
                }
            })
            start()
        }
    }

    private fun retract() = animateTo(0f) { removePanel() }

    private fun openHub() = animateTo(1f) {
        val options = ActivityOptions.makeCustomAnimation(this, R.anim.hub_enter, R.anim.hub_exit).toBundle()
        startActivity(LauncherActivity.hubIntent(this), options)
        // Keep the panel up until the Hub has slid in underneath it.
        panel?.postDelayed({ removePanel() }, HANDOFF_MILLIS)
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"
        private const val STRIP_WIDTH_DP = 14
        private const val STATUS_MARGIN_DP = 32
        private const val NAV_MARGIN_DP = 64
        private const val FLING_MULTIPLIER = 8
        private const val SETTLE_MILLIS = 180L
        private const val HANDOFF_MILLIS = 450L

        /** The running service, if the user has enabled it; used for the pull-down shortcut. */
        @Volatile
        var instance: PeekOverlayService? = null
            private set

        /** Opens the notification shade, like BB10's swipe down from the top of the home screen. */
        fun openNotificationShade(): Boolean =
            instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) ?: false
    }
}
