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

## Setup Instructions

### 1. Obtain a Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the **Maps SDK for Android** API: https://console.cloud.google.com/google/maps-apis/home
4. Create an API key at https://console.cloud.google.com/apis/credentials
    1. Restrict the key to Android apps and add your app's certificate fingerprint (TODO)
    1. Restrict to Maps SDK for Android
5. Copy the API key and paste it into the `.env` file:
   ```
   GOOGLE_MAP_API_KEY=your_actual_api_key_here
   ```
