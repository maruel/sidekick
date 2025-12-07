package com.fghbuild.sidekick.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fghbuild.sidekick.R
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.HrmDevice
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.ui.components.gpsAccuracyIndicator
import com.fghbuild.sidekick.ui.components.mainMetricsPanel
import com.fghbuild.sidekick.ui.components.screenContainer
import kotlinx.coroutines.flow.StateFlow
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
    gpsAccuracyMeters: StateFlow<Float>? = null,
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

    val context = LocalContext.current
    LaunchedEffect(showDisconnectToast.value) {
        if (showDisconnectToast.value) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.home_device_disconnected))
                showDisconnectToast.value = false
            }
        }
    }

    screenContainer(modifier = modifier.fillMaxSize()) {
        // Start button at top
        Button(onClick = onStartRun) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.content_description_start),
                modifier = Modifier.padding(end = 4.dp),
            )
            Text(stringResource(R.string.home_start_run))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Metric cards and heart rate chart
        mainMetricsPanel(
            runData = runData,
            heartRateData = heartRateData,
            connectedDevice = connectedDevice,
            userAge = userAge,
            isRunning = isRunning,
            onHeartRateLongPress = {
                if (connectedDevice != null) {
                    onDisconnect()
                    showDisconnectToast.value = true
                }
            },
        )

        Spacer(modifier = Modifier.weight(1f))

        // Show button to pair if not connected
        if (connectedDevice == null) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = { showPairingDialog.value = true }) {
                Text(stringResource(R.string.home_connect_hr_monitor))
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // GPS Accuracy indicator at bottom
        gpsAccuracyMeters?.let { accuracyFlow ->
            val accuracy by accuracyFlow.collectAsState()
            gpsAccuracyIndicator(accuracyMeters = accuracy)
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data ->
            Snackbar(snackbarData = data)
        },
    )
}
