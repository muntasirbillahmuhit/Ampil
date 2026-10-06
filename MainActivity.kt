package com.ampil.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * MainActivity
 *
 * Provides service verification, direct accessibility activation intent,
 * Android 13+ "Restricted Setting" unblocker, and Do Not Disturb / Notification Policy
 * access for unrestricted full-power volume control.
 */
class MainActivity : Activity() {

    private lateinit var statusBadge: TextView
    private lateinit var statusDescription: TextView
    private lateinit var dndStatusText: TextView
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val rootScrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0A0A0A"))
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(36), dpToPx(20), dpToPx(32))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // App Title
        val titleText = TextView(this).apply {
            text = "Ampil"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val subtitleText = TextView(this).apply {
            text = "Full-Power Hardware-Free Volume & Gesture Engine"
            textSize = 13f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(4), 0, dpToPx(20))
        }

        // Live Service Status Card
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#141416"))
                cornerRadius = dpToPx(18).toFloat()
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
            setPadding(0, dpToPx(8), 0, 0)
        }

        dndStatusText = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#34E0A1"))
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(6), 0, 0)
        }

        statusCard.addView(statusBadge)
        statusCard.addView(statusDescription)
        statusCard.addView(dndStatusText)

        // Action 1: Enable Accessibility Button
        val btnEnable = Button(this).apply {
            text = "1. Enable Accessibility Service"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                colors = intArrayOf(Color.parseColor("#9B8CFF"), Color.parseColor("#5B47EB"))
                cornerRadius = dpToPx(16).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(52)
            ).apply {
                setMargins(0, dpToPx(18), 0, dpToPx(10))
            }
            setOnClickListener {
                openAccessibilitySettings()
            }
        }

        // Action 2: Grant DND / Notification Policy Access (Full Power for Ringer / Silent Mode)
        val btnDndAccess = Button(this).apply {
            text = "2. Grant Full DND & Ring Mode Power"
            setTextColor(Color.parseColor("#34E0A1"))
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1D"))
                cornerRadius = dpToPx(14).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#3334E0A1"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            ).apply {
                setMargins(0, 0, 0, dpToPx(10))
            }
            setOnClickListener {
                openNotificationPolicySettings()
            }
        }

        // Action 3: Android 13+ Restricted Setting Helper Button
        val btnRestrictedSettings = Button(this).apply {
            text = "⚠️ Android 13+ Sideload? Allow Restricted Settings"
            setTextColor(Color.parseColor("#FFB52E"))
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1D"))
                cornerRadius = dpToPx(14).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#33FFB52E"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(46)
            ).apply {
                setMargins(0, 0, 0, dpToPx(16))
            }
            setOnClickListener {
                openAppDetailsSettings()
            }
        }

        // Step-by-Step Instructions Card
        val guideCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#121214"))
                cornerRadius = dpToPx(16).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#10FFFFFF"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val guideTitle = TextView(this).apply {
            text = "Full Volume Power Setup:"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dpToPx(8))
        }

        val guideSteps = TextView(this).apply {
            text = "1. If toggle is grayed out: Tap 'Allow Restricted Settings' → 3-dots top right → 'Allow restricted settings'.\n\n2. Tap 'Enable Accessibility Service' → Select 'Ampil' → Toggle ON.\n\n3. Tap 'Grant Full DND & Ring Mode Power' to allow Ampil to control silent, vibrate, and ringer volume modes without restriction."
            textSize = 12f
            setTextColor(Color.parseColor("#888899"))
            setLineSpacing(dpToPx(2).toFloat(), 1.1f)
        }

        guideCard.addView(guideTitle)
        guideCard.addView(guideSteps)

        // Quick Test Section
        val testHeader = TextView(this).apply {
            text = "Direct Audio Test Controls"
            textSize = 13.5f
            setTextColor(Color.parseColor("#777788"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(20), 0, dpToPx(8))
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
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND)
        }
        val btnRaise = createTestButton("+ Vol") {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND)
        }
        val btnPanel = createTestButton("Show Panel") {
            audioManager.adjustSuggestedStreamVolume(AudioManager.ADJUST_SAME, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND)
        }

        testButtonsLayout.addView(btnLower)
        testButtonsLayout.addView(btnRaise)
        testButtonsLayout.addView(btnPanel)

        layout.addView(titleText)
        layout.addView(subtitleText)
        layout.addView(statusCard)
        layout.addView(btnEnable)
        layout.addView(btnDndAccess)
        layout.addView(btnRestrictedSettings)
        layout.addView(guideCard)
        layout.addView(testHeader)
        layout.addView(testButtonsLayout)

        rootScrollView.addView(layout)
        setContentView(rootScrollView)
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openNotificationPolicySettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openAppDetailsSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if VolumeAccessibilityService is currently enabled using multiple system checks.
     */
    fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
        if (VolumeAccessibilityService.isServiceRunning) {
            return true
        }

        try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            val enabledList = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            if (enabledList != null) {
                for (info in enabledList) {
                    val sInfo = info.resolveInfo?.serviceInfo
                    if (sInfo != null && sInfo.packageName == context.packageName &&
                        (sInfo.name == serviceClass.name || sInfo.name.endsWith("VolumeAccessibilityService"))) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback
        }

        try {
            val expectedServiceName = ComponentName(context, serviceClass).flattenToString()
            val shortExpected = ComponentName(context, serviceClass).flattenToShortString()
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                    componentName.equals(shortExpected, ignoreCase = true) ||
                    componentName.contains("VolumeAccessibilityService", ignoreCase = true)) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore
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
            statusDescription.text = "Floating volume button is active on screen."
        } else {
            statusBadge.text = "○ Service Inactive"
            statusBadge.setTextColor(Color.parseColor("#FF5C6C"))
            statusBadge.background = GradientDrawable().apply {
                setColor(Color.parseColor("#1FFF5C6C"))
                cornerRadius = dpToPx(12).toFloat()
            }
            statusDescription.text = "Accessibility permission is required for on-screen controls."
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasDnd = notificationManager.isNotificationPolicyAccessGranted
            dndStatusText.text = if (hasDnd) "✓ Full DND & Ringer Access Granted" else "⚠️ DND / Ringer Mode Access Not Granted (Optional)"
            dndStatusText.setTextColor(if (hasDnd) Color.parseColor("#34E0A1") else Color.parseColor("#FFB52E"))
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
