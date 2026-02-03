# Elysium Guild Mobile App

A modern, high-performance Android application built with Jetpack Compose, designed for elite guild management. This app integrates directly with the Elysium Dashboard API to provide real-time tracking of world bosses, guild events, and member performance.

## 🚀 Version 1.0.12 (Latest)
*   **Premium Glassy UI Design**: Every interactive element (filter chips, toggles, buttons) has been redesigned with a premium glassy texture, including translucent backgrounds and custom linear gradient borders for a "frosted glass" aesthetic.
*   **ELYSIUM Rotation Highlighting**: World Boss rotations for our guild **ELYSIUM** now feature a visual popout with a unique guild glow and an explicit "(OUR TURN)" status indicator for instant recognition.
*   **Glassy Top-Notifications**: Replaced standard system toasts with theme-aware, glassy notification cards that slide in from the top, providing a more integrated and consistent feedback experience.
*   **UI Layout Optimization**: Refined badge and chip layouts to prevent text wrapping on smaller devices, ensuring all numbers and labels remain on a single, clean line.
*   **Centralized Settings Architecture**: Decoupled settings components from screen logic, improving performance, modularity, and maintainability across the app.

## 🎯 Core Features

### 1. Boss Timers (Real-Time)
*   **Live Countdown**: Track precise spawn times for all world bosses.
*   **Smart Filtering**: Categorize bosses by `Ready`, `Soon` (within 30m), and `Tracking`.
*   **Haptic Alerts**: Tactile feedback on status changes and pull-to-refresh actions.
*   **Visual Progress**: Adaptive status colors and progress bars optimized for both Light and Dark themes.
*   **Call to Arms**: Quick-share boss status to Discord/Clipboard with one tap.

### 2. Guild Events
*   **Dynamic Schedule**: Syncs with the dashboard to show daily/weekly guild activities.
*   **Synced Visuals**: Event cards share the same premium glassy style and stage logic as Boss cards.
*   **Precise Reminders**: Local exact alarms notify you exactly when the action starts.

### 3. Leaderboards & Profiles
*   **Competitive Rankings**: Track top performers in `Attendance` and `Points`.
*   **Adaptive Podium**: High-contrast podium display that works perfectly in all lighting conditions.
*   **Detailed Statistics**: View personal participation history and point accrual.

### 4. Advanced Settings
*   **Theme Engine**: Support for `Light`, `Dark`, and `System` modes with instant UI switching.
*   **Sound Selection**: Choose your preferred alert sound with live previews.
*   **Guild Support**: Instant access to the guild's donation QR code for maintenance support.

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
4.  Create a tag: `git tag v1.0.12 && git push origin v1.0.12`.
5.  GitHub will build the APK and attach it to the release automatically.

---
**Built with ❤️ by the Elysium Development Team**
