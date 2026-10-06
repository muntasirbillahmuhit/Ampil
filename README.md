# Ampil 🔊

Control your device volume intuitively using simple, accessible screen gestures powered by Android Accessibility Services and modern Web technologies.

---

## 🌟 Key Features

1. **Hardware-Free Volume Control**: Adjust system volume, media, alarm, notification, and ring volumes without using physical volume rocker buttons.
2. **Floating Accessibility Bubble (Android Native)**:
   - Attached via `TYPE_ACCESSIBILITY_OVERLAY` (functions over all apps and on the lock screen without requiring `SYSTEM_ALERT_WINDOW`).
   - **Single Tap**: Opens a sleek on-screen mini mixer with live percentage indicators, stream switchers (Media, Ring, Alarm, Notification), and `+`/`–`/`Mute` buttons.
   - **Long Press**: Triggers Android's native system volume slider panel (`FLAG_SHOW_UI`).
   - **Drag & Snap**: Draggable anywhere on the screen with smooth physics-based edge snapping.
3. **Concentric Multi-Ring Interface (Web & App)**:
   - Real-time circular gesture ring dial for multiple audio channels simultaneously.
   - Web Audio API synthesizer tones provide real-time audio feedback (Media @ 440Hz, Ring @ 587Hz, Alarm @ 880Hz, Notification @ 659Hz).
   - "Mute All" and "Vibrate Mode" quick switches.
4. **Android 13+ Sideload Compatibility**:
   - Built-in guidance and shortcuts for unlocking "Restricted Settings" on sideloaded APKs.

---

## 🚀 Architecture & File Structure

```
├── AndroidManifest.xml                    # Android Manifest with Accessibility Service declaration
├── VolumeAccessibilityService.kt          # Accessibility Service with floating overlay & volume triggers
├── MainActivity.kt                        # Status checker, settings launch intent & restricted setting handler
├── res/
│   ├── xml/accessibility_service_config.xml # Accessibility service configuration
│   └── values/strings.xml, styles.xml     # String resources & dark theme styles
├── index.html                             # Responsive concentric ring UI with Web Audio synthesis
├── server.js                              # Express static web server (Node.js runtime)
├── capacitor.config.json                  # Capacitor cross-platform bridge config
├── .github/workflows/main.yml             # GitHub Actions automated APK build & artifact upload
└── package.json                           # Scripts & dependencies
```

---

## 📱 How to Enable on Android

1. Install the `ampil` APK on your Android device.
2. **For Android 13+ (Sideloaded APKs)**:
   - Open **App Info** for Ampil (or tap "Allow Restricted Settings" inside the app).
   - Tap the 3 dots in the top right corner &rarr; select **"Allow restricted settings"** &rarr; authenticate with PIN / fingerprint.
3. Open **Accessibility Settings**:
   - Navigate to **Downloaded Services / Installed Apps**.
   - Select **Ampil** and toggle it **ON**.
4. The floating volume bubble will appear on your screen immediately.

---

## 🛠️ Automated CI / APK Building

The included GitHub Actions workflow (`.github/workflows/main.yml`) automatically builds a debug APK on push or manual dispatch using JDK 21 and Capacitor, outputting a downloadable `ampil-APK` artifact in GitHub Actions.
