# Elysium Guild Mobile App

A modern, high-performance Android application built with Jetpack Compose, designed for elite guild management. This app integrates directly with the Elysium Dashboard API to provide real-time tracking of world bosses, guild events, and member performance.

## 🚀 Version 1.0.8 (Current)
*   **Auto-Update Engine**: Fully integrated with GitHub Releases. The app automatically detects, downloads, and installs updates from the `brunongmacho/elysium-guild` repository.
*   **Permanent Signing**: Uses a stable cryptographic signature (`debug.keystore`) to ensure seamless updates without "conflict" errors.
*   **System Permissions Integration**: A consolidated dashboard in Settings for managing Notifications, Exact Alarms, Battery Optimization, and APK Installation permissions.
*   **Performance Optimization**: Purged all legacy widget code and Glance dependencies to reduce APK size and memory footprint.

## 🎯 Core Features

### 1. Boss Timers (Real-Time)
*   **Live Countdown**: Track precise spawn times for all world bosses.
*   **Smart Filtering**: Categorize bosses by `Ready`, `Soon` (within 30m), and `Tracking`.
*   **Haptic Alerts**: Tactile feedback on status changes and pull-to-refresh actions.
*   **Visual Progress**: Gradient-coded status rings and progress bars for "Spawning Soon" states.
*   **Call to Arms**: Quick-share boss status to Discord/Clipboard with one tap.

### 2. Guild Events
*   **Dynamic Schedule**: Syncs with the dashboard to show daily/weekly guild activities.
*   **Precise Reminders**: Local exact alarms notify you 10 minutes before an event starts.
*   **Filterable Feed**: Toggle between different event types to stay focused.

### 3. Leaderboards & Profiles
*   **Competitive Rankings**: Track top performers in `Attendance` and `Points`.
*   **Podium Display**: Special UI treatment for the Top 3 members.
*   **Detailed Statistics**: View personal participation history and point accrual.

### 4. Advanced Settings
*   **Theme Engine**: Support for `Light`, `Dark`, and `System` modes.
*   **Permission Auditor**: Real-time status of required system permissions with deep links to fix them instantly.
*   **Software Updates**: One-tap version checking with release notes display.

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
│   ├── components/      # Glassmorphic UI, BossCards, Profile widgets
│   └── theme/           # Material 3 Color Schemes and Typography
├── viewmodel/           # State management for all major screens
├── models/              # POJO and Room Entity definitions
├── network/             # Retrofit interfaces for Dashboard and Updates
├── repository/          # Single source of truth for data flow
├── database/            # Room Database and DAO definitions
└── utils/               # UpdateManager, NotificationHelper, Constants
```

## 📦 Setup & Deployment

### For Developers
1.  Open in **Android Studio Ladybug** (or later).
2.  Ensure **JDK 17** is configured.
3.  Set the `BASE_URL` in `Constants.kt` to point to your Koyeb/Backend instance.

### For Releasing Updates
The app uses a fully automated **GitHub Actions** pipeline:
1.  Update `versionCode` in `app/build.gradle`.
2.  Update `update-manifest.json` in the root folder.
3.  Push changes: `git push origin main`.
4.  Create a tag: `git tag v1.0.8 && git push origin v1.0.8`.
5.  GitHub will build the APK and attach it to the release automatically.

---
**Built with ❤️ by the Elysium Development Team**
