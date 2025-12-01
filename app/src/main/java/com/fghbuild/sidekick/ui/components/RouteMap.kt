package com.fghbuild.sidekick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                .background(Color(0xFFE0E0E0)),
        contentAlignment = Alignment.Center,
    ) {
        if (routePoints.isEmpty()) {
            Text(
                "No route data yet",
                fontSize = 14.sp,
                color = Color.Gray,
            )
        } else {
            Text(
                "${routePoints.size} points recorded",
                fontSize = 14.sp,
                color = Color.Gray,
            )
        }
    }
}
