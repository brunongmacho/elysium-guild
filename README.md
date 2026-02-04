# Elysium Guild Mobile App

A modern, high-performance Android application built with Jetpack Compose, designed for elite guild management. This app integrates directly with the Elysium Dashboard API to provide real-time tracking of world bosses, guild events, and member performance.

## 🚀 Version 1.1.3 (Stable)
*   **Global Stability Hardening**: Re-engineered core components to ensure 100% stability across all device manufacturers (Samsung, Xiaomi, Lenovo, Pixel). Fixed critical crashes related to rapid orientation changes and background process death.
*   **Android 14+ Compliance**: Fully compliant with modern Android requirements, including Foreground Service (FGS) type properties and exact alarm security handling.
*   **Premium Floating Bubble**: A circular, high-contrast boss timer bubble that floats over other apps. Optimized with a solid black background and zoomed icon for visibility on all screen types.
*   **Intelligent Permission Management**: Centralized permission tracking in the Settings screen. Handles Battery Optimization, Overlay, and Exact Alarm requests with direct deep-links to system settings.
*   **Resilient Data Layer**: Updated Room database with auto-migration safety and repositories with silent network failure handling to ensure a stutter-free experience.

## 🎯 Core Features

### 1. Boss Timers (Real-Time)
*   **Live Countdown**: Track precise spawn times for all world bosses.
*   **Floating Overlay**: Keep timers visible while playing with the premium circular bubble.
*   **Smart Filtering**: Categorize bosses by `Ready`, `Soon` (within 30m), and `Tracking`.
*   **Haptic Alerts**: Tactile feedback on status changes and pull-to-refresh actions.

### 2. Guild Events
*   **Dynamic Schedule**: Syncs with the dashboard to show daily/weekly guild activities.
*   **Synced Visuals**: Event cards share the same premium glassy style and stage logic as Boss cards.
*   **Precise Reminders**: Local exact alarms notify you exactly when the action starts, even on Android 14.

### 3. Leaderboards & Profiles
*   **Competitive Rankings**: Track top performers in `Attendance` and `Points`.
*   **Adaptive Podium**: High-contrast podium display that works perfectly in all lighting conditions.
*   **Detailed Statistics**: View personal participation history and point accrual.

### 4. Advanced Settings
*   **Permission Center**: One-tap status checks and fixes for notifications, battery, and overlays.
*   **Theme Engine**: Support for `Light`, `Dark`, and `System` modes with instant UI switching.
*   **Sound Selection**: Choose your preferred alert sound with live previews and robust resource fallbacks.

## 🛠 Tech Stack
*   **Language**: Kotlin 1.9+
*   **UI**: Jetpack Compose (Material 3)
*   **Architecture**: MVVM with Clean Architecture principles
*   **DI**: Hilt (Dagger)
*   **Database**: Room (Local caching with destructive migration safety)
*   **Networking**: Retrofit 2 + OkHttp 4
*   **Background**: WorkManager + Foreground Services
*   **Automation**: GitHub Actions (CI/CD)

## 🏗 Project Architecture
```
com.elysium.guild/
├── di/                  # Hilt modules for Network, Database, and Utils
├── ui/
│   ├── screens/         # BossTimers, Leaderboard, Events, Profile
│   ├── components/      # Centralized UI cards and glassy components
│   └── theme/           # Adaptive Material 3 Color Schemes
├── viewmodel/           # State management for all major screens
├── models/              # Robust data models with API mapping
├── repository/          # Data layer with detailed error reporting
├── database/            # Room Database and DAO definitions
├── widget/              # BossBubbleService and Overlay management
└── utils/               # Constants, UIUtils, Worker, and Notification Helper
```

## 📦 Setup & Deployment

### For Developers
1.  Open in **Android Studio Ladybug** (or later).
2.  Ensure **JDK 17** is configured.
3.  Set the `BASE_URL` in `Constants.kt` to point to your API instance.

### For Releasing Updates
The app uses a fully automated **GitHub Actions** pipeline:
1.  Update `versionCode` and `versionName` in `app/build.gradle`.
2.  Update `update-manifest.json` in the root folder.
3.  Push changes: `git push origin main`.
4.  Create a tag: `git tag v1.1.3 && git push origin v1.1.3`.
5.  GitHub will build the APK and attach it to the release automatically.

---
**Built with ❤️ by the Elysium Development Team**
