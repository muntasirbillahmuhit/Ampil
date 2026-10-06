package com.ampil.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * VolumeAccessibilityService
 *
 * An accessibility service providing floating on-screen volume controls and gesture support
 * to adjust device volume without requiring physical hardware buttons.
 *
 * Uses TYPE_ACCESSIBILITY_OVERLAY in onServiceConnected() so it functions system-wide
 * (even over lock screen) without requiring SYSTEM_ALERT_WINDOW permission.
 */
class VolumeAccessibilityService : AccessibilityService() {

    private lateinit var audioManager: AudioManager
    private lateinit var windowManager: WindowManager
    private var vibrator: Vibrator? = null

    private var floatingRootView: View? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isExpanded = false

    companion object {
        private const val TAG = "VolumeAccService"
        var isServiceRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        Log.d(TAG, "VolumeAccessibilityService onCreate")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        Log.d(TAG, "VolumeAccessibilityService onServiceConnected")

        // Ensure service configuration is fully applied
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            serviceInfo = info
        } catch (e: Exception) {
            Log.e(TAG, "Error applying serviceInfo", e)
        }

        // Overlay MUST be created in onServiceConnected (after window token is assigned)
        if (floatingRootView == null) {
            handler.post {
                createFloatingWidget()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing hook
    }

    override fun onInterrupt() {
        Log.w(TAG, "VolumeAccessibilityService onInterrupt")
    }

    /**
     * Triggers the native system volume UI slider.
     */
    fun showVolumePanel() {
        vibrate(35)
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_SAME,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing volume panel", e)
        }
    }

    /**
     * Increases volume for the designated audio stream.
     */
    fun increaseVolume(stream: Int = AudioManager.STREAM_MUSIC) {
        vibrate(15)
        try {
            audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {
            Log.e(TAG, "Error increasing volume", e)
        }
    }

    /**
     * Decreases volume for the designated audio stream.
     */
    fun decreaseVolume(stream: Int = AudioManager.STREAM_MUSIC) {
        vibrate(15)
        try {
            audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {
            Log.e(TAG, "Error decreasing volume", e)
        }
    }

    /**
     * Toggles mute on the given stream.
     */
    fun toggleMute(stream: Int = AudioManager.STREAM_MUSIC) {
        vibrate(25)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
            } else {
                val current = audioManager.getStreamVolume(stream)
                if (current > 0) {
                    audioManager.setStreamVolume(stream, 0, AudioManager.FLAG_SHOW_UI)
                } else {
                    val max = audioManager.getStreamMaxVolume(stream)
                    audioManager.setStreamVolume(stream, max / 2, AudioManager.FLAG_SHOW_UI)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling mute", e)
        }
    }

    /**
     * Creates a floating overlay widget attached via TYPE_ACCESSIBILITY_OVERLAY.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingWidget() {
        if (floatingRootView != null) return

        try {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels

            val layoutFlag = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = dpToPx(16)
                y = dpToPx(220)
            }
            windowLayoutParams = params

            // Main Container
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }

            // Quick Controls Panel (Initially collapsed)
            val quickPanel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#EE141416"))
                    cornerRadius = dpToPx(20).toFloat()
                    setStroke(dpToPx(1), Color.parseColor("#33FFFFFF"))
                }
            }

            val btnPlus = createQuickButton("+") { increaseVolume() }
            val btnMinus = createQuickButton("-") { decreaseVolume() }
            val btnSystem = createQuickButton("⚙") { showVolumePanel() }

            quickPanel.addView(btnPlus)
            quickPanel.addView(btnMinus)
            quickPanel.addView(btnSystem)

            // Floating Bubble Button
            val bubbleSize = dpToPx(56)
            val bubble = FrameLayout(this).apply {
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    colors = intArrayOf(Color.parseColor("#9B8CFF"), Color.parseColor("#6347EB"))
                    gradientType = GradientDrawable.RADIAL_GRADIENT
                    gradientRadius = (bubbleSize / 1.2).toFloat()
                }
                background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    RippleDrawable(ColorStateList.valueOf(Color.parseColor("#44FFFFFF")), bgDrawable, null)
                } else {
                    bgDrawable
                }
                elevation = dpToPx(8).toFloat()
            }

            val icon = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_lock_silent_mode_off)
                setColorFilter(Color.WHITE)
                val iconPadding = dpToPx(14)
                setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            }
            bubble.addView(icon, FrameLayout.LayoutParams(bubbleSize, bubbleSize))

            container.addView(quickPanel)
            container.addView(bubble)

            // Touch handling
            val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
            val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()

            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false

            val longPressRunnable = Runnable {
                if (!isDragging) {
                    showVolumePanel()
                }
            }

            bubble.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        handler.postDelayed(longPressRunnable, longPressTimeout)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()

                        if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                            isDragging = true
                            handler.removeCallbacks(longPressRunnable)
                        }

                        if (isDragging) {
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager.updateViewLayout(container, params)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating overlay layout", e)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        handler.removeCallbacks(longPressRunnable)
                        if (!isDragging) {
                            vibrate(15)
                            isExpanded = !isExpanded
                            quickPanel.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        } else {
                            val targetX = if (params.x + bubbleSize / 2 < screenWidth / 2) dpToPx(12) else screenWidth - bubbleSize - dpToPx(12)
                            animateSnap(container, params, targetX)
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(longPressRunnable)
                        true
                    }
                    else -> false
                }
            }

            floatingRootView = container
            windowManager.addView(container, params)
            Log.d(TAG, "Floating overlay successfully attached to window manager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach floating overlay", e)
        }
    }

    private fun createQuickButton(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val size = dpToPx(38)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(4))
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#33FFFFFF"))
                cornerRadius = dpToPx(12).toFloat()
            }
            setOnClickListener {
                onClick()
            }
        }
    }

    private fun animateSnap(view: View, params: WindowManager.LayoutParams, targetX: Int) {
        val startX = params.x
        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }
        animator.start()
    }

    private fun vibrate(milliseconds: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(milliseconds)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        handler.removeCallbacksAndMessages(null)
        floatingRootView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay", e)
            }
        }
        floatingRootView = null
    }
}
