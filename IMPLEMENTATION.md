# Implementation Strategy for Sidekick Android App

## Overview
Sidekick is an Android application designed to assist during running training sessions. See AGENTS.md for the technology stack and build commands.

## Architecture
- **MVVM Pattern**: Model-View-ViewModel for clean architecture
- **Services**: Foreground services for continuous monitoring during runs
- **Permissions**: Location, Bluetooth, Microphone, Notifications
- **Data Storage**: Local database for run history and settings

## Implementation Phases

### Phase 1: Core Setup and UI Foundation
1. Set up Android project with Kotlin and Jetpack Compose
2. Implement main activity and navigation structure
3. Create basic UI screens (Home, Run in Progress, History)
4. Set up Gradle build configuration for CLI compilation

### Phase 2: GPS and Location Tracking
1. Implement location permissions and GPS tracking
2. Calculate distance and pace in real-time
3. Store route data (latitude/longitude points)
4. Display current pace and distance on screen

### Phase 3: Heart Rate Monitoring
1. Implement BLE device scanning and connection
2. Parse heart rate data from HRM devices
3. Remember last connected device
4. Display current and average heart rate

### Phase 4: Voice Commands and Audio Feedback
1. Implement speech recognition for "pause" and "resume"
2. Add text-to-speech for pace/distance announcements (every 1km)
3. Add heart rate announcements (every minute)
4. Handle microphone permissions

### Phase 5: Visual Features
1. Implement heart rate graph using charting library
2. Implement pace graph
3. Add zoomable map display for route
4. Create notification with live pace/distance and lock screen display

### Phase 6: Run Management
1. Implement run start/pause/resume/stop logic
2. Add automatic home detection to stop run
3. Store completed runs in database
4. Calculate and display improvements over time

### Phase 7: Testing and Refinement
1. Unit tests for calculations and data processing
2. Integration tests for BLE and GPS
3. UI testing with Compose testing framework
4. Performance optimization for battery usage


