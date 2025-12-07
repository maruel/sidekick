package com.fghbuild.sidekick.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fghbuild.sidekick.R
import com.fghbuild.sidekick.data.RoutePoint
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun routeMap(
    routePoints: List<RoutePoint>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (routePoints.isEmpty()) {
                Text(
                    stringResource(R.string.route_no_data),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                routeMapGoogle(
                    routePoints = routePoints,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                )
            }
        }
    }
}

@Composable
private fun routeMapGoogle(
    routePoints: List<RoutePoint>,
    modifier: Modifier = Modifier,
) {
    if (routePoints.size < 2) return

    // Convert RoutePoint to LatLng
    val latLngPoints = routePoints.map { LatLng(it.latitude, it.longitude) }

    // Calculate center of route
    val avgLat = latLngPoints.map { it.latitude }.average()
    val avgLng = latLngPoints.map { it.longitude }.average()
    val centerPoint = LatLng(avgLat, avgLng)

    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(centerPoint, 15f)
        }

    val routeColor = MaterialTheme.colorScheme.primary

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
    ) {
        // Draw polyline for the route
        Polyline(
            points = latLngPoints,
            color = routeColor,
            width = 5f,
        )
    }
}
