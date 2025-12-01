# Agent Development Guide

Instructions for coding agents working on this project.

## Build Commands

| Command | Description |
|---------|-------------|
| `./gradlew build` | Full build with tests and lint |
| `./gradlew assembleDebug` | Debug APK |
| `./gradlew assembleRelease` | Release APK |
| `./gradlew test` | Run unit tests |
| `./gradlew ktlintCheck` | Check code formatting |
| `./gradlew ktlintFormat` | Auto-fix formatting |

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
- Unit tests in `src/test/` for utilities and managers
- UI tests in `src/androidTest/` for Compose screens
