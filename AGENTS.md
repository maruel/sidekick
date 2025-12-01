# Agent development guide

A file to [guide coding agents](https://agents.md/).

## Requirements

- Make sure the code compiles: Run `./gradlew build` before committing to verify compilation.
- Format the code: Run `./gradlew ktlintFormat` and `./gradlew ktlintCheck` to ensure proper formatting.
- Update this file (AGENTS.md) everytime you make a change that affects the agent. This may include adding new
  entries, sections or removing old ones.

## Build & Compilation

- **Gradle Wrapper**: Project uses `./gradlew` for all builds
- **Compile**: `./gradlew build`
- **Debug APK**: `./gradlew assembleDebug`
- **Release APK**: `./gradlew assembleRelease`
- **Tests**: `./gradlew test`
- **Format Check**: `./gradlew ktlintCheck` (fails if unformatted files detected)
- **Format Fix**: `./gradlew ktlintFormat` (automatically formats code)

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Min SDK**: 34
- **Target SDK**: 36
- **JVM Version**: 21
- **Build System**: Gradle 9.2.1

## Key Dependencies

- Jetpack Compose (Material3, Navigation)
- Google Play Services (Location, Maps, Nearby/BLE)
- MPAndroidChart (charting)
- Room (local database)
- Android TextToSpeech and SpeechRecognizer APIs
