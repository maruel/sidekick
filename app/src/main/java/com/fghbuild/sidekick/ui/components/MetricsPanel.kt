package com.fghbuild.sidekick.ui.components

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fghbuild.sidekick.R
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.HrmDevice
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.util.PaceUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.StateFlow

@Composable
fun metricsPanel(
    runData: RunData,
    heartRateData: HeartRateData,
    isRunning: Boolean,
    onHeartRateLongPress: () -> Unit,
    onHeartRateCardClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        metricCard(
            label = stringResource(R.string.metrics_heart_rate),
            value = if (heartRateData.currentBpm > 0) "${heartRateData.currentBpm} ${stringResource(R.string.unit_bpm)}" else "--",
            emoji = "❤️",
            averageValue = if (heartRateData.averageBpm > 0) "${heartRateData.averageBpm}" else null,
            minValue = if (runData.heartRateHistory.isNotEmpty()) "${runData.heartRateHistory.map { it.bpm }.minOrNull() ?: 0}" else null,
            maxValue = if (runData.heartRateHistory.isNotEmpty()) "${runData.heartRateHistory.map { it.bpm }.maxOrNull() ?: 0}" else null,
            modifier = Modifier.weight(1f),
            onLongPress = onHeartRateLongPress,
            onClick = onHeartRateCardClick,
        )
        metricCard(
            label = stringResource(R.string.metrics_pace),
            value = if (isRunning) "${PaceUtils.formatPace(runData.paceMinPerKm)}${stringResource(R.string.unit_pace_suffix)}" else "--",
            emoji = "⚡",
            averageValue =
                if (runData.paceHistory.isNotEmpty()) {
                    PaceUtils.formatPace(
                        runData.paceHistory.map { it.pace }.average(),
                    )
                } else {
                    null
                },
            minValue =
                if (runData.paceHistory.isNotEmpty()) {
                    PaceUtils.formatPace(
                        runData.paceHistory.map { it.pace }.minOrNull() ?: 0.0,
                    )
                } else {
                    null
                },
            maxValue =
                if (runData.paceHistory.isNotEmpty()) {
                    PaceUtils.formatPace(
                        runData.paceHistory.map { it.pace }.maxOrNull() ?: 0.0,
                    )
                } else {
                    null
                },
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        metricCard(
            label = stringResource(R.string.metrics_distance),
            value = if (isRunning) stringResource(R.string.format_distance, runData.distanceMeters / 1000.0) else "--",
            emoji = "🛣️",
            averageValue = null,
            minValue = null,
            maxValue = null,
            modifier = Modifier.weight(1f),
        )
        metricCard(
            label = stringResource(R.string.metrics_duration),
            value = if (isRunning) PaceUtils.formatDuration(runData.durationMillis) else "--",
            emoji = "⏱️",
            averageValue = null,
            minValue = null,
            maxValue = null,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun historyMetricsPanel(
    runData: RunData,
    heartRateData: HeartRateData,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        metricCard(
            label = stringResource(R.string.metrics_heart_rate),
            value = "--",
            emoji = "❤️",
            averageValue = if (heartRateData.averageBpm > 0) "${heartRateData.averageBpm} ${stringResource(R.string.unit_bpm)}" else null,
            minValue = if (runData.heartRateHistory.isNotEmpty()) "${runData.heartRateHistory.map { it.bpm }.minOrNull() ?: 0}" else null,
            maxValue = if (runData.heartRateHistory.isNotEmpty()) "${runData.heartRateHistory.map { it.bpm }.maxOrNull() ?: 0}" else null,
            modifier = Modifier.weight(1f),
            displayAverageAsMain = true,
        )
        metricCard(
            label = stringResource(R.string.metrics_pace),
            value =
                if (runData.paceHistory.isNotEmpty()) {
                    PaceUtils.formatPace(
                        runData.paceHistory.map { it.pace }.average(),
                    )
                } else {
                    "--"
                },
            emoji = "⚡",
            averageValue =
                if (runData.paceHistory.isNotEmpty()) {
                    PaceUtils.formatPace(
                        runData.paceHistory.map { it.pace }.average(),
                    )
                } else {
                    null
                },
            minValue =
                if (runData.paceHistory.isNotEmpty()) {
                    PaceUtils.formatPace(
                        runData.paceHistory.map { it.pace }.minOrNull() ?: 0.0,
                    )
                } else {
                    null
                },
            maxValue =
                if (runData.paceHistory.isNotEmpty()) {
                    PaceUtils.formatPace(
                        runData.paceHistory.map { it.pace }.maxOrNull() ?: 0.0,
                    )
                } else {
                    null
                },
            modifier = Modifier.weight(1f),
            displayAverageAsMain = true,
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        metricCard(
            label = stringResource(R.string.metrics_distance),
            value = stringResource(R.string.format_distance, runData.distanceMeters / 1000.0),
            emoji = "🛣️",
            averageValue = null,
            minValue = null,
            maxValue = null,
            modifier = Modifier.weight(1f),
            displayAverageAsMain = true,
        )
        metricCard(
            label = stringResource(R.string.metrics_duration),
            value = PaceUtils.formatDuration(runData.durationMillis),
            emoji = "⏱️",
            averageValue = null,
            minValue = null,
            maxValue = null,
            modifier = Modifier.weight(1f),
            displayAverageAsMain = true,
        )
    }
}

@Composable
fun mainMetricsPanel(
    runData: RunData,
    heartRateData: HeartRateData,
    connectedDevice: HrmDevice?,
    userAge: Int,
    isRunning: Boolean,
    onHeartRateLongPress: () -> Unit,
    currentLocation: StateFlow<Location?>?,
    gpsAccuracyMeters: StateFlow<Float>?,
    onHeartRateCardClick: () -> Unit = {},
) {
    // Route map always visible
    val locationFromFlow =
        currentLocation?.let { flow ->
            val location by flow.collectAsState(initial = null)
            location
        }
    val userLocation =
        runData.routePoints.lastOrNull()?.let {
            LatLng(it.latitude, it.longitude)
        } ?: locationFromFlow?.let {
            LatLng(it.latitude, it.longitude)
        }
    routeMap(
        routePoints = runData.filteredRoutePoints.ifEmpty { runData.routePoints },
        userLocation = userLocation,
        gpsAccuracyMeters = gpsAccuracyMeters,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Metric cards
    metricsPanel(
        runData = runData,
        heartRateData = heartRateData,
        isRunning = isRunning,
        onHeartRateLongPress = onHeartRateLongPress,
        onHeartRateCardClick = onHeartRateCardClick,
    )

    // Show heart rate chart when device is connected
    if (connectedDevice != null) {
        Spacer(modifier = Modifier.height(4.dp))
        heartRateChart(
            heartRateHistory = runData.heartRateHistory,
            age = userAge,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
