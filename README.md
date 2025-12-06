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

# Format code
./gradlew ktlintFormat
```

## Testing

```bash
# Run unit tests
./gradlew test

# Run unit tests with code coverage
./gradlew jacocoTestReport
# HTML report: app/build/reports/jacoco/jacocoTestReport/html/index.html
# XML report: app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml

# Run with Docker (no setup needed)
docker build -f Dockerfile.tests -t sidekick-tests . && docker run sidekick-tests

# Run specific test suite
./gradlew androidTest --tests "*DatabaseTest*"
./gradlew androidTest --tests "*Manager*"
./gradlew androidTest --tests "*Screen*"
```

Integration tests:

```bash
docker build -f Dockerfile.tests -t sidekick-tests .

docker run sidekick-tests
```

Bash script for running tests locally with xvfb:

```bash
./run_integration_tests.sh
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
