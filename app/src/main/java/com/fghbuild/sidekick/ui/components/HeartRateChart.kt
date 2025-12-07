package com.fghbuild.sidekick.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.fghbuild.sidekick.util.HeartRateUtils
import com.fghbuild.sidekick.util.HeartRateZone

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
                .clip(MaterialTheme.shapes.small)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Draw the graph with zones and labels - heart emoji floats over it
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small),
        ) {
            heartRateGraphCanvas(
                measurements = last5Minutes,
                age = age,
                zones = HeartRateUtils.getHeartRateZones(age),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp),
            )

            // Heart emoji floats over graph, below data points
            Text(
                "❤️",
                fontSize = 16.sp,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 4.dp, top = 4.dp)
                        .zIndex(0.5f),
            )
        }
    }
}

@Composable
private fun heartRateGraphCanvas(
    measurements: List<Int>,
    age: Int,
    zones: List<HeartRateZone>,
    modifier: Modifier = Modifier,
) {
    val maxHR = HeartRateUtils.calculateMaxHeartRate(age)
    val textMeasurer = rememberTextMeasurer()

    // Use max HR as display max to ensure all 5 zones are visible
    val displayMin = HeartRateUtils.getGraphDisplayMin(age).toFloat()
    val displayMax = maxHR.toFloat()

    Canvas(
        modifier = modifier,
    ) {
        val width = size.width
        val height = size.height

        // Draw zone background bands - ensure they span full height without gaps
        for (i in zones.indices) {
            val zone = zones[i]
            val zoneTopBpm = zone.maxBpm.toFloat()
            val zoneBottomBpm = if (i == 0) displayMin else zones[i - 1].maxBpm.toFloat()
            val zoneTopY = height * (1 - (zoneTopBpm - displayMin) / (displayMax - displayMin))
            val zoneBottomY = height * (1 - (zoneBottomBpm - displayMin) / (displayMax - displayMin))

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
                color = zoneColor.copy(alpha = 0.25f),
                topLeft = Offset(0f, zoneTopY),
                size =
                    androidx.compose.ui.geometry.Size(
                        width = width,
                        height = zoneBottomY - zoneTopY,
                    ),
            )
        }

        // Draw grid lines and labels for zones
        for (zone in zones) {
            val zoneBpm = zone.maxBpm.toFloat()
            val yPos = height * (1 - (zoneBpm - displayMin) / (displayMax - displayMin))
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, yPos),
                end = Offset(width, yPos),
                strokeWidth = 1f,
            )

            // Draw zone label aligned at the top of the zone
            drawText(
                textMeasurer = textMeasurer,
                text = "${zone.maxBpm}",
                topLeft = Offset(2f + with(density) { 24.dp.toPx() }, yPos - 6f),
                style = androidx.compose.ui.text.TextStyle(fontSize = 8.sp),
            )
        }

        // Draw the line and points only if there's data
        if (measurements.size > 1) {
            val xStep = width / (measurements.size - 1)

            // Draw the line connecting all points
            for (i in 0 until measurements.size - 1) {
                val x1 = i * xStep
                val y1 = (height * (1 - (measurements[i].toFloat() - displayMin) / (displayMax - displayMin))).coerceIn(0f, height)
                val x2 = (i + 1) * xStep
                val y2 = (height * (1 - (measurements[i + 1].toFloat() - displayMin) / (displayMax - displayMin))).coerceIn(0f, height)

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
                val y = (height * (1 - (measurements[i].toFloat() - displayMin) / (displayMax - displayMin))).coerceIn(0f, height)

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
