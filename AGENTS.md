# Agent Development Guide

Instructions for coding agents working on this project.

## Project Overview

**Sidekick** is an Android running companion app with GPS tracking, heart rate monitoring, voice commands, and run history persistence.

### Architecture

- **MVVM Pattern** with Jetpack Compose UI
- **Room Database** for run persistence
- **StateFlow** for reactive state management
- **Foreground Service** for background tracking

### Key Components

- `LocationTracker` - GPS updates via LocationManager
- `BleManager` - BLE scanning, HR data parsing, device pairing
- `RunManager` - Distance calculation (Haversine), pace tracking
- `VoiceCommandListener` - Speech recognition for pause/resume
- `AnnouncementManager` - Text-to-speech feedback
- `HeartRateUtils` - HR statistics and personalized zones (5 zones based on age)
- Charts: `HeartRateChart`, `PaceChart`, `RouteMap`

### Project Structure

```
app/src/main/java/com/fghbuild/sidekick/
├── audio/           # Voice commands, TTS
├── ble/             # Bluetooth HRM device management
├── data/            # Data models
├── database/        # Room entities, DAOs
├── location/        # GPS tracking
├── notifications/   # Run notifications
├── preferences/     # Device preferences
├── repository/      # Data repository
├── run/             # Run state management
├── ui/
│   ├── components/  # Charts, map, UI components
│   ├── screens/     # Run, History, Device Pairing screens
│   └── theme/       # Material 3 theme
└── util/            # Calculation utilities (Geo, Pace, HeartRate)
```

## Build Commands

| Command | Description |
|---------|-------------|
| `./gradlew build` | Full build with tests and lint |
| `./gradlew assembleDebug` | Debug APK |
| `./gradlew assembleRelease` | Release APK |
| `./gradlew test` | Run unit tests |
| `./gradlew connectedAndroidTest` | Run instrumentation tests on device/emulator |
| `./run_integration_tests.sh` | Run integration tests with emulator (automated setup) |
| `./gradlew ktlintCheck` | Check code formatting |
| `./gradlew ktlintFormat` | Auto-fix formatting |
| `./gradlew jacocoTestReport` | Generate code coverage report |

## Requirements

- Run `./gradlew build` before committing to verify compilation
- Run `./gradlew ktlintFormat` to fix formatting issues
- Update this file when adding new patterns or dependencies

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Database | Room |
| Min SDK | 34 (Android 14) |
| Target SDK | 36 |
| Unit Tests | JUnit 6 (Jupiter) |
| Integration Tests | JUnit 6 with Compose UI Testing |

## Key Dependencies

- **Compose** - Material3, Navigation Suite
- **Room** - Local database for run history
- **Android APIs** - LocationManager, BluetoothLE, TextToSpeech, SpeechRecognizer

## Code Patterns

### State Management
- Use `StateFlow` for observable state
- Collect flows in Compose with `collectAsState()`

### Utilities
- `GeoUtils` - Distance calculations (Haversine)
- `PaceUtils` - Pace calculation and formatting
- `HeartRateUtils` - HR statistics

### Testing
- Unit tests in `src/test/` use JUnit 6 (Jupiter) with annotations from `org.junit.jupiter.api`
- Instrumentation tests in `src/androidTest/` use JUnit 4 with Compose test rule for device/emulator testing
- Compose UI tests use `@get:Rule val composeTestRule = createComposeRule()`
- Unit test fixtures: `@BeforeEach`, `@AfterEach`, `@DisplayName` from `org.junit.jupiter.api`
- Integration test fixtures: `@Before`, `@After`, `@Test` from `org.junit` (JUnit 4)

### Localization
- **Never hardcode `Locale.US`** - Always use `Locale.getDefault()` to respect user's device locale
- Pass context to access LocalConfiguration for Compose when needed
