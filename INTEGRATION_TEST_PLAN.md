# Integration Testing Plan for Sidekick

## Overview

This plan outlines a strategy for adding comprehensive integration tests to the Sidekick running app. Integration tests will verify that multiple components work together correctly in realistic scenarios, bridging the gap between unit tests and full end-to-end testing.

## Current State

**Existing Tests:**
- Unit tests: `src/test/` - Utilities, data models, RunManager (~10 tests)
- UI tests: `src/androidTest/` - Basic Compose screen tests, isolated components (~9 tests)
- No integration tests: No tests combining database, managers, and UI

**Test Infrastructure:**
- JUnit 6 with Compose testing rules (upgraded Dec 2025)
- No test doubles (mocks) for managers
- No database integration tests

## Goals

1. **Verify core user workflows** - Full run lifecycle from start to save
2. **Test data persistence** - Location tracking, HR data, and run history storage
3. **Validate cross-layer communication** - ViewModel ↔ Managers ↔ Database
4. **Enable UI testing with real data** - Replace all mock data with actual app data
5. **Support headless CI/CD** - Run in Docker with xvfb for visual testing

## Architecture

### Test Environment Setup

```
Integration Tests (androidTest/)
├── fixtures/              # Test data factories, fake managers
├── scenarios/             # User workflow tests
├── database/              # Room + Repository tests
└── ui/                    # E2E UI tests with real state
```

### Test Layers

1. **Database Layer** - Room DAO operations
   - Verify RunEntity/RoutePointEntity schema
   - Test CRUD operations
   - Validate queries and relationships

2. **Repository Layer** - Data access abstraction
   - Test save run with route points
   - Test load/delete operations
   - Verify data transformation

3. **Manager Layer** - Business logic coordination
   - RunManager with real location data
   - BleManager with mock BLE devices
   - Announcement coordination

4. **ViewModel + UI Layer** - State management and presentation
   - UI state reflects data changes
   - User interactions trigger correct flows
   - Charts/graphs render with real data

5. **End-to-End Workflows** - Complete user journeys
   - Start run → track location → record HR → pause → resume → stop → save
   - Load run history → view run details → delete run
   - Multiple consecutive runs

## Implementation Phases

### Phase 1: Test Infrastructure (2-3 hours)

**Setup xvfb + Emulator:**
```bash
# Script: run_integration_tests.sh
Xvfb :99 -screen 0 1024x768x24 &
export DISPLAY=:99
emulator -avd <device_name> -no-window &
./gradlew connectedAndroidTest
```

**Add test dependencies:**
- Espresso for UI testing
- Room testing library
- MockK or Mockito for manager mocks
- Hilt for dependency injection in tests

**Create test utilities:**
- `TestDatabase` - In-memory Room database
- `FakeLocationTracker` - Simulates GPS updates
- `FakeBleManager` - Simulates HRM device
- `TestDataFactory` - Creates test entities
- `TestRule` classes for setup/teardown

### Phase 2: Database & Repository Tests (4-6 hours)

**Test files:**
- `RunRepositoryTest.kt` - Save/load/delete runs
- `RoutePointDaoTest.kt` - Route point persistence
- `RunDatabaseTest.kt` - Schema and migrations

**Test scenarios:**
1. Save single run with empty route
2. Save run with 100+ route points
3. Load all runs (ordered by date)
4. Load single run with all route points
5. Delete run and verify cascade delete
6. Query runs by date range
7. Calculate run statistics (distance, duration)
8. Verify foreign key constraints

### Phase 3: Manager Integration Tests (6-8 hours)

**Test files:**
- `RunManagerIntegrationTest.kt` - Location tracking + distance
- `RunStateManagerIntegrationTest.kt` - State transitions + announcements
- `BleManagerFakeTest.kt` - HR data with fake device

**Test scenarios:**
1. **RunManager:**
   - Start tracking, receive 10 location updates
   - Verify distance calculations (Haversine)
   - Verify pace calculations
   - Pause/resume tracking
   - Stop and finalize run

