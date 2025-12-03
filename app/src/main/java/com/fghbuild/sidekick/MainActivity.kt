package com.fghbuild.sidekick

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.app.ActivityCompat
import com.fghbuild.sidekick.audio.AnnouncementManager
import com.fghbuild.sidekick.audio.VoiceCommandListener
import com.fghbuild.sidekick.ble.BleManager
import com.fghbuild.sidekick.database.SidekickDatabase
import com.fghbuild.sidekick.location.LocationTracker
import com.fghbuild.sidekick.preferences.DevicePreferences
import com.fghbuild.sidekick.repository.RunRepository
import com.fghbuild.sidekick.run.RunManager
import com.fghbuild.sidekick.run.RunStateManager
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
            sidekickTheme {
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
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    val database = remember { SidekickDatabase.getInstance(context) }
    val runRepository =
        remember {
            RunRepository(database.runDao(), database.routePointDao())
        }
    val devicePreferences = remember { DevicePreferences(context) }

    var onboardingComplete by remember { mutableStateOf(devicePreferences.isOnboardingComplete()) }

    val runManager = remember { RunManager() }
    val bleManager = remember { BleManager(context) }
    val locationTracker = remember { LocationTracker(context) }
    val announcementManager = remember { AnnouncementManager(context) }
    val voiceCommandListener = remember { VoiceCommandListener(context) }
    val runStateManager =
        remember {
            RunStateManager(runManager, announcementManager, voiceCommandListener)
        }

    var runStartTime by remember { mutableStateOf(0L) }

    val runData by runStateManager.runData.collectAsState()
    val heartRateData by bleManager.heartRateData.collectAsState()
    val allRuns by runRepository.getAllRuns().collectAsState(initial = emptyList())
    val routePoints by locationTracker.routePoints.collectAsState()

    LaunchedEffect(runData.isRunning) {
        if (runData.isRunning) {
            locationTracker.currentLocation.filterNotNull().collect { location ->
                runManager.updateLocation(location)
            }
        }
    }

    LaunchedEffect(routePoints) {
        if (runData.isRunning) {
            runManager.updateRoutePoints(routePoints)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runStateManager.cleanup()
            locationTracker.stopTracking()
            bleManager.disconnect()
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
                                contentDescription = it.label,
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it },
                    )
                }
            },
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                when (currentDestination) {
                    AppDestinations.HOME ->
                        homeScreen(
                            modifier = Modifier.padding(innerPadding),
                            isRunning = runData.isRunning,
                            onStartRun = {
                                runStartTime = System.currentTimeMillis()
                                locationTracker.resetRoute()
                                runStateManager.startRun()
                                locationTracker.startTracking()
                                currentDestination = AppDestinations.RUN
                            },
                            onStopRun = {
                                val endTime = System.currentTimeMillis()
                                runStateManager.stopRun()
                                locationTracker.stopTracking()
                                scope.launch {
                                    runRepository.saveRun(
                                        runData = runData,
                                        heartRateData = heartRateData,
                                        startTime = runStartTime,
                                        endTime = endTime,
                                    )
                                }
                                currentDestination = AppDestinations.HOME
                            },
                        )

                    AppDestinations.RUN ->
                        runInProgressScreen(
                            modifier = Modifier.padding(innerPadding),
                            runData = runData,
                            heartRateData = heartRateData,
                            onResume = { runStateManager.resumeRun() },
                            onStop = {
                                val endTime = System.currentTimeMillis()
                                runStateManager.stopRun()
                                locationTracker.stopTracking()
                                scope.launch {
                                    runRepository.saveRun(
                                        runData = runData,
                                        heartRateData = heartRateData,
                                        startTime = runStartTime,
                                        endTime = endTime,
                                    )
                                }
                                currentDestination = AppDestinations.HOME
                            },
                        )

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
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    RUN("Run", Icons.Default.PlayArrow),
    HISTORY("History", Icons.AutoMirrored.Filled.List),
}
