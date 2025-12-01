package com.fghbuild.sidekick.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.ui.components.heartRateChart
import com.fghbuild.sidekick.ui.components.paceChart
import com.fghbuild.sidekick.ui.components.routeMap

@Composable
fun runInProgressScreen(
    modifier: Modifier = Modifier,
    runData: RunData = RunData(),
    heartRateData: HeartRateData = HeartRateData(),
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Run in Progress",
            fontSize = 28.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Distance: ${String.format("%.2f", runData.distanceMeters / 1000.0)} km",
                fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pace: ${formatPace(runData.paceMinPerKm)} min/km",
                fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Heart Rate: ${heartRateData.currentBpm} bpm",
                fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Duration: ${formatDuration(runData.durationMillis)}",
                fontSize = 20.sp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        routeMap(
            routePoints = runData.routePoints,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        paceChart(
            paceHistory = runData.paceHistory,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        heartRateChart(
            measurements = heartRateData.measurements,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onResume) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Close, contentDescription = "Stop")
            }
        }
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
