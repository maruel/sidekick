package com.fghbuild.sidekick.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.fixtures.TestDataFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GpsCalibrationDatabaseTest {
    private lateinit var database: SidekickDatabase
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
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun gpsMeasurement_canBeInsertedAndRetrieved() =
        runBlocking {
            val run = TestDataFactory.createTestRunEntity()
            val runId = runDao.insertRun(run)

            val measurement =
                GpsMeasurementEntity(
                    runId = runId,
                    activity = "running",
                    timestamp = System.currentTimeMillis(),
                    accuracy = 5f,
                    bearingAccuracy = 10f,
                    speed = 2.5f,
                    bearing = 45f,
                )
            gpsMeasurementDao.insert(measurement)

            val retrieved = gpsMeasurementDao.getRunMeasurements(runId)
            assertEquals(1, retrieved.size)
            assertEquals("running", retrieved[0].activity)
            assertEquals(5f, retrieved[0].accuracy)
        }

    @Test
    fun gpsMeasurement_supportsNullRunIdForPrewarmup() =
        runBlocking {
            // Pre-warmup measurement with null runId
            val measurement =
                GpsMeasurementEntity(
                    runId = null,
                    activity = "running",
                    timestamp = System.currentTimeMillis(),
                    accuracy = 8f,
                    bearingAccuracy = 12f,
                    speed = 0f,
                    bearing = 0f,
                )
            gpsMeasurementDao.insert(measurement)

            val retrieved = gpsMeasurementDao.getPrewarmup("running")
            assertEquals(1, retrieved.size)
            assertTrue(retrieved[0].runId == null)
        }

    @Test
    fun gpsCalibration_canBeInsertedAndRetrieved() =
        runBlocking {
            val calibration =
                GpsCalibrationEntity(
                    activity = "running",
                    avgAccuracyMeters = 5.5,
                    p95AccuracyMeters = 10.2,
                    avgBearingAccuracyDegrees = 8.5,
                    samplesCollected = 100,
                    kalmanProcessNoise = 0.001,
                    kalmanMeasurementNoise = 15.0,
                    lastUpdated = System.currentTimeMillis(),
                )
            gpsCalibrationDao.upsert(calibration)

            val retrieved = gpsCalibrationDao.getCalibration("running")
            assertNotNull(retrieved)
            assertEquals(5.5, retrieved.avgAccuracyMeters)
            assertEquals(100, retrieved.samplesCollected)
        }

    @Test
    fun gpsCalibration_upsertUpdatesExisting() =
        runBlocking {
            val calibration1 =
                GpsCalibrationEntity(
                    activity = "running",
                    avgAccuracyMeters = 5.5,
                    p95AccuracyMeters = 10.2,
                    avgBearingAccuracyDegrees = 8.5,
                    samplesCollected = 100,
                    kalmanProcessNoise = 0.001,
                    kalmanMeasurementNoise = 15.0,
                    lastUpdated = System.currentTimeMillis(),
                )
            gpsCalibrationDao.upsert(calibration1)

            val calibration2 =
                calibration1.copy(
                    avgAccuracyMeters = 6.5,
                    samplesCollected = 150,
                )
            gpsCalibrationDao.upsert(calibration2)

            val all = gpsCalibrationDao.getAllCalibrations()
            assertEquals(1, all.size)
            assertEquals(6.5, all[0].avgAccuracyMeters)
            assertEquals(150, all[0].samplesCollected)
        }

    @Test
    fun gpsMeasurement_cascadeDeleteRemovesMeasurements() =
        runBlocking {
            val run = TestDataFactory.createTestRunEntity()
            val runId = runDao.insertRun(run)

            val measurement =
                GpsMeasurementEntity(
                    runId = runId,
                    activity = "running",
                    timestamp = System.currentTimeMillis(),
                    accuracy = 5f,
                    bearingAccuracy = 10f,
                    speed = 2.5f,
                    bearing = 45f,
                )
            gpsMeasurementDao.insert(measurement)

            var retrieved = gpsMeasurementDao.getRunMeasurements(runId)
            assertEquals(1, retrieved.size)

            // Delete the run
            val runToDelete = runDao.getRunById(runId)
            assertNotNull(runToDelete)
            runDao.deleteRun(runToDelete)

            // Measurements should be cascade deleted
            retrieved = gpsMeasurementDao.getRunMeasurements(runId)
            assertEquals(0, retrieved.size)
        }
}
