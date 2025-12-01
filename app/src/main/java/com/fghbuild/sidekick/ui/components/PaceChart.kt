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
fun paceChart(
    paceHistory: List<Double>,
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
            "Pace Trend",
            fontSize = 16.sp,
        )

        if (paceHistory.isEmpty()) {
            Text(
                "No data yet",
                fontSize = 12.sp,
                color = Color.Gray,
            )
        } else {
            val maxPace = paceHistory.maxOrNull() ?: 0.0
            val minPace = paceHistory.minOrNull() ?: 0.0
            val avgPace =
                if (paceHistory.isNotEmpty()) {
                    paceHistory.average()
                } else {
                    0.0
                }

            Text(
                "Min: ${formatPaceValue(minPace)}  Avg: ${formatPaceValue(avgPace)}  Max: ${formatPaceValue(maxPace)}",
                fontSize = 12.sp,
            )
        }
    }
}

private fun formatPaceValue(pace: Double): String {
    return if (pace.isFinite() && pace > 0) {
        val minutes = pace.toInt()
        val seconds = ((pace - minutes) * 60).toInt()
        "$minutes:${"%02d".format(seconds)}"
    } else {
        "0:00"
    }
}
