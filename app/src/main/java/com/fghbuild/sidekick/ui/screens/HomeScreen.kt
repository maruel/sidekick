package com.fghbuild.sidekick.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.HrmDevice
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.ui.components.heartRateChart
import com.fghbuild.sidekick.ui.components.metricsPanel
import kotlinx.coroutines.launch

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
    discoveredDevices: List<HrmDevice> = emptyList(),
    isScanning: Boolean = false,
    onStartScanning: () -> Unit = {},
    onStopScanning: () -> Unit = {},
    onSelectDevice: (HrmDevice) -> Unit = {},
    onDisconnect: () -> Unit = {},
) {
    val showPairingDialog = remember { mutableStateOf(false) }
    val showDisconnectToast = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showPairingDialog.value) {
        devicePairingDialog(
            discoveredDevices = discoveredDevices,
            connectedDevice = connectedDevice,
            isScanning = isScanning,
            onStartScanning = onStartScanning,
            onStopScanning = onStopScanning,
            onSelectDevice = { device ->
                onStopScanning()
                onSelectDevice(device)
                showPairingDialog.value = false
            },
            onDisconnect = onDisconnect,
            onDismiss = { showPairingDialog.value = false },
        )
    }

    LaunchedEffect(showDisconnectToast.value) {
        if (showDisconnectToast.value) {
            scope.launch {
                snackbarHostState.showSnackbar("Device disconnected")
                showDisconnectToast.value = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(snackbarData = data)
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Start button at top
            Button(onClick = onStartRun) {
                Text("Start Run")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metric cards grid
            metricsPanel(
                runData = runData,
                heartRateData = heartRateData,
                isRunning = isRunning,
                onHeartRateLongPress = {
                    if (connectedDevice != null) {
                        onDisconnect()
                        showDisconnectToast.value = true
                    }
                },
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

            // Show connected device or button to pair
            if (connectedDevice != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Connected: ${connectedDevice.name}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { showPairingDialog.value = true }) {
                    Text("Connect Heart Rate Monitor")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
