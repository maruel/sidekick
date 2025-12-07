package com.fghbuild.sidekick.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fghbuild.sidekick.R

@Composable
fun gpsAccuracyIndicator(
    accuracyMeters: Float = 0f,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = stringResource(R.string.gps_accuracy_label),
            modifier = Modifier.padding(end = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text =
                if (accuracyMeters > 0) {
                    stringResource(R.string.gps_accuracy_format, accuracyMeters)
                } else {
                    stringResource(R.string.gps_accuracy_waiting)
                },
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
