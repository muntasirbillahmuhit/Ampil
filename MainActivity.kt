package com.ampil.app

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * MainActivity
 *
 * Provides status verification for the VolumeAccessibilityService,
 * a direct shortcut to Android Accessibility Settings, and testing controls.
 */
class MainActivity : Activity() {

    private lateinit var statusBadge: TextView
    private lateinit var statusDescription: TextView
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val rootScrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0A0A0A"))
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(48), dpToPx(24), dpToPx(32))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // App Header
        val titleText = TextView(this).apply {
            text = "Ampil"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val subtitleText = TextView(this).apply {
            text = "Hardware-free Gesture & Floating Volume Control"
            textSize = 14f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(4), 0, dpToPx(32))
        }

        // Status Card
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#141416"))
                cornerRadius = dpToPx(20).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#1AFFFFFF"))
            }
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        statusBadge = TextView(this).apply {
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(16), dpToPx(6), dpToPx(16), dpToPx(6))
            gravity = Gravity.CENTER
        }

        statusDescription = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(12), 0, 0)
        }

        statusCard.addView(statusBadge)
        statusCard.addView(statusDescription)

        // Enable Accessibility Button
        val btnEnable = Button(this).apply {
            text = "Enable Accessibility"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                colors = intArrayOf(Color.parseColor("#9B8CFF"), Color.parseColor("#5B47EB"))
                cornerRadius = dpToPx(16).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(56)
            ).apply {
                setMargins(0, dpToPx(24), 0, dpToPx(16))
            }
            setOnClickListener {
                openAccessibilitySettings()
            }
        }

        // Quick Test Buttons Section
        val testHeader = TextView(this).apply {
            text = "Test Volume Controls"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(16), 0, dpToPx(8))
        }

        val testButtonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val btnLower = createTestButton("– Vol") {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        }
        val btnRaise = createTestButton("+ Vol") {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        }
        val btnPanel = createTestButton("Show Panel") {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        }

        testButtonsLayout.addView(btnLower)
        testButtonsLayout.addView(btnRaise)
        testButtonsLayout.addView(btnPanel)

        // Instructions Footer
        val instructions = TextView(this).apply {
            text = "Tip: Once enabled, a floating volume bubble appears on the screen.\n• Tap to toggle quick ± controls\n• Drag to reposition or snap to edge\n• Long-press to open full system volume panel"
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, dpToPx(24), 0, dpToPx(16))
            gravity = Gravity.CENTER
        }

        layout.addView(titleText)
        layout.addView(subtitleText)
        layout.addView(statusCard)
        layout.addView(btnEnable)
        layout.addView(testHeader)
        layout.addView(testButtonsLayout)
        layout.addView(instructions)

        rootScrollView.addView(layout)
        setContentView(rootScrollView)
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    /**
     * Checks if VolumeAccessibilityService is currently enabled in system settings.
     */
    fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
        val expectedServiceName = ComponentName(context, serviceClass).flattenToString()
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun updateServiceStatus() {
        val isEnabled = isAccessibilityServiceEnabled(this, VolumeAccessibilityService::class.java)

        if (isEnabled) {
            statusBadge.text = "● Service Active"
            statusBadge.setTextColor(Color.parseColor("#34E0A1"))
            statusBadge.background = GradientDrawable().apply {
                setColor(Color.parseColor("#1F34E0A1"))
                cornerRadius = dpToPx(12).toFloat()
            }
            statusDescription.text = "Floating volume button is ready and active."
        } else {
            statusBadge.text = "○ Service Disabled"
            statusBadge.setTextColor(Color.parseColor("#FF5C6C"))
            statusBadge.background = GradientDrawable().apply {
                setColor(Color.parseColor("#1FFF5C6C"))
                cornerRadius = dpToPx(12).toFloat()
            }
            statusDescription.text = "Tap 'Enable Accessibility' below to turn on Ampil."
        }
    }

    private fun createTestButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1D"))
                cornerRadius = dpToPx(12).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#22FFFFFF"))
            }
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(44), 1f).apply {
                setMargins(dpToPx(4), 0, dpToPx(4), 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
