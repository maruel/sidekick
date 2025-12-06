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
import androidx.compose.material.icons.filled.Pause
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
import com.fghbuild.sidekick.ui.components.MetricsPanel

@Composable
fun runInProgressScreen(
    modifier: Modifier = Modifier,
    runData: RunData = RunData(),
    heartRateData: HeartRateData = HeartRateData(),
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    connectedDevice: com.fghbuild.sidekick.data.HrmDevice? = null,
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
        // Top title - fixed height (matches home screen)
        Text(
            text = "Run in progress...",
            fontSize = 28.sp,
        )

        Spacer(modifier = Modifier.height(32.dp))

        MetricsPanel(
            runData = runData,
            heartRateData = heartRateData,
            isRunning = true,
        )

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

        Spacer(modifier = Modifier.weight(1f))

        // Show connected device if available
        if (connectedDevice != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Connected: ${connectedDevice.name}",
                fontSize = 14.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (runData.isPaused) {
                IconButton(onClick = onResume) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                }
            } else {
                IconButton(onClick = onPause) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                }
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Close, contentDescription = "Stop")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
