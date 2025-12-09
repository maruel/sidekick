// Details view for a historical run from the run history.
// Shows route map, heart rate and pace charts, and comprehensive run statistics with delete functionality.

package com.fghbuild.sidekick.ui.screens

import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fghbuild.sidekick.R
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.ui.components.heartRateChart
import com.fghbuild.sidekick.ui.components.historyMetricsPanel
import com.fghbuild.sidekick.ui.components.paceChart
import com.fghbuild.sidekick.ui.components.routeMap
import com.fghbuild.sidekick.ui.components.screenContainer
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun historyRunDetailScreen(
    modifier: Modifier = Modifier,
    runData: RunData,
    heartRateData: HeartRateData,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    userAge: Int,
    gpsAccuracyMeters: StateFlow<Float>?,
    currentLocation: StateFlow<Location?>?,
    runStartTime: Long,
) {
    BackHandler { onBack() }

    screenContainer(modifier = modifier.fillMaxSize()) {
        // Back button, timestamp and delete icon on top
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.content_description_back),
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatDate(runStartTime),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.content_description_delete_run),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Route map
        val locationFromFlow =
            currentLocation?.let { flow ->
                val location by flow.collectAsState(initial = null)
                location
            }
        val userLocation =
            runData.routePoints.lastOrNull()?.let {
                com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
            } ?: locationFromFlow?.let {
                com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
            }
        routeMap(
            routePoints = runData.filteredRoutePoints.ifEmpty { runData.routePoints },
            userLocation = userLocation,
            gpsAccuracyMeters = gpsAccuracyMeters,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(4.dp))

        // History metrics panel with average as main value
        historyMetricsPanel(
            runData = runData,
            heartRateData = heartRateData,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Show pace chart if there are pace measurements
        if (runData.paceHistory.isNotEmpty()) {
            paceChart(
                paceHistory = runData.paceHistory,
                modifier = Modifier.fillMaxWidth(),
                showAllData = true,
                isLiveRun = false,
            )
        }

        // Show heart rate chart if there are heart rate measurements
        if (runData.heartRateHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            heartRateChart(
                heartRateHistory = runData.heartRateHistory,
                age = userAge,
                modifier = Modifier.fillMaxWidth(),
                showAllData = true,
                isLiveRun = false,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