2. **RunStateManager:**
   - Trigger announcements at correct distance intervals (1km)
   - Trigger HR announcements at correct intervals (1min)
   - Verify TextToSpeech calls for announcements
   - Verify voice command recognition (pause/resume)

3. **BleManager (with fake):**
   - Scan for devices
   - Connect to fake device
   - Parse HR characteristic values
   - Receive HR updates every second

### Phase 4: ViewModel & UI Integration Tests (8-10 hours)

**Test files:**
- `RunScreenViewModelTest.kt` - State transitions
- `HistoryScreenViewModelTest.kt` - Run list management
- `RunFlowScreenTest.kt` - Start → In Progress → Save flow

**Test scenarios:**
1. **Home → Run In Progress Flow:**
   - Click "Start Run" → RunInProgressScreen appears
   - Manager starts sending location updates
   - UI updates with distance/pace/HR
   - Charts update with real data

2. **Run In Progress → Complete Flow:**
   - Click "Pause" → state changes
   - Pause UI shows resume button
   - Click "Resume" → tracking resumes
   - Click "Stop" → save dialog appears
   - Enter run name → save to database
   - Navigate to History → new run appears in list

3. **History Management:**
   - Load history with 5+ runs from database
   - Display runs with stats
   - Click run → details screen
   - Charts render with route and HR data
   - Delete run → removed from list and database

### Phase 5: End-to-End Scenario Tests (6-8 hours)

**Test files:**
- `FullRunWorkflowTest.kt` - Complete run lifecycle
- `MultipleRunsTest.kt` - Multiple consecutive runs
- `HistoryDetailTest.kt` - View saved run details

**Test scenarios:**
1. **Full Run (15-20 seconds simulated):**
   - Onboarding → Home screen
   - Start run (LocationTracker begins)
   - Receive 10 location updates (1km+ total)
   - Connect fake BLE device → receive HR data
   - Trigger announcement (distance/HR)
   - Pause → resume → stop
   - Save with name "Morning Run"
   - Verify in history with correct stats

2. **Multiple Runs:**
   - Run 1: 2km in 15 minutes
   - Run 2: 1km in 8 minutes
   - History shows both, newest first
   - Stats aggregated correctly

3. **View Run Details:**
   - Click saved run from history
   - Verify route rendered on map
   - Verify HR chart shows data points
   - Verify pace chart shows segments
   - Delete run → removed from history

## Test Data Strategy

### Real vs. Fake Components

| Component | Strategy | Reason |
|-----------|----------|--------|
| Room Database | Real (in-memory) | Test actual persistence |
| LocationTracker | Fake (feed test data) | No GPS in emulator |
| BleManager | Fake (mock device) | No BLE in emulator |
| ViewModels | Real | Test state management |
| Compose UI | Real | Test rendering |
| TextToSpeech | Mock (no actual audio) | Can't verify audio output |
| SpeechRecognizer | Mock (inject voice commands) | Simulate user voice |

### Location Data Factory

```kotlin
fun createTestRoute(distanceKm: Double): List<RoutePoint> {
    // Create path with known distance
    // (simulates actual GPS tracking)
}

fun createHeartRateData(count: Int): List<HeartRateEvent> {
    // Create HR values over time
}
```

## Tools & Dependencies

### Testing Framework: JUnit 6 ✅

**Status:** JUnit 6 is now the standard for this project (upgraded Dec 2025).

**Why JUnit 6:**
- **Java 17+ baseline** - Sidekick targets Android 14+ (SDK 34), which supports Java 17
- **Kotlin `suspend` support** - Can use async test methods natively
- **JSpecify annotations** - Better null safety in tests
- **Unified versioning** - Platform, Jupiter, and Vintage all use version 6.0.0
- **Better parameterized tests** - `@ParameterizedTest` with cleaner syntax
- **DisplayName support** - More readable test output

