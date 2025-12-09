// Data Access Object for RoutePoint entities. Provides database operations for storing and retrieving GPS route points.
package com.fghbuild.sidekick.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RoutePointDao {
    @Insert
    suspend fun insertRoutePoints(points: List<RoutePointEntity>)

    @Query("SELECT * FROM route_points WHERE runId = :runId ORDER BY timestamp ASC")
    suspend fun getRoutePointsForRun(runId: Long): List<RoutePointEntity>

    @Query("DELETE FROM route_points WHERE runId = :runId")
    suspend fun deleteRoutePointsForRun(runId: Long)
}
