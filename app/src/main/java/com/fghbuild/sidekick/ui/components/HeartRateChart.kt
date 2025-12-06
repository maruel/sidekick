package com.fghbuild.sidekick.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fghbuild.sidekick.util.HeartRateUtils

@Composable
fun heartRateChart(
    measurements: List<Int>,
    age: Int = 0,
    modifier: Modifier = Modifier,
) {
    val last5Minutes = measurements.takeLast(300) // 5 minutes at 1 Hz = 300 samples

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Heart Rate (Last 5 Minutes)",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (last5Minutes.isEmpty()) {
            Text(
                "No data yet",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            // Draw the graph with zones
            heartRateGraphCanvas(
                measurements = last5Minutes,
                age = age,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun heartRateGraphCanvas(
    measurements: List<Int>,
    age: Int,
    modifier: Modifier = Modifier,
) {
    if (measurements.isEmpty()) {
        return
    }

    val maxHR = HeartRateUtils.calculateMaxHeartRate(age)
    val zones = HeartRateUtils.getHeartRateZones(age)

    // Use max HR as display max to ensure all 5 zones are visible
    val displayMax = maxHR.toFloat()

    if (measurements.size > 1) {
        Canvas(
            modifier =
                modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(8.dp),
        ) {
            val width = size.width
            val height = size.height
            val xStep = width / (measurements.size - 1)

            // Draw zone background bands
            for (zone in zones) {
                val zoneTopBpm = zone.maxBpm.toFloat()
                val zoneBottomBpm = zone.minBpm.toFloat()
                val zoneTopY = height * (1 - zoneTopBpm / displayMax)
                val zoneBottomY = height * (1 - zoneBottomBpm / displayMax)

                val zoneColor =
                    when (zone.zone) {
                        1 -> Color(0xFF4CAF50) // Green - Rest
                        2 -> Color(0xFF8BC34A) // Light Green - Light
                        3 -> Color(0xFFFFC107) // Yellow - Moderate
                        4 -> Color(0xFFFF9800) // Orange - Tempo
                        5 -> Color(0xFFF44336) // Red - Max
                        else -> Color.Gray
                    }

                drawRect(
                    color = zoneColor.copy(alpha = 0.1f),
                    topLeft = Offset(0f, zoneTopY),
                    size =
                        androidx.compose.ui.geometry.Size(
                            width = width,
                            height = zoneBottomY - zoneTopY,
                        ),
                )
            }

            // Draw grid lines for zones
            for (zone in zones) {
                val zoneBpm = zone.maxBpm.toFloat()
                val yPos = height * (1 - zoneBpm / displayMax)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, yPos),
                    end = Offset(width, yPos),
                    strokeWidth = 1f,
                )
            }

            // Draw the line connecting all points
            for (i in 0 until measurements.size - 1) {
                val x1 = i * xStep
                val y1 = height * (1 - measurements[i].toFloat() / displayMax)
                val x2 = (i + 1) * xStep
                val y2 = height * (1 - measurements[i + 1].toFloat() / displayMax)

                val pointColor =
                    HeartRateUtils.getZoneForBpm(measurements[i], age)?.let { zone ->
                        when (zone.zone) {
                            1 -> Color(0xFF4CAF50)
                            2 -> Color(0xFF8BC34A)
                            3 -> Color(0xFFFFC107)
                            4 -> Color(0xFFFF9800)
                            5 -> Color(0xFFF44336)
                            else -> Color.Blue
                        }
                    } ?: Color.Blue

                drawLine(
                    color = pointColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 2f,
                )
            }

            // Draw points for each measurement
            for (i in measurements.indices) {
                val x = i * xStep
                val y = height * (1 - measurements[i].toFloat() / displayMax)

                val pointColor =
                    HeartRateUtils.getZoneForBpm(measurements[i], age)?.let { zone ->
                        when (zone.zone) {
                            1 -> Color(0xFF4CAF50)
                            2 -> Color(0xFF8BC34A)
                            3 -> Color(0xFFFFC107)
                            4 -> Color(0xFFFF9800)
                            5 -> Color(0xFFF44336)
                            else -> Color.Blue
                        }
                    } ?: Color.Blue

                drawCircle(
                    color = pointColor,
                    radius = 3f,
                    center = Offset(x, y),
                )
            }
        }
    }
}
