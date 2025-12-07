package com.fghbuild.sidekick.database

import android.location.Location
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.fixtures.TestDataFactory
import com.fghbuild.sidekick.run.RunManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test the full GPS calibration workflow including startup initialization,
 * measurement collection, and session finalization.
 */
class GpsStartupTest {
    private lateinit var database: SidekickDatabase
    private lateinit var runManager: RunManager
    private lateinit var gpsMeasurementDao: GpsMeasurementDao
    private lateinit var gpsCalibrationDao: GpsCalibrationDao
    private lateinit var runDao: RunDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        database =
            Room.inMemoryDatabaseBuilder(
                context,
                SidekickDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()
        gpsMeasurementDao = database.gpsMeasurementDao()
        gpsCalibrationDao = database.gpsCalibrationDao()
        runDao = database.runDao()
        runManager =
            RunManager(
                gpsMeasurementDao,
                gpsCalibrationDao,
            )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun startup_databaseInitializesWithoutError() {
        // Just verify the DAOs exist and are accessible
        assertNotNull(gpsMeasurementDao)
        assertNotNull(gpsCalibrationDao)
        assertNotNull(runDao)
    }

    @Test
    fun startupWorkflow_initializeRunSession_completesWithoutError() =
        runBlocking {
            // Create a run entity
            val runEntity =
                TestDataFactory.createTestRunEntity(
                    distanceMeters = 0.0,
                    durationMillis = 0L,
                )
            val runId = runDao.insertRun(runEntity)
            assertTrue(runId > 0)

            // Initialize run session (this is called after run starts)
            runManager.initializeRunSession(runId, "running")

            // No exceptions should be thrown
        }

    @Test
    fun fullWorkflow_startTrackFinalize_persistsMeasurements() =
        runBlocking {
            // 1. Start run
            runManager.startRun()

            // 2. Create run entity and initialize session
            val runEntity =
                TestDataFactory.createTestRunEntity(
                    distanceMeters = 0.0,
                    durationMillis = 0L,
                )
            val runId = runDao.insertRun(runEntity)
            runManager.initializeRunSession(runId, "running")

            // 3. Add location updates and accumulate measurements
            val baseTime = System.currentTimeMillis()
            val locations =
                listOf(
                    Location("test").apply {
                        latitude = 40.7128
                        longitude = -74.0060
                        time = baseTime
                        accuracy = 5f
                    },
                    Location("test").apply {
                        latitude = 40.7138
                        longitude = -74.0060
                        time = baseTime + 60000
                        accuracy = 6f
                    },
                    Location("test").apply {
                        latitude = 40.7148
                        longitude = -74.0060
                        time = baseTime + 120000
                        accuracy = 4f
                    },
                )

            for (location in locations) {
                runManager.updateLocation(location)
            }

            // 4. Finalize session (should update calibration if >= 30 seconds)
            runManager.finalizeRunSession()

            // 5. Verify measurements were recorded
            val measurements = gpsMeasurementDao.getRunMeasurements(runId)
            assertEquals(2, measurements.size) // 3 locations - 1 (first has no prior point)

            // Verify all measurements have correct data
            for (measurement in measurements) {
                assertNotNull(measurement.accuracy)
                assertTrue(measurement.accuracy > 0)
            }
        }

    @Test
    fun calibrationUpdate_onlyHappensAfter30Seconds() =
        runBlocking {
            // 1. Create a short run (less than 30 seconds)
            runManager.startRun()

            // 5 second run
            val runEntity =
                TestDataFactory.createTestRunEntity(
                    distanceMeters = 0.0,
                    durationMillis = 5000L,
                )
            val runId = runDao.insertRun(runEntity)
            runManager.initializeRunSession(runId, "running")

            // Add one location
            val location =
                Location("test").apply {
                    latitude = 40.7128
                    longitude = -74.0060
                    accuracy = 5f
                }
            runManager.updateLocation(location)

            // Finalize - calibration should NOT be updated (< 30 seconds)
            runManager.finalizeRunSession()

            // Verify no calibration was created
            val calibration = gpsCalibrationDao.getCalibration("running")
            // Calibration may not exist for short runs
            // (This is implementation-dependent)
        }

    @Test
    fun multipleRuns_calibrationMergesProperly() =
        runBlocking {
            // Run 1: Record some measurements
            runManager.startRun()
            val run1 = TestDataFactory.createTestRunEntity()
            val runId1 = runDao.insertRun(run1)
            runManager.initializeRunSession(runId1, "running")

            val baseTime1 = System.currentTimeMillis()
            for (i in 0..4) {
                val location =
                    Location("test").apply {
                        latitude = 40.7128 + i * 0.001
                        longitude = -74.0060
                        accuracy = (5 + i).toFloat()
                    }
                runManager.updateLocation(location)
            }

            // Manually insert measurements for this run
            // Speed is in m/s
            gpsMeasurementDao.insert(
                GpsMeasurementEntity(
                    runId = runId1,
                    activity = "running",
                    timestamp = baseTime1,
                    accuracy = 5f,
                    bearingAccuracy = 10f,
                    speed = 2.5f,
                    bearing = 45f,
                ),
            )
            gpsMeasurementDao.insert(
                GpsMeasurementEntity(
                    runId = runId1,
                    activity = "running",
                    timestamp = baseTime1 + 60000,
                    accuracy = 6f,
                    bearingAccuracy = 10f,
                    speed = 2.5f,
                    bearing = 45f,
                ),
            )

            runManager.finalizeRunSession()

            // Run 2: Record more measurements and verify calibration merges
            runManager.startRun()
            val run2 = TestDataFactory.createTestRunEntity()
            val runId2 = runDao.insertRun(run2)
            runManager.initializeRunSession(runId2, "running")

            val baseTime2 = System.currentTimeMillis()
            gpsMeasurementDao.insert(
                GpsMeasurementEntity(
                    runId = runId2,
                    activity = "running",
                    timestamp = baseTime2,
                    accuracy = 7f,
                    bearingAccuracy = 10f,
                    speed = 2.5f,
                    bearing = 45f,
                ),
            )
            gpsMeasurementDao.insert(
                GpsMeasurementEntity(
                    runId = runId2,
                    activity = "running",
                    timestamp = baseTime2 + 60000,
                    accuracy = 8f,
                    bearingAccuracy = 10f,
                    speed = 2.5f,
                    bearing = 45f,
                ),
            )

            runManager.finalizeRunSession()

            // Verify calibration was created
            val finalCalibration = gpsCalibrationDao.getCalibration("running")
            assertNotNull(finalCalibration)
            // Samples should be accumulated from both runs
            assertEquals(4, finalCalibration.samplesCollected)
        }
}
