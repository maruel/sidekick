package com.fghbuild.sidekick.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fghbuild.sidekick.R
import com.fghbuild.sidekick.database.RunEntity
import com.fghbuild.sidekick.util.PaceUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun historyScreen(
    modifier: Modifier = Modifier,
    runs: List<RunEntity> = emptyList(),
    onDeleteRun: (Long) -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.history_title),
            fontSize = 28.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (runs.isEmpty()) {
            Text(
                text = stringResource(R.string.history_empty),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(runs) { run ->
                    runHistoryCard(
                        run = run,
                        onDelete = { onDeleteRun(run.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun runHistoryCard(
    run: RunEntity,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDate(run.startTime),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.content_description_delete_run),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                statItem(
                    label = stringResource(R.string.history_distance),
                    value = stringResource(R.string.format_distance, run.distanceMeters / 1000.0),
                )
                statItem(
                    label = stringResource(R.string.history_duration),
                    value = PaceUtils.formatDuration(run.durationMillis),
                )
                statItem(
                    label = stringResource(R.string.history_pace),
                    value = PaceUtils.formatPace(run.averagePaceMinPerKm) + " " + stringResource(R.string.unit_pace_suffix),
                )
            }

            if (run.averageHeartRate > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    statItem(
                        label = stringResource(R.string.history_avg_hr),
                        value = "${run.averageHeartRate} ${stringResource(R.string.unit_bpm)}",
                    )
                    statItem(
                        label = stringResource(R.string.history_max_hr),
                        value = "${run.maxHeartRate} ${stringResource(R.string.unit_bpm)}",
                    )
                    statItem(
                        label = stringResource(R.string.history_min_hr),
                        value = "${run.minHeartRate} ${stringResource(R.string.unit_bpm)}",
                    )
                }
            }
        }
    }
}

@Composable
private fun statItem(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
