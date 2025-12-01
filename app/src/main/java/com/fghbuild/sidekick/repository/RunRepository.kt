package com.fghbuild.sidekick.repository

import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.RoutePoint
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.database.RoutePointDao
import com.fghbuild.sidekick.database.RoutePointEntity
import com.fghbuild.sidekick.database.RunDao
import com.fghbuild.sidekick.database.RunEntity
import kotlinx.coroutines.flow.Flow

class RunRepository(
    private val runDao: RunDao,
    private val routePointDao: RoutePointDao,
) {
    fun getAllRuns(): Flow<List<RunEntity>> {
        return runDao.getAllRuns()
    }

    suspend fun saveRun(
        runData: RunData,
        heartRateData: HeartRateData,
        startTime: Long,
        endTime: Long,
    ) {
        val averagePace =
            if (runData.paceHistory.isNotEmpty()) {
                runData.paceHistory.average()
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
                minHeartRate =
                    heartRateData.measurements.minOrNull() ?: 0,
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
}
