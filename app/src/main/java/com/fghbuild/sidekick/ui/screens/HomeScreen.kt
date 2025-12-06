package com.fghbuild.sidekick.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.HrmDevice
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.ui.components.heartRateChart
import com.fghbuild.sidekick.ui.components.metricsPanel

@Composable
fun homeScreen(
    modifier: Modifier = Modifier,
    isRunning: Boolean = false,
    onStartRun: () -> Unit = {},
    onStopRun: () -> Unit = {},
    runData: RunData = RunData(),
    heartRateData: HeartRateData = HeartRateData(),
    connectedDevice: HrmDevice? = null,
    userAge: Int = 30,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top title - fixed height
        Text(
            text = if (isRunning) "Run in progress..." else "Ready to run!",
            fontSize = 28.sp,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Metric cards grid
        metricsPanel(
            runData = runData,
            heartRateData = heartRateData,
            isRunning = isRunning,
        )

        // Show heart rate chart when device is connected and has measurements
        if (connectedDevice != null) {
            Spacer(modifier = Modifier.height(24.dp))
            heartRateChart(
                measurements = heartRateData.measurements,
                age = userAge,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Show connected device if available
        if (connectedDevice != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Connected: ${connectedDevice.name}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isRunning) {
            Button(
                onClick = onStopRun,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("Stop Run")
            }
        } else {
            Button(onClick = onStartRun) {
                Text("Start Run")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
