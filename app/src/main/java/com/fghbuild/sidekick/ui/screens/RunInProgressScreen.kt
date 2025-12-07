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
import android.location.Location
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fghbuild.sidekick.R
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.HrmDevice
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.ui.components.gpsAccuracyIndicator
import com.fghbuild.sidekick.ui.components.mainMetricsPanel
import com.fghbuild.sidekick.ui.components.paceChart
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
    currentLocation: StateFlow<Location?>? = null,
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
                        contentDescription = stringResource(R.string.run_resume),
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(stringResource(R.string.run_resume))
                }
            } else {
                Button(onClick = onPause) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = stringResource(R.string.run_pause),
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(stringResource(R.string.run_pause))
                }
            }
            Button(onClick = onStop) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.run_stop),
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(stringResource(R.string.run_stop))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        mainMetricsPanel(
            runData = runData,
            heartRateData = heartRateData,
            connectedDevice = connectedDevice,
            userAge = userAge,
            isRunning = true,
            currentLocation = currentLocation,
        )

        Spacer(modifier = Modifier.height(4.dp))

        paceChart(
            paceHistory = runData.paceHistory,
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
