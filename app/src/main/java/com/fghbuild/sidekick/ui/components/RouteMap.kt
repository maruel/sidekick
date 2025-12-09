// Displays route on Google Map with polyline, user location marker, and GPS accuracy circle
package com.fghbuild.sidekick.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fghbuild.sidekick.R
import com.fghbuild.sidekick.data.RoutePoint
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun routeMap(
    routePoints: List<RoutePoint>,
    userLocation: LatLng?,
    gpsAccuracyMeters: StateFlow<Float>?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small,
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
            if (routePoints.isEmpty() && userLocation == null) {
                Text(
                    stringResource(R.string.route_no_data),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                routeMapGoogle(
                    routePoints = routePoints,
                    userLocation = userLocation,
                    gpsAccuracyMeters = gpsAccuracyMeters,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                )
            }
        }
    }
}

private fun createBlueDotBitmap(sizePixels: Int): BitmapDescriptor {
    val bitmap = Bitmap.createBitmap(sizePixels, sizePixels, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Draw outer blue circle with transparency
    paint.color = 0x4D2196F3.toInt() // Material blue with alpha
    canvas.drawCircle(sizePixels / 2f, sizePixels / 2f, sizePixels / 2f, paint)

    // Draw inner solid blue circle
    paint.color = 0xFF2196F3.toInt() // Material blue
    canvas.drawCircle(sizePixels / 2f, sizePixels / 2f, sizePixels / 4f, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
private fun routeMapGoogle(
    routePoints: List<RoutePoint>,
    userLocation: LatLng?,
    gpsAccuracyMeters: StateFlow<Float>?,
    modifier: Modifier = Modifier,
) {
    // Convert RoutePoint to LatLng
    val latLngPoints = routePoints.map { LatLng(it.latitude, it.longitude) }

    // Calculate center of route or use user location
    val centerPoint =
        if (latLngPoints.size >= 2) {
            val avgLat = latLngPoints.map { it.latitude }.average()
            val avgLng = latLngPoints.map { it.longitude }.average()
            LatLng(avgLat, avgLng)
        } else if (userLocation != null) {
            userLocation
        } else {
            return
        }

    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(centerPoint, 15f)
        }

    val routeColor = MaterialTheme.colorScheme.primary

    // Collect GPS accuracy if available
    val gpsAccuracy by if (gpsAccuracyMeters != null) {
        gpsAccuracyMeters.collectAsState(initial = 0f)
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
    ) {
        // Draw polyline for the route if available
        if (latLngPoints.size >= 2) {
            Polyline(
                points = latLngPoints,
                color = routeColor,
                width = 5f,
            )
        }

        // Draw user location marker and accuracy circle if available
        userLocation?.let {
            // Draw GPS accuracy radius as a circle
            if (gpsAccuracy > 0f) {
                Circle(
                    center = it,
                    radius = gpsAccuracy.toDouble(),
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    strokeWidth = 1f,
                )
            }

            Marker(
                state = rememberMarkerState(position = it),
                title = "Current Location",
                icon = createBlueDotBitmap(48),
            )
        }
    }
}
