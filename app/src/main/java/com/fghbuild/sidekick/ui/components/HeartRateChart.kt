package com.fghbuild.sidekick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun heartRateChart(
    measurements: List<Int>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Color.LightGray.copy(alpha = 0.1f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Heart Rate",
            fontSize = 16.sp,
        )

        if (measurements.isEmpty()) {
            Text(
                "No data yet",
                fontSize = 12.sp,
                color = Color.Gray,
            )
        } else {
            val maxBpm = measurements.maxOrNull() ?: 0
            val minBpm = measurements.minOrNull() ?: 0
            val avgBpm =
                if (measurements.isNotEmpty()) {
                    (measurements.sum() / measurements.size)
                } else {
                    0
                }

            Text(
                "Min: $minBpm  Avg: $avgBpm  Max: $maxBpm bpm",
                fontSize = 12.sp,
            )
        }
    }
}
