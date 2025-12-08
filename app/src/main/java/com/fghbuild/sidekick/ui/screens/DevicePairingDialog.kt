package com.fghbuild.sidekick.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fghbuild.sidekick.R
import com.fghbuild.sidekick.data.HrmDevice

@Composable
private fun deviceListItem(
    device: HrmDevice,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (device.rssi != 0) {
                    Text(
                        text = stringResource(R.string.device_signal) + "${device.rssi} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onSelect,
                modifier = Modifier.padding(start = 4.dp),
            ) { Text(stringResource(R.string.device_connect)) }
        }
    }
}

@Composable
fun devicePairingDialog(
    modifier: Modifier = Modifier,
    discoveredDevices: List<HrmDevice>,
    connectedDevice: HrmDevice?,
    isScanning: Boolean,
    onStartScanning: () -> Unit,
    onStopScanning: () -> Unit,
    onSelectDevice: (HrmDevice) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) { onStartScanning() }

    Dialog(
        onDismissRequest = {
            onStopScanning()
            onDismiss()
        },
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
            ),
    ) {
        Card(
            modifier = modifier.fillMaxWidth(0.9f).padding(4.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.device_heart_rate_monitor),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    IconButton(
                        onClick = {
                            onStopScanning()
                            onDismiss()
                        },
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.content_description_close),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Connected device section
                if (connectedDevice != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = stringResource(R.string.device_connected),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text(
                                        text = stringResource(R.string.device_connected),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = connectedDevice.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = connectedDevice.address,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Button(
                                onClick = onDisconnect,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                            ) { Text(stringResource(R.string.device_disconnect)) }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    // Scanning section
                    Text(
                        text = stringResource(R.string.device_available_devices),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Scanning indicator
                    if (isScanning) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(0.dp),
                                )
                                Text(stringResource(R.string.device_scanning))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Devices list
                    if (discoveredDevices.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text =
                                    if (isScanning) {
                                        stringResource(R.string.device_searching)
                                    } else {
                                        stringResource(R.string.device_no_devices_found)
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = discoveredDevices,
                                key = { it.address },
                            ) { device ->
                                deviceListItem(
                                    device = device,
                                    onSelect = { onSelectDevice(device) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
