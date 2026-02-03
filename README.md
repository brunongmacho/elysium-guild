# Elysium Guild Mobile App

A modern, high-performance Android application built with Jetpack Compose, designed for elite guild management. This app integrates directly with the Elysium Dashboard API to provide real-time tracking of world bosses, guild events, and member performance.

## 🚀 Version 1.0.11 (Current)
*   **Centered UI Architecture**: All page headers and subtitles are now perfectly centered across the entire app for a balanced, premium aesthetic.
*   **GCASH Donation Integration**: A new "Guild Support" feature in Settings that instantly opens a full-height QR display for easy GCASH scanning.
*   **Page Subtitles**: Every major screen now features descriptive subtitles (e.g., "Stay synced with guild activities") to improve UX clarity.
*   **Deep Logic Centralization**: Page titles, subtitles, resource names, and UI behavioral logic have been moved into `Constants.kt`, making the project extremely easy to maintain and scale.
*   **Performance Synchronization**: Boss and Event timers now share the exact same optimized logic and smooth color transitions, ensuring silky smooth scrolling.
*   **Light Mode Refinement**: Introduced a vibrant **Azure Blue** for tracking stages in Light Mode, paired with high-contrast adaptive status badges.

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
4.  Create a tag: `git tag v1.0.11 && git push origin v1.0.11`.
5.  GitHub will build the APK and attach it to the release automatically.

---
**Built with ❤️ by the Elysium Development Team**
