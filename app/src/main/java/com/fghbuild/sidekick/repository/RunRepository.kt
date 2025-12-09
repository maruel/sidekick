package com.fghbuild.sidekick.repository

import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.RoutePoint
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.database.RoutePointDao
import com.fghbuild.sidekick.database.RoutePointEntity
import com.fghbuild.sidekick.database.RunDao
import com.fghbuild.sidekick.database.RunEntity
import com.fghbuild.sidekick.util.GeoUtils
import kotlinx.coroutines.flow.Flow

class RunRepository(
    private val runDao: RunDao,
    private val routePointDao: RoutePointDao,
) {
    fun getAllRuns(): Flow<List<RunEntity>> {
        return runDao.getAllRuns()
    }

    suspend fun createRun(runEntity: RunEntity): Long {
        return runDao.insertRun(runEntity)
    }

    suspend fun saveRun(
        runData: RunData,
        heartRateData: HeartRateData,
        startTime: Long,
        endTime: Long,
    ): Long {
        val averagePace =
            if (runData.paceHistory.isNotEmpty()) {
                runData.paceHistory.map { it.pace }.average()
            } else {
                0.0
            }

        val runEntity =
            RunEntity(
                startTime = startTime,
                endTime = endTime,
                distanceMeters = runData.distanceMeters,
                durationMillis = runData.durationMillis,
                averagePaceMinPerKm = averagePace,
                maxHeartRate = heartRateData.measurements.maxOrNull() ?: 0,
                minHeartRate = heartRateData.measurements.minOrNull() ?: 0,
                averageHeartRate = heartRateData.averageBpm,
            )

        val runId = runDao.insertRun(runEntity)

        val routePointEntities =
            runData.routePoints.map { point ->
                RoutePointEntity(
                    runId = runId,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    timestamp = point.timestamp,
                )
            }

        if (routePointEntities.isNotEmpty()) {
            routePointDao.insertRoutePoints(routePointEntities)
        }

        return runId
    }

    suspend fun deleteRun(runId: Long) {
        val run = runDao.getRunById(runId)
        if (run != null) {
            routePointDao.deleteRoutePointsForRun(runId)
            runDao.deleteRun(run)
        }
    }

    suspend fun getRoutePointsForRun(runId: Long): List<RoutePoint> {
        return routePointDao
            .getRoutePointsForRun(runId)
            .map { entity ->
                RoutePoint(
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    timestamp = entity.timestamp,
                )
            }
    }

    suspend fun getCompleteRunData(runId: Long): RunData? {
        val runEntity = runDao.getRunById(runId) ?: return null

        val routePoints = getRoutePointsForRun(runId)
        if (routePoints.isEmpty()) {
            return RunData(
                distanceMeters = runEntity.distanceMeters,
                paceMinPerKm = runEntity.averagePaceMinPerKm,
                durationMillis = runEntity.durationMillis,
                routePoints = emptyList(),
                filteredRoutePoints = emptyList(),
                paceHistory = emptyList(),
                heartRateHistory = emptyList(),
                isRunning = false,
                isPaused = false,
            )
        }

        // Reconstruct pace history from route points
        val paceHistory = mutableListOf<com.fghbuild.sidekick.data.PaceWithTime>()
        for (i in 1 until routePoints.size) {
            val prevPoint = routePoints[i - 1]
            val currentPoint = routePoints[i]
            val distanceDelta =
                GeoUtils.calculateDistanceMeters(
                    prevPoint.latitude,
                    prevPoint.longitude,
                    currentPoint.latitude,
                    currentPoint.longitude,
                )
            val timeDeltaMinutes =
                (currentPoint.timestamp - prevPoint.timestamp) / 1000.0 / 60.0
            if (timeDeltaMinutes > 0 && distanceDelta > 0) {
                val pace = timeDeltaMinutes / (distanceDelta / 1000.0)
                paceHistory.add(
                    com.fghbuild.sidekick.data.PaceWithTime(
                        pace = pace,
                        timestamp = currentPoint.timestamp,
                    ),
                )
            }
        }

        // Reconstruct heart rate history from the stored min/avg/max
        // Note: We don't have individual HR samples stored, so we create placeholders
        val heartRateHistory =
            if (runEntity.averageHeartRate > 0) {
                val startTime = routePoints.first().timestamp
                val endTime = routePoints.last().timestamp
                val duration = endTime - startTime
                listOf(
                    com.fghbuild.sidekick.data.HeartRateWithTime(
                        bpm = runEntity.minHeartRate,
                        timestamp = startTime,
                    ),
                    com.fghbuild.sidekick.data.HeartRateWithTime(
                        bpm = runEntity.averageHeartRate,
                        timestamp = startTime + duration / 2,
                    ),
                    com.fghbuild.sidekick.data.HeartRateWithTime(
                        bpm = runEntity.maxHeartRate,
                        timestamp = endTime,
                    ),
                )
            } else {
                emptyList()
            }

        return RunData(
            distanceMeters = runEntity.distanceMeters,
            paceMinPerKm = runEntity.averagePaceMinPerKm,
            durationMillis = runEntity.durationMillis,
            routePoints = routePoints,
            // Use route points as filtered since we don't have them separately stored
            filteredRoutePoints = routePoints,
            paceHistory = paceHistory,
            heartRateHistory = heartRateHistory,
            isRunning = false,
            isPaused = false,
        )
    }
}
