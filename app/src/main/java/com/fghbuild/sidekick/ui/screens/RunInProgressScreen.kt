package com.fghbuild.sidekick.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.HrmDevice
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.ui.components.gpsAccuracyIndicator
import com.fghbuild.sidekick.ui.components.mainMetricsPanel
import com.fghbuild.sidekick.ui.components.paceChart
import com.fghbuild.sidekick.ui.components.routeMap
import com.fghbuild.sidekick.ui.components.screenContainer
import kotlinx.coroutines.flow.StateFlow

@Composable
fun runInProgressScreen(
    modifier: Modifier = Modifier,
    runData: RunData = RunData(),
    heartRateData: HeartRateData = HeartRateData(),
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    connectedDevice: HrmDevice? = null,
    userAge: Int = 30,
    gpsAccuracyMeters: StateFlow<Float>? = null,
) {
    screenContainer(modifier = modifier.fillMaxSize()) {
        // Pause/Resume and Stop buttons at top
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (runData.isPaused) {
                Button(onClick = onResume) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Resume")
                }
            } else {
                Button(onClick = onPause) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = "Pause",
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Pause")
                }
            }
            Button(onClick = onStop) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Stop",
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Stop")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        mainMetricsPanel(
            runData = runData,
            heartRateData = heartRateData,
            connectedDevice = connectedDevice,
            userAge = userAge,
            isRunning = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        paceChart(
            paceHistory = runData.paceHistory,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        routeMap(
            routePoints = runData.routePoints,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

        // GPS Accuracy indicator at bottom
        gpsAccuracyMeters?.let { accuracyFlow ->
            val accuracy by accuracyFlow.collectAsState()
            gpsAccuracyIndicator(accuracyMeters = accuracy)
        }
    }
}