**Note:** We've removed all traces of JUnit 4. Previous test infrastructure using `@Rule`, `@Before`, `@After`, and `org.junit.Assert` has been migrated to JUnit 6 patterns.

### Gradle Dependencies (JUnit 6)

**Core testing dependencies (already added to `gradle/libs.versions.toml`):**

```kotlin
// Testing - JUnit 6
testImplementation(platform(libs.junit.bom))           // 6.0.0
testImplementation(libs.junit.api)                     // 6.0.0
testImplementation(libs.junit.params)                  // 6.0.0
testRuntimeOnly(libs.junit.engine)                     // 6.0.0
testImplementation(libs.kotlin.test)                   // 2.0.21
testImplementation(libs.mockk)                         // 1.13.7
testImplementation(libs.coroutines.test)               // 1.7.3

androidTestImplementation(platform(libs.junit.bom))    // 6.0.0
androidTestImplementation(libs.junit.api)              // 6.0.0
androidTestImplementation(libs.kotlin.test)            // 2.0.21
androidTestImplementation(libs.androidx.junit)         // 1.2.0
androidTestImplementation(libs.androidx.espresso.core) // 3.5.1
androidTestImplementation(libs.androidx.room.testing)  // 2.6.0
androidTestImplementation(libs.mockk.android)          // 1.13.7
androidTestImplementation(libs.coroutines.test)        // 1.7.3
```

### JUnit 6 Test Pattern

```kotlin
@DisplayName("Run saving workflow")
class RunSavingTest {
    @Test
    @DisplayName("should save run with route points to database")
    fun saveRunWithRoute() {
        // Assertions use kotlin.test (not org.junit.Assert)
        assertEquals(expected, actual)
        assertTrue(condition)
    }
    
    @ParameterizedTest
    @ValueSource(doubles = [1.0, 5.0, 10.0, 20.0])
    @DisplayName("should calculate pace for various distances")
    fun calculatePace(distanceKm: Double) {
        // Parameterized tests are cleaner in JUnit 6
        val pace = PaceUtils.calculatePaceMinPerKm(distanceKm * 1000, 60 * 1000)
        assertTrue(pace > 0)
    }
    
    @BeforeEach  // Not @Before (JUnit 4 syntax)
    fun setup() {
        // Test setup
    }
}
```

### xvfb Setup Script

Create `run_integration_tests.sh`:

```bash
#!/bin/bash
set -e

# Start virtual X server
export DISPLAY=:99
Xvfb :99 -screen 0 1024x768x24 &
XVFB_PID=$!
sleep 2

# Create/boot emulator
emulator -avd Pixel_6_API_34 -no-window &
EMULATOR_PID=$!
sleep 30

# Wait for device
adb wait-for-device

# Run integration tests
./gradlew connectedAndroidTest

# Cleanup
kill $EMULATOR_PID $XVFB_PID
```

## Success Criteria

- [ ] All test infrastructure scripts working in Docker
- [ ] 100% of Manager classes tested with realistic data
- [ ] Database operations verified with Room testing library
- [ ] Full run workflow tested end-to-end
- [ ] UI renders correctly with real data
- [ ] Tests run in CI/CD pipeline with xvfb
- [ ] 80%+ code coverage for core modules
- [ ] All tests pass in <5 minutes
- [ ] Screenshots captured for failed tests

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Emulator slow in Docker | Use minimal system image, xvfb for headless |
| Flaky location tests | Use deterministic test routes, fixed seed |
| BLE simulation complex | Start with fake manager, mock at low level |
| Long test runtime | Parallel test execution, unit tests for fast feedback |
| Device-specific issues | Test on multiple API levels (34, 35, 36) |

## Next Steps

1. Review and approve plan
2. Set up xvfb test environment
3. Add test dependencies to gradle
4. Create test utilities and fixtures
5. Implement Phase 2 (database tests)
6. Progressively implement remaining phases
7. Integrate into CI/CD pipeline

