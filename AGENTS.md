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
├── audio/           # Voice commands, TTS (AnnouncementManager, VoiceCommandListener)
├── ble/             # Bluetooth HRM device management (BleManager)
├── data/            # Data models (Run, RunStatistics)
├── database/        # Room entities, DAOs (RunEntity, RunDao)
├── location/        # GPS tracking (LocationTracker)
├── notifications/   # Run notifications (RunNotificationManager)
├── preferences/     # Device preferences (DevicePreferences)
├── repository/      # Data repository (RunRepository)
├── run/             # Run state management (RunStateManager, RunManager)
├── ui/
│   ├── components/  # Charts, map, UI components (HeartRateChart, PaceChart, RouteMap)
│   ├── screens/     # Run, History, Device Pairing screens
│   └── theme/       # Material 3 theme (Theme)
├── util/            # Calculation utilities (Geo, Pace, HeartRate)
├── RunTrackingService  # Foreground service for background tracking
└── MainActivity        # App entry point
```

Each file includes a short description comment at the top explaining its purpose.

## Codebase Discovery

Use the `extract_descriptions.sh` script to efficiently understand the entire codebase structure:

```bash
./extract_descriptions.sh
```

This displays source file descriptions in a token-efficient format (`path: description`). Each source file has a short description comment at the top (single-line `//` format). Use this when starting a new thread to quickly understand the codebase organization and locate relevant files.

When modifying a file's purpose or adding significant functionality, update its description comment at the top of the file to keep the codebase map accurate.

## Common Commands

| Command | Description |
|---------|-------------|
| `./gradlew build` | Full build with tests and lint |
| `./gradlew assembleDebug` | Debug APK |
| `./gradlew assembleRelease` | Release APK |
| `./gradlew test` | Run unit tests |
| `./gradlew connectedAndroidTest` | Run instrumentation tests on device/emulator |
| `./run_integration_tests.sh` | Run integration tests with emulator (automated setup) |
| `./gradlew lint` | Run Android lint check (fails on unused resources) |
| `./gradlew ktlintCheck` | Check code formatting |
| `./gradlew ktlintFormat` | Auto-fix formatting |
| `./gradlew jacocoTestReport` | Generate code coverage report |
| `shellcheck script.sh` | Validate bash script |

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
- **Google Maps Compose** - Maps SDK for Android (v4.3.3) with Jetpack Compose support

## Code Patterns

### Code Style Guidelines
- Each source file under app/src/main/java/com/fghbuild/sidekick/ must have a short description comment at the top (`// purpose description`) explaining its purpose. Update this comment if the file's functionality changes significantly.
- **Default Arguments**: Do not use default arguments in functions and constructors, except for data classes where they are idiomatic Kotlin usage and for Compose Modifier parameters where `modifier: Modifier = Modifier` is the standard pattern. All function parameters should be explicitly provided at call sites, with the exception of Compose Modifier parameters.
- **Never hardcode `Locale.US`** - Always use `Locale.getDefault()` to respect user's device locale
- Pass context to access LocalConfiguration for Compose when needed

### Token Efficiency
- Be token-efficient by default: remove decorative elements, minimize whitespace, use compact formatting
- Group related information to avoid repeating prefixes or common paths

### State Management
- Use `StateFlow` for observable state
- Collect flows in Compose with `collectAsState()`

### Testing
- Unit tests in `src/test/` use JUnit 6 (Jupiter) with annotations from `org.junit.jupiter.api`
- Instrumentation tests in `src/androidTest/` use JUnit 4 with Compose test rule for device/emulator testing
- Compose UI tests use `@get:Rule val composeTestRule = createComposeRule()`
- Unit test fixtures: `@BeforeEach`, `@AfterEach`, `@DisplayName` from `org.junit.jupiter.api`
- Integration test fixtures: `@Before`, `@After`, `@Test` from `org.junit` (JUnit 4)
