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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.fghbuild.sidekick.util.PaceUtils
import com.fghbuild.sidekick.util.PaceZone

@Composable
fun paceChart(
    paceHistory: List<Double>,
    modifier: Modifier = Modifier,
) {
    val last5Minutes = paceHistory.takeLast(300) // 5 minutes at 1 Hz = 300 samples

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Draw the graph with zones and labels - shoe emoji floats over it
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            paceGraphCanvas(
                paceHistory = last5Minutes,
                zones = PaceUtils.getPaceZones(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(bottom = 4.dp),
            )

            // Shoe emoji floats over graph, below data points
            Text(
                "👟",
                fontSize = 16.sp,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 4.dp)
                        .zIndex(0.5f),
            )
        }
    }
}

@Composable
private fun paceGraphCanvas(
    paceHistory: List<Double>,
    zones: List<PaceZone>,
    modifier: Modifier = Modifier,
) {
    val displayMin = PaceUtils.getGraphDisplayMin()
    val displayMax = PaceUtils.getGraphDisplayMax()
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier =
            modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                ),
    ) {
        val width = size.width
        val height = size.height

        // Draw zone background bands - note: inverted Y for pace (slower is higher)
        for (i in zones.indices) {
            val zone = zones[i]
            val zoneMinPace = zone.minPace
            val zoneMaxPace = zone.maxPace

            val zoneMinY = height * (1 - (zoneMinPace - displayMax) / (displayMin - displayMax))
            val zoneMaxY = height * (1 - (zoneMaxPace - displayMax) / (displayMin - displayMax))

            val zoneColor =
                when (zone.zone) {
                    1 -> Color(0xFF4CAF50) // Green - Recovery
                    2 -> Color(0xFF8BC34A) // Light Green - Easy
                    3 -> Color(0xFFFFC107) // Yellow - Moderate
                    4 -> Color(0xFFFF9800) // Orange - Tempo
                    5 -> Color(0xFFF44336) // Red - Fast
                    else -> Color.Gray
                }

            drawRect(
                color = zoneColor.copy(alpha = 0.25f),
                topLeft = Offset(0f, zoneMaxY.toFloat()),
                size =
                    androidx.compose.ui.geometry.Size(
                        width = width.toFloat(),
                        height = (zoneMinY - zoneMaxY).toFloat(),
                    ),
            )
        }

        // Draw grid lines and labels for zones
        for (zone in zones) {
            val zonePace = zone.maxPace
            val yPos = height * (1 - (zonePace - displayMax) / (displayMin - displayMax))
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
                topLeft = Offset(2f, (yPos - 6f).toFloat()),
                style = androidx.compose.ui.text.TextStyle(fontSize = 8.sp),
            )
        }

        // Draw the line and points only if there's data
        if (paceHistory.size > 1) {
            val xStep = width / (paceHistory.size - 1)

            // Draw the line connecting all points
            for (i in 0 until paceHistory.size - 1) {
                val x1 = (i * xStep).toFloat()
                val y1 =
                    (height * (1 - (paceHistory[i] - displayMax) / (displayMin - displayMax)))
                        .toFloat()
                        .coerceIn(0f, height)
                val x2 = ((i + 1) * xStep).toFloat()
                val y2 =
                    (height * (1 - (paceHistory[i + 1] - displayMax) / (displayMin - displayMax)))
                        .toFloat()
                        .coerceIn(0f, height)

                val pointColor =
                    PaceUtils.getZoneForPace(paceHistory[i])?.let { zone ->
                        when (zone.zone) {
                            1 -> Color(0xFF4CAF50)
                            2 -> Color(0xFF8BC34A)
                            3 -> Color(0xFFFFC107)
                            4 -> Color(0xFFFF9800)
                            5 -> Color(0xFFF44336)
                            else -> primaryColor
                        }
                    } ?: primaryColor

                drawLine(
                    color = pointColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 2f,
                )
            }

            // Draw points for each measurement
            for (i in paceHistory.indices) {
                val x = (i * xStep).toFloat()
                val y =
                    (height * (1 - (paceHistory[i] - displayMax) / (displayMin - displayMax)))
                        .toFloat()
                        .coerceIn(0f, height)

                val pointColor =
                    PaceUtils.getZoneForPace(paceHistory[i])?.let { zone ->
                        when (zone.zone) {
                            1 -> Color(0xFF4CAF50)
                            2 -> Color(0xFF8BC34A)
                            3 -> Color(0xFFFFC107)
                            4 -> Color(0xFFFF9800)
                            5 -> Color(0xFFF44336)
                            else -> primaryColor
                        }
                    } ?: primaryColor

                drawCircle(
                    color = pointColor,
                    radius = 3f,
                    center = Offset(x, y),
                )
            }
        }
    }
}
