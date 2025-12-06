package com.fghbuild.sidekick.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.RunData

@Composable
fun MetricsPanel(
    runData: RunData,
    heartRateData: HeartRateData,
    isRunning: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            label = "Heart Rate",
            value = if (heartRateData.currentBpm > 0) "${heartRateData.currentBpm} bpm" else "--",
            averageValue = if (heartRateData.averageBpm > 0) "${heartRateData.averageBpm} bpm" else null,
            maxValue = if (heartRateData.measurements.isNotEmpty()) "${heartRateData.measurements.maxOrNull() ?: 0} bpm" else null,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = "Pace",
            value = if (isRunning) formatPace(runData.paceMinPerKm) else "--",
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            label = "Distance",
            value = if (isRunning) String.format("%.2f km", runData.distanceMeters / 1000.0) else "--",
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = "Duration",
            value = if (isRunning) formatDuration(runData.durationMillis) else "--",
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatPace(paceMinPerKm: Double): String {
    return if (paceMinPerKm.isFinite() && paceMinPerKm > 0) {
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        String.format("%d:%02d", minutes, seconds)
    } else {
        "0:00"
    }
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
