# Sidekick

An Android running companion app that tracks your runs with GPS, heart rate monitoring, and voice feedback.

## Features

- **GPS Tracking** - Real-time distance and pace calculation with route mapping
- **Heart Rate Monitoring** - Connect to BLE heart rate monitors, view live and average HR
- **Voice Commands** - Say "pause" or "resume" to control your run hands-free
- **Audio Feedback** - Announces pace/distance every 1km, heart rate every minute
- **Run History** - Stores completed runs with stats and route data
- **Live Notifications** - Shows current pace and distance on lock screen

## Screenshots

*Coming soon*

## Requirements

- Android 14+ (SDK 34)
- GPS-enabled device
- Bluetooth LE for heart rate monitoring (optional)

## Build

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Format code
./gradlew ktlintFormat
```

## Architecture

The app uses MVVM architecture with Jetpack Compose for UI:

- `ui/screens/` - Compose screens (Home, Run, History)
- `ui/components/` - Reusable UI components (charts, map)
- `run/` - Run state management
- `ble/` - Bluetooth heart rate monitor integration
- `location/` - GPS tracking
- `audio/` - Voice commands and text-to-speech
- `database/` - Room persistence for run history
- `repository/` - Data layer
- `util/` - Calculation utilities
