package com.fghbuild.sidekick.location

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.core.app.ActivityCompat
import com.fghbuild.sidekick.data.RoutePoint
import com.fghbuild.sidekick.database.GpsMeasurementDao
import com.fghbuild.sidekick.database.GpsMeasurementEntity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

class LocationTracker(
    private val context: Context,
    private val gpsMeasurementDao: GpsMeasurementDao,
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _routePoints = MutableStateFlow<List<RoutePoint>>(emptyList())
    val routePoints: StateFlow<List<RoutePoint>> = _routePoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _currentAccuracyMeters = MutableStateFlow(0f)
    val currentAccuracyMeters: StateFlow<Float> = _currentAccuracyMeters.asStateFlow()

    private var isTracking = false

    // Atomic reference to current run context - set BEFORE requesting location updates
    @Volatile
    private var currentRunContext: RunContext? = null

    private data class RunContext(
        val runId: Long?,
        val activity: String?,
    )

    private val locationCallback =
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    _currentLocation.value = location
                    _currentAccuracyMeters.value = location.accuracy
                    _routePoints.value =
                        _routePoints.value +
                        RoutePoint(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timestamp = System.currentTimeMillis(),
                        )

                    // Collect GPS measurement for calibration
                    recordGpsMeasurement(location)
                }
            }
        }

    private fun recordGpsMeasurement(location: Location) {
        // Record measurements when tracking is active (with or without a run)
        val runContext = currentRunContext ?: return

        // Queue measurement for async insertion
        // runId can be null for pre-warmup calibration data collection
        val measurement =
            GpsMeasurementEntity(
                runId = runContext.runId,
                activity = runContext.activity,
                timestamp = System.currentTimeMillis(),
                accuracy = location.accuracy,
                bearingAccuracy =
                    location.bearingAccuracyDegrees.takeIf { it >= 0f } ?: 0f,
                speed = location.speed,
                bearing = location.bearing,
            )
        pendingMeasurements.add(measurement)
    }

    // Thread-safe queue of measurements waiting to be inserted
    // CopyOnWriteArrayList is safe for concurrent read/write from location callbacks
    private val pendingMeasurements = CopyOnWriteArrayList<GpsMeasurementEntity>()

    suspend fun flushPendingMeasurements() {
        if (pendingMeasurements.isNotEmpty()) {
            pendingMeasurements.forEach { gpsMeasurementDao.insert(it) }
            pendingMeasurements.clear()
        }
    }

    fun startTracking(runId: Long?) {
        // Update run context even if already tracking (allows switching from pre-warmup to run)
        currentRunContext = RunContext(runId = runId, activity = null)

        if (isTracking) return

        // CRITICAL: Set run context BEFORE requesting updates to prevent race condition
        // runId can be null for pre-warmup GPS measurement collection

        if (
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            isTracking = true
            val locationRequest =
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null,
            )
        }
    }

    fun stopTracking() {
        isTracking = false
        // Clear run context first to prevent new measurements being queued
        currentRunContext = null
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun resetRoute() {
        _routePoints.value = emptyList()
    }
}
