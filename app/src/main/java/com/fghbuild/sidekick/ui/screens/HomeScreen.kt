package com.fghbuild.sidekick.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun homeScreen(
    modifier: Modifier = Modifier,
    isRunning: Boolean = false,
    onStartRun: () -> Unit = {},
    onStopRun: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sidekick",
            fontSize = 32.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isRunning) "Run in progress..." else "Ready to run!",
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (isRunning) {
            Button(
                onClick = onStopRun,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("Stop Run")
            }
        } else {
            Button(onClick = onStartRun) {
                Text("Start Run")
            }
        }
    }
}
