# Implementation Status

## Overview

Sidekick is an Android running companion app. See [AGENTS.md](AGENTS.md) for build commands and technology stack.

## Architecture

- **MVVM Pattern** with Jetpack Compose UI
- **Room Database** for run persistence
- **StateFlow** for reactive state management
- **Foreground Service** for background tracking

## Completed Phases

### Phase 1: Core Setup ✓
- Android project with Kotlin and Jetpack Compose
- Navigation with `NavigationSuiteScaffold` (Home, Run, History tabs)
- Material 3 theming

### Phase 2: GPS Tracking ✓
- `LocationTracker` - GPS updates via LocationManager
- `RunManager` - Distance calculation (Haversine formula), pace tracking
- `RoutePoint` data model for storing coordinates

### Phase 3: Heart Rate Monitoring ✓
- `BleManager` - BLE scanning, connection, HR data parsing
- Standard HRM service UUID support
- `DevicePreferences` - Remembers last connected device

### Phase 4: Voice & Audio ✓
- `VoiceCommandListener` - Speech recognition for "pause"/"resume"
- `AnnouncementManager` - Text-to-speech feedback
- `RunStateManager` - Coordinates announcements (1km distance, 1min HR)

### Phase 5: Visual Features ✓
- `HeartRateChart` - HR graph over time
- `PaceChart` - Pace history visualization
- `RouteMap` - Canvas-based route display
- `RunNotificationManager` - Live notifications

### Phase 6: Persistence ✓
- Room database with `RunEntity` and `RoutePointEntity`
- `RunRepository` - Save/load/delete runs
- History screen with run cards and stats

### Phase 7: Testing ✓
- Unit tests for utilities (GeoUtils, PaceUtils, HeartRateUtils)
- Unit tests for RunManager and data models
- Compose UI tests for all screens

### Phase 8: User Onboarding & Heart Rate Zones ✓
- `OnboardingScreen` - Birth year input on first launch
- `DevicePreferences` - Stores user profile data
- `HeartRateUtils` - Calculates personalized heart rate zones (5 zones based on age)
- Extensible onboarding: new questions auto-require before showing app

## Project Structure

```
app/src/main/java/com/fghbuild/sidekick/
├── audio/           # Voice commands, TTS
├── ble/             # Bluetooth HRM
├── data/            # Data models
├── database/        # Room entities, DAOs
├── location/        # GPS tracking
├── notifications/   # Run notifications
├── preferences/     # Device preferences
├── repository/      # Data repository
├── run/             # Run state management
├── ui/
│   ├── components/  # Charts, map
│   ├── screens/     # Home, Run, History
│   └── theme/       # Material theme
└── util/            # Calculation utilities
```
