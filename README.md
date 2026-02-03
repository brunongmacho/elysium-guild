# Elysium Guild Mobile App

A modern, high-performance Android application built with Jetpack Compose, designed for elite guild management. This app integrates directly with the Elysium Dashboard API to provide real-time tracking of world bosses, guild events, and member performance.

## 🚀 Version 1.0.10 (Current)
*   **Deep Theme Synchronization**: All screens (Boss Timers, Events, Leaderboard) now perfectly follow the system appearance with unified color palettes.
*   **Light Mode Optimization**: Overhauled UI contrast for Light Mode, including a new vibrant **Azure Blue** for the Tracking stage and high-contrast status badges.
*   **Architecture Centralization**: Unified all UI colors, labels, work names, and intent extras into a single source of truth in `Constants.kt`.
*   **Modern Notification Engine**: Notification sounds are now managed via modern Android Notification Channels, ensuring reliable sound playback on Android 8.0+.
*   **Performance Tuning**: Implemented localized recomposition scopes for cards to ensure silky smooth scrolling even during active timer updates.
*   **Robust Data Mapping**: Added comprehensive `@SerializedName` support to ensure stable API communication regardless of backend naming conventions.

## 🎯 Core Features

### 1. Boss Timers (Real-Time)
*   **Live Countdown**: Track precise spawn times for all world bosses.
*   **Smart Filtering**: Categorize bosses by `Ready`, `Soon` (within 30m), and `Tracking`.
*   **Haptic Alerts**: Tactile feedback on status changes and pull-to-refresh actions.
*   **Visual Progress**: Adaptive status colors and progress bars optimized for both Light and Dark themes.
*   **Call to Arms**: Quick-share boss status to Discord/Clipboard with one tap.

### 2. Guild Events
*   **Dynamic Schedule**: Syncs with the dashboard to show daily/weekly guild activities.
*   **Synced Visuals**: Event cards now share the same premium visual style and stage logic as Boss cards.
*   **Precise Reminders**: Local exact alarms notify you exactly when the action starts.

### 3. Leaderboards & Profiles
*   **Competitive Rankings**: Track top performers in `Attendance` and `Points`.
*   **Adaptive Podium**: High-contrast podium display that works perfectly in all lighting conditions.
*   **Detailed Statistics**: View personal participation history and point accrual.

### 4. Advanced Settings
*   **Theme Engine**: Support for `Light`, `Dark`, and `System` modes with instant UI switching.
*   **Sound Selection**: Choose your preferred alert sound with live previews.
*   **Permission Auditor**: Real-time monitoring of required system permissions.

## 🛠 Tech Stack
*   **Language**: Kotlin 1.9+
*   **UI**: Jetpack Compose (Material 3)
*   **Architecture**: MVVM with Clean Architecture principles
*   **DI**: Hilt (Dagger)
*   **Database**: Room (Local caching for offline access)
*   **Networking**: Retrofit 2 + OkHttp 4
*   **Async**: Kotlin Coroutines & Flow
*   **Automation**: GitHub Actions (CI/CD)

## 🏗 Project Architecture
```
com.elysium.guild/
├── di/                  # Hilt modules for Network, Database, and Utils
├── ui/
│   ├── screens/         # BossTimers, Leaderboard, Events, Profile
│   ├── components/      # Centralized UI cards and widgets
│   └── theme/           # Adaptive Material 3 Color Schemes
├── viewmodel/           # State management for all major screens
├── models/              # Robust data models with API mapping
├── repository/          # Data layer with detailed error reporting
├── database/            # Room Database and DAO definitions
└── utils/               # Constants, UIUtils, and Notification management
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
4.  Create a tag: `git tag v1.0.10 && git push origin v1.0.10`.
5.  GitHub will build the APK and attach it to the release automatically.

---
**Built with ❤️ by the Elysium Development Team**
