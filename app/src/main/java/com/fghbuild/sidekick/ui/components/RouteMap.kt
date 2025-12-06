package com.fghbuild.sidekick.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fghbuild.sidekick.data.RoutePoint

@Composable
fun routeMap(
    routePoints: List<RoutePoint>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (routePoints.isEmpty()) {
            Text(
                "No route data yet",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val primaryColor = MaterialTheme.colorScheme.primary
            routeMapCanvas(
                routePoints = routePoints,
                lineColor = primaryColor,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
            )
        }
    }
}

@Composable
private fun routeMapCanvas(
    routePoints: List<RoutePoint>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier =
            modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                ),
    ) {
        if (routePoints.size < 2) return@Canvas

        val width = size.width
        val height = size.height

        // Find bounding box of route
        val minLat = routePoints.minOf { it.latitude }
        val maxLat = routePoints.maxOf { it.latitude }
        val minLon = routePoints.minOf { it.longitude }
        val maxLon = routePoints.maxOf { it.longitude }

        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon

        // Prevent division by zero
        if (latRange == 0.0 || lonRange == 0.0) return@Canvas

        // Add 10% padding
        val padding = 0.05f
        val paddedWidth = width * (1 - 2 * padding)
        val paddedHeight = height * (1 - 2 * padding)

        // Project lat/lon to canvas coordinates
        fun projectPoint(point: RoutePoint): Offset {
            val x =
                (((point.longitude - minLon) / lonRange) * paddedWidth + width * padding).toFloat()
            // Invert Y axis so north is up
            val y =
                (height * (1 - padding) - ((point.latitude - minLat) / latRange) * paddedHeight)
                    .toFloat()
            return Offset(x, y)
        }

        // Draw route line
        for (i in 0 until routePoints.size - 1) {
            val p1 = projectPoint(routePoints[i])
            val p2 = projectPoint(routePoints[i + 1])
            drawLine(
                color = lineColor,
                start = p1,
                end = p2,
                strokeWidth = 2f,
            )
        }

        // Draw start point (green circle)
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = 5f,
            center = projectPoint(routePoints.first()),
        )

        // Draw end point (red circle)
        drawCircle(
            color = Color(0xFFF44336),
            radius = 5f,
            center = projectPoint(routePoints.last()),
        )
    }
}
