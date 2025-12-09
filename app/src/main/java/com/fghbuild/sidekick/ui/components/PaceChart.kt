package com.fghbuild.sidekick.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.fghbuild.sidekick.data.PaceWithTime
import com.fghbuild.sidekick.util.PaceUtils
import com.fghbuild.sidekick.util.PaceZone

@Composable
fun paceChart(
    paceHistory: List<PaceWithTime>,
    modifier: Modifier = Modifier,
    showAllData: Boolean = false,
    isLiveRun: Boolean = false,
) {
    val currentTime = System.currentTimeMillis()
    val fiveMinutesInMillis = 5 * 60 * 1000L // 5 minutes in milliseconds
    val showFullData = remember { mutableStateOf(false) }

    // Determine the run duration to enable toggle only if > 5 minutes
    val oldestTimestamp = paceHistory.minByOrNull { it.timestamp }?.timestamp ?: currentTime
    val runDuration = currentTime - oldestTimestamp
    val canToggle = runDuration > fiveMinutesInMillis

    val displayData =
        when {
            showAllData -> paceHistory
            isLiveRun && showFullData.value -> paceHistory
            else -> paceHistory.filter { currentTime - it.timestamp <= fiveMinutesInMillis }
        }

    Column(
        modifier =
            modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Draw the graph with zones and labels - shoe emoji floats over it
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(4.dp)
                    .clickable(enabled = canToggle) {
                        showFullData.value = !showFullData.value
                    },
        ) {
            paceGraphCanvas(
                paceHistory = displayData,
                zones = PaceUtils.getPaceZones(),
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )

            // Shoe emoji floats over graph, below data points
            Text(
                "👟",
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp).zIndex(0.5f),
            )
        }
    }
}

@Composable
private fun paceGraphCanvas(
    paceHistory: List<PaceWithTime>,
    zones: List<PaceZone>,
    modifier: Modifier = Modifier,
) {
    val displayMin = PaceUtils.GRAPH_DISPLAY_MIN
    val displayMax = PaceUtils.GRAPH_DISPLAY_MAX
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier,
    ) {
        val width = size.width
        val height = size.height

        // Draw zone background bands - note: faster pace is at top, slower at bottom
        for (i in zones.indices) {
            val zone = zones[i]
            val zoneMinPace = if (i == zones.size - 1) displayMax else zones[i + 1].minPace
            val zoneMaxPace = zone.maxPace

            val zoneMaxY = height * ((zoneMinPace - displayMin) / (displayMax - displayMin))
            val zoneMinY = height * ((zoneMaxPace - displayMin) / (displayMax - displayMin))

            val zoneColor =
                when (zone.zone) {
                    1 -> Color(0xFFF44336) // Red - Recovery (slowest, at bottom)
                    2 -> Color(0xFFFF9800) // Orange - Easy
                    3 -> Color(0xFFFFC107) // Yellow - Moderate
                    4 -> Color(0xFF8BC34A) // Light Green - Tempo
                    5 -> Color(0xFF4CAF50) // Green - Fast (fastest, at top)
                    else -> Color.Gray
                }

            drawRect(
                color = zoneColor.copy(alpha = 0.25f),
                topLeft = Offset(0f, minOf(zoneMaxY, zoneMinY).toFloat()),
                size =
                    androidx.compose.ui.geometry.Size(
                        width = width.toFloat(),
                        height = kotlin.math.abs(zoneMinY - zoneMaxY).toFloat(),
                    ),
            )
        }

        // Draw grid lines and labels for zones
        for (zone in zones) {
            val zonePace = zone.maxPace
            val yPos = height * ((zonePace - displayMin) / (displayMax - displayMin))
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, yPos.toFloat()),
                end = Offset(width.toFloat(), yPos.toFloat()),
                strokeWidth = 1f,
            )

            // Draw zone label
            drawText(
                textMeasurer = textMeasurer,
                text = "${"%.1f".format(zonePace)}",
                topLeft = Offset(2f + with(density) { 24.dp.toPx() }, (yPos - 6f).toFloat()),
                style = androidx.compose.ui.text.TextStyle(fontSize = 8.sp),
            )
        }

        // Draw the line and points only if there's data
        if (paceHistory.isNotEmpty()) {
            // Calculate the time range for the x-axis scaling
            val timestamps = paceHistory.map { it.timestamp }
            val minTimestamp = timestamps.minOrNull() ?: 0L
            val maxTimestamp = timestamps.maxOrNull() ?: 0L
            val timeRange = maxTimestamp - minTimestamp

            // Draw the line connecting all points
            for (i in 0 until paceHistory.size - 1) {
                val currentPoint = paceHistory[i]
                val nextPoint = paceHistory[i + 1]

                // Calculate x positions based on timestamps
                val x1 =
                    if (timeRange > 0) {
                        (width * (currentPoint.timestamp - minTimestamp).toDouble()) /
                            timeRange.toDouble()
                    } else {
                        width * 0.5
                    }

                val y1 =
                    (height * ((currentPoint.pace - displayMin) / (displayMax - displayMin)))
                        .toFloat()
                        .coerceIn(0f, height)

                val x2 =
                    if (timeRange > 0) {
                        (width * (nextPoint.timestamp - minTimestamp).toDouble()) / timeRange.toDouble()
                    } else {
                        width * 0.5
                    }

                val y2 =
                    (height * ((nextPoint.pace - displayMin) / (displayMax - displayMin)))
                        .toFloat()
                        .coerceIn(0f, height)

                val pointColor =
                    PaceUtils.getZoneForPace(currentPoint.pace)?.let { zone ->
                        when (zone.zone) {
                            1 -> Color(0xFFF44336)
                            2 -> Color(0xFFFF9800)
                            3 -> Color(0xFFFFC107)
                            4 -> Color(0xFF8BC34A)
                            5 -> Color(0xFF4CAF50)
                            else -> primaryColor
                        }
                    }
                        ?: primaryColor

                drawLine(
                    color = pointColor,
                    start = Offset(x1.toFloat(), y1),
                    end = Offset(x2.toFloat(), y2),
                    strokeWidth = 2f,
                )
            }

            // Draw points for each measurement
            for (point in paceHistory) {
                val x =
                    if (timeRange > 0) {
                        (width * (point.timestamp - minTimestamp).toDouble()) / timeRange.toDouble()
                    } else {
                        width * 0.5
                    }

                val y =
                    (height * ((point.pace - displayMin) / (displayMax - displayMin)))
                        .toFloat()
                        .coerceIn(0f, height)

                val pointColor =
                    PaceUtils.getZoneForPace(point.pace)?.let { zone ->
                        when (zone.zone) {
                            1 -> Color(0xFFF44336)
                            2 -> Color(0xFFFF9800)
                            3 -> Color(0xFFFFC107)
                            4 -> Color(0xFF8BC34A)
                            5 -> Color(0xFF4CAF50)
                            else -> primaryColor
                        }
                    }
                        ?: primaryColor

                drawCircle(
                    color = pointColor,
                    radius = 3f,
                    center = Offset(x.toFloat(), y),
                )
            }
        }
    }
}
