# Elysium Guild Mobile App

A modern Android application for guild management that complements the Elysium Dashboard and Attendance Bot.

## Features

### 🎯 Boss Timers
- Real-time boss spawn timers
- Filter by status (Ready, Spawning Soon, Overdue)
- Mark bosses as killed directly from the app
- Visual status indicators
- Push notifications for upcoming spawns

### 🏆 Leaderboards
- Attendance rankings
- Points rankings
- Top 3 podium display
- Member profiles with avatars
- Real-time updates

### 📅 Guild Events
- Daily and weekly event schedules
- Event reminders
- Countdown timers
- Filter by event type

### 👤 Member Profile
- Personal statistics
- Points overview
- Attendance history
- Recent activity feed
- Guild role display

## Technology Stack

- **Kotlin** - Modern Android development
- **Jetpack Compose** - Modern UI toolkit
- **Hilt** - Dependency injection
- **Room** - Local database
- **Retrofit** - Network requests
- **Coroutines** - Asynchronous programming
- **Navigation Component** - Navigation
- **WorkManager** - Background tasks

## Setup Instructions

### Prerequisites

1. Android Studio Hedgehog or later
2. JDK 8 or higher
3. Android SDK API 24+ (Android 7.0)

### Configuration

1. Update the base URL in `Constants.kt`:
   ```kotlin
   const val BASE_URL = "https://your-elysium-dashboard-url.com/"
   ```

2. Configure Discord OAuth integration in your dashboard

3. Set up MongoDB connection for data synchronization

### Building the APK

1. Open the project in Android Studio
2. Build → Build Bundle(s)/APK(s) → Build APK(s)
3. APK will be generated in `app/build/outputs/apk/`

### Command Line Build

```bash
cd elysium-apk
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease       # Release APK
```

## Project Structure

```
app/
├── src/main/
│   ├── java/com/elysium/guild/
│   │   ├── ui/                  # UI components and screens
│   │   │   ├── screens/         # Main screens
│   │   │   ├── components/      # Reusable UI components
│   │   │   └── theme/           # Theme and styling
│   │   ├── viewmodels/          # ViewModels
│   │   ├── models/              # Data models
│   │   ├── network/             # API services
│   │   ├── repository/          # Data repositories
│   │   ├── database/            # Room database
│   │   ├── di/                  # Dependency injection
│   │   └── utils/              # Utilities
│   ├── res/
│   │   ├── layout/              # Compose layouts
│   │   ├── values/              # Resources (strings, colors, etc.)
│   │   └── drawable/           # Drawables
│   └── AndroidManifest.xml
├── build.gradle                 # App-level build configuration
└── proguard-rules.pro          # ProGuard configuration
```

## API Integration

The app integrates with your existing Elysium Dashboard API:

### Endpoints Used
- `GET /api/bosses` - Boss timer data
- `POST /api/bosses/{name}` - Mark boss as killed
- `GET /api/members` - Leaderboard data
- `GET /api/events` - Event schedules
- `GET /api/members/{id}` - Member profile

### Authentication
Uses Discord OAuth2 for secure member authentication.

## Data Synchronization

- **Real-time**: Live updates from the dashboard API
- **Offline**: Cached data using Room database
- **Background**: Automatic sync with WorkManager
- **Push Notifications**: Boss spawn alerts and event reminders

## Customization

### Theming
Update colors in `res/values/colors.xml`:
```xml
<color name="primary">#9333EA</color>
<color name="secondary">#0EA5E9</color>
```

### Boss Configuration
Boss data is fetched from your existing `boss_points.json` configuration.

### Events
Event schedules sync with your dashboard's event system.

## Security

- Network traffic uses HTTPS
- API authentication tokens secured
- Local data encrypted
- ProGuard obfuscation enabled for release builds

## Troubleshooting

### Build Issues
1. Ensure JDK 8+ is installed
2. Update Android SDK and build tools
3. Clean and rebuild project

### API Connection Issues
1. Verify `BASE_URL` in Constants.kt
2. Check network permissions
3. Test API endpoints manually

### Runtime Issues
1. Check logcat for errors
2. Verify API endpoints are accessible
3. Ensure proper authentication

## Contributing

1. Follow Android development best practices
2. Use Kotlin coding conventions
3. Write unit tests for new features
4. Update documentation

## License

This project is part of the Elysium Guild tools suite.

---

**Built with ❤️ for the Elysium Guild Community**