package com.fghbuild.sidekick.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fghbuild.sidekick.R
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.HrmDevice
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.util.PaceUtils

@Composable
fun metricsPanel(
    runData: RunData,
    heartRateData: HeartRateData,
    isRunning: Boolean,
    onHeartRateLongPress: () -> Unit = {},
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
            minValue = if (heartRateData.measurements.isNotEmpty()) "${heartRateData.measurements.minOrNull() ?: 0}" else null,
            maxValue = if (heartRateData.measurements.isNotEmpty()) "${heartRateData.measurements.maxOrNull() ?: 0}" else null,
            modifier = Modifier.weight(1f),
            onLongPress = onHeartRateLongPress,
        )
        metricCard(
            label = stringResource(R.string.metrics_pace),
            value = if (isRunning) "${PaceUtils.formatPace(runData.paceMinPerKm)}${stringResource(R.string.unit_pace_suffix)}" else "--",
            emoji = "⚡",
            averageValue = if (runData.paceHistory.isNotEmpty()) PaceUtils.formatPace(runData.paceHistory.average()) else null,
            minValue = if (runData.paceHistory.isNotEmpty()) PaceUtils.formatPace(runData.paceHistory.minOrNull() ?: 0.0) else null,
            maxValue = if (runData.paceHistory.isNotEmpty()) PaceUtils.formatPace(runData.paceHistory.maxOrNull() ?: 0.0) else null,
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
            modifier = Modifier.weight(1f),
        )
        metricCard(
            label = stringResource(R.string.metrics_duration),
            value = if (isRunning) PaceUtils.formatDuration(runData.durationMillis) else "--",
            emoji = "⏱️",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun mainMetricsPanel(
    runData: RunData,
    heartRateData: HeartRateData,
    connectedDevice: HrmDevice? = null,
    userAge: Int = 30,
    isRunning: Boolean = false,
    onHeartRateLongPress: () -> Unit = {},
) {
    // Metric cards
    metricsPanel(
        runData = runData,
        heartRateData = heartRateData,
        isRunning = isRunning,
        onHeartRateLongPress = onHeartRateLongPress,
    )

    // Show heart rate chart when device is connected
    if (connectedDevice != null) {
        Spacer(modifier = Modifier.height(4.dp))
        heartRateChart(
            measurements = heartRateData.measurements,
            age = userAge,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
