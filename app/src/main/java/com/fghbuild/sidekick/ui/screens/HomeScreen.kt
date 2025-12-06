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
import com.fghbuild.sidekick.ui.components.heartRateChart

@Composable
fun homeScreen(
    modifier: Modifier = Modifier,
    isRunning: Boolean = false,
    onStartRun: () -> Unit = {},
    onStopRun: () -> Unit = {},
    heartRateData: HeartRateData = HeartRateData(),
    connectedDevice: HrmDevice? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sidekick",
            fontSize = 32.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isRunning) "Run in progress..." else "Ready to run!",
        )

        // Show connected device and HR metrics if device is connected
        if (connectedDevice != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Connected: ${connectedDevice.name}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Heart Rate: ${heartRateData.currentBpm} bpm",
                fontSize = 20.sp,
            )
            if (heartRateData.measurements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                heartRateChart(
                    measurements = heartRateData.measurements,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
    }
}
