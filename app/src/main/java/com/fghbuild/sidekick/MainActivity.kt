package com.fghbuild.sidekick

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.app.ActivityCompat
import com.fghbuild.sidekick.audio.AnnouncementManager
import com.fghbuild.sidekick.audio.VoiceCommandListener
import com.fghbuild.sidekick.ble.BleManager
import com.fghbuild.sidekick.data.HrmDevice
import com.fghbuild.sidekick.database.SidekickDatabase
import com.fghbuild.sidekick.location.LocationTracker
import com.fghbuild.sidekick.preferences.DevicePreferences
import com.fghbuild.sidekick.repository.RunRepository
import com.fghbuild.sidekick.run.RunManager
import com.fghbuild.sidekick.run.RunStateManager
import com.fghbuild.sidekick.service.RunTrackingService
import com.fghbuild.sidekick.ui.screens.historyScreen
import com.fghbuild.sidekick.ui.screens.homeScreen
import com.fghbuild.sidekick.ui.screens.onboardingScreen
import com.fghbuild.sidekick.ui.screens.runInProgressScreen
import com.fghbuild.sidekick.ui.theme.sidekickTheme
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE,
            ),
            1,
        )

        enableEdgeToEdge()
        setContent {
            sidekickTheme(
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = true,
            ) {
                sidekickApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun sidekickApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.RUN) }
    var trackingService by remember { mutableStateOf<RunTrackingService?>(null) }

    val database = remember { SidekickDatabase.getInstance(context) }
    val runRepository =
        remember {
            RunRepository(database.runDao(), database.routePointDao())
        }
    val devicePreferences = remember { DevicePreferences(context) }

    var onboardingComplete by remember {
        mutableStateOf(
            devicePreferences.isOnboardingComplete(),
        )
    }

    // Create managers but they're only used after onboarding completes
    val gpsMeasurementDao = remember { database.gpsMeasurementDao() }
    val gpsCalibrationDao = remember { database.gpsCalibrationDao() }

    val runManager = remember { RunManager(gpsMeasurementDao, gpsCalibrationDao) }
    val bleManager =
        remember {
            BleManager(context)
        }
    val locationTracker =
        remember {
            LocationTracker(context, gpsMeasurementDao)
        }
    val announcementManager = remember { AnnouncementManager(context) }
    val voiceCommandListener = remember { VoiceCommandListener(context) }
    val runStateManager =
        remember {
            RunStateManager(runManager, announcementManager, voiceCommandListener, bleManager.heartRateData)
        }

    var runStartTime by remember { mutableStateOf(0L) }

    val runData by runStateManager.runData.collectAsState()
    val heartRateData by bleManager.heartRateData.collectAsState()
    val allRuns by runRepository.getAllRuns().collectAsState(initial = emptyList())
    val routePoints by locationTracker.routePoints.collectAsState()
    val discoveredDevices by bleManager.discoveredDevices.collectAsState()
    val connectedDevice by bleManager.connectedDevice.collectAsState()
    val isScanning by bleManager.isScanning.collectAsState()

    // Bind to tracking service
    DisposableEffect(Unit) {
        val serviceConnection =
            object : ServiceConnection {
                override fun onServiceConnected(
                    name: ComponentName?,
                    service: IBinder?,
                ) {
                    if (service is RunTrackingService.LocalBinder) {
                        trackingService = service.getService()
                        // If run is already active, restore the run state
                        if (service.getService().isRunActive() && !runData.isRunning) {
                            runStateManager.resumeRun()
                        }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    trackingService = null
                }
            }

        val intent = Intent(context, RunTrackingService::class.java)
        context.bindService(intent, serviceConnection, ComponentActivity.BIND_AUTO_CREATE)

        onDispose {
            context.unbindService(serviceConnection)
        }
    }

    LaunchedEffect(runData.isRunning || runData.isPaused) {
        if (runData.isRunning || runData.isPaused) {
            // Start foreground service to keep app alive
            val serviceIntent =
                Intent(context, RunTrackingService::class.java).apply {
                    action = RunTrackingService.ACTION_START_RUN
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            // Stop foreground service when run ends
            val stopIntent =
                Intent(context, RunTrackingService::class.java).apply {
                    action = RunTrackingService.ACTION_STOP_RUN
                }
            context.startService(stopIntent)
        }
    }

    LaunchedEffect(Unit) {
        // Auto-reconnect to last HRM device on app startup
        val lastDeviceAddress = devicePreferences.getLastHrmDeviceAddress()
        val lastDeviceName = devicePreferences.getLastHrmDeviceName()
        if (!lastDeviceAddress.isNullOrEmpty() && !lastDeviceName.isNullOrEmpty()) {
            val lastDevice = HrmDevice(address = lastDeviceAddress, name = lastDeviceName, rssi = 0)
            bleManager.connectToDevice(lastDevice)
        }
    }

    // Start GPS measurement collection at app startup (pre-warmup)
    LaunchedEffect(Unit) {
        locationTracker.startTracking(runId = null)
    }

    LaunchedEffect(runData.isRunning) {
        if (runData.isRunning) {
            locationTracker.currentLocation.filterNotNull().collect { location ->
                runManager.updateLocation(location)
            }
        }
    }

    LaunchedEffect(runData) {
        if (runData.isRunning || runData.isPaused) {
            // Update notification with current run stats
            val durationSeconds = runData.durationMillis / 1000
            val distanceKm = runData.distanceMeters / 1000.0
            trackingService?.updateNotification(
                distanceKm = distanceKm,
                paceMinPerKm = runData.paceMinPerKm,
                durationSeconds = durationSeconds,
                currentBpm = heartRateData.currentBpm,
            )
        }
    }

    LaunchedEffect(routePoints) {
        if (runData.isRunning) {
            runManager.updateRoutePoints(routePoints)
        }
    }

    LaunchedEffect(runData) {
        if (runData.isRunning) {
            runStateManager.update()
        }
    }

    LaunchedEffect(Unit) {
        // Flush measurements periodically (both during runs and pre-warmup)
        while (true) {
            kotlinx.coroutines.delay(10000) // Flush every 10 seconds
            locationTracker.flushPendingMeasurements()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runStateManager.cleanup()
            locationTracker.stopTracking()
            bleManager.disconnect()
            // Stop foreground service on app destroy
            val stopIntent =
                Intent(context, RunTrackingService::class.java).apply {
                    action = RunTrackingService.ACTION_STOP_RUN
                }
            context.startService(stopIntent)
        }
    }

    if (!onboardingComplete) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            onboardingScreen(
                modifier = Modifier.padding(innerPadding),
                onBirthYearSubmit = { birthYear ->
                    devicePreferences.saveBirthYear(birthYear)
                    onboardingComplete = true
                },
            )
        }
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        icon = {
                            Icon(
                                it.icon,
                                contentDescription = stringResource(it.labelResId),
                            )
                        },
                        label = { Text(stringResource(it.labelResId)) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it },
                    )
                }
            },
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                when (currentDestination) {
                    AppDestinations.RUN ->
                        if (runData.isRunning || runData.isPaused) {
                            val userAge = devicePreferences.getCurrentAge()
                            runInProgressScreen(
                                modifier = Modifier.padding(innerPadding),
                                runData = runData,
                                heartRateData = heartRateData,
                                onPause = { runStateManager.pauseRun() },
                                onResume = { runStateManager.resumeRun() },
                                onStop = {
                                    val endTime = System.currentTimeMillis()
                                    runStateManager.stopRun()
                                    locationTracker.stopTracking()
                                    scope.launch {
                                        locationTracker.flushPendingMeasurements()
                                        val runId =
                                            runRepository.saveRun(
                                                runData = runData,
                                                heartRateData = heartRateData,
                                                startTime = runStartTime,
                                                endTime = endTime,
                                            )
                                        runManager.finalizeRunSession()
                                    }
                                },
                                connectedDevice = connectedDevice,
                                userAge = userAge,
                                gpsAccuracyMeters = locationTracker.currentAccuracyMeters,
                                currentLocation = locationTracker.currentLocation,
                                discoveredDevices = discoveredDevices,
                                isScanning = isScanning,
                                onStartScanning = { bleManager.startScanning() },
                                onStopScanning = { bleManager.stopScanning() },
                                onSelectDevice = { device ->
                                    bleManager.connectToDevice(device)
                                    devicePreferences.saveLastHrmDevice(device.address, device.name)
                                },
                                onDisconnect = { bleManager.disconnect() },
                            )
                        } else {
                            val userAge = devicePreferences.getCurrentAge()
                            homeScreen(
                                modifier = Modifier.padding(innerPadding),
                                onStartRun = {
                                    runStartTime = System.currentTimeMillis()
                                    locationTracker.resetRoute()
                                    runStateManager.startRun()
                                    scope.launch {
                                        val runId =
                                            runRepository.createRun(
                                                com.fghbuild.sidekick.database.RunEntity(
                                                    startTime = runStartTime,
                                                    endTime = runStartTime,
                                                    distanceMeters = 0.0,
                                                    durationMillis = 0L,
                                                    averagePaceMinPerKm = 0.0,
                                                ),
                                            )
                                        locationTracker.startTracking(runId)
                                        runManager.initializeRunSession(runId, "running")
                                    }
                                },
                                runData = runData,
                                heartRateData = heartRateData,
                                connectedDevice = connectedDevice,
                                userAge = userAge,
                                discoveredDevices = discoveredDevices,
                                isScanning = isScanning,
                                onStartScanning = { bleManager.startScanning() },
                                onStopScanning = { bleManager.stopScanning() },
                                onSelectDevice = { device ->
                                    bleManager.connectToDevice(device)
                                    devicePreferences.saveLastHrmDevice(device.address, device.name)
                                },
                                onDisconnect = { bleManager.disconnect() },
                                gpsAccuracyMeters = locationTracker.currentAccuracyMeters,
                                currentLocation = locationTracker.currentLocation,
                            )
                        }

                    AppDestinations.HISTORY ->
                        historyScreen(
                            modifier = Modifier.padding(innerPadding),
                            runs = allRuns,
                            onDeleteRun = { runId ->
                                scope.launch {
                                    runRepository.deleteRun(runId)
                                }
                            },
                        )
                }
            }
        }
    }
}

enum class AppDestinations(
    val labelResId: Int,
    val icon: ImageVector,
) {
    RUN(R.string.nav_run, Icons.Default.PlayArrow),
    HISTORY(R.string.nav_history, Icons.AutoMirrored.Filled.List),
}
