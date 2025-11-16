package com.example.projekt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.projekt.ui.theme.ProjektTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var themePreferenceManager: ThemePreferenceManager
    private lateinit var stepCounter: StepCounter

    private var steps by mutableIntStateOf(0)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startStepCounter()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themePreferenceManager = ThemePreferenceManager(applicationContext)
        stepCounter = StepCounter(applicationContext)

        if (stepCounter.isSensorAvailable()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    startStepCounter()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            } else {
                startStepCounter()
            }
        }

        enableEdgeToEdge()
        setContent {
            val isDarkTheme by themePreferenceManager.isDarkTheme.collectAsState(
                initial = isSystemInDarkTheme()
            )
            val isMetric by themePreferenceManager.isMetric.collectAsState(initial = true)
            val stepGoal by themePreferenceManager.stepGoal.collectAsState(initial = 10000)

            val coroutineScope = rememberCoroutineScope()

            ProjektTheme(darkTheme = isDarkTheme) {
                ProjektApp(
                    useDarkTheme = isDarkTheme,
                    onThemeToggle = { coroutineScope.launch { themePreferenceManager.setDarkTheme(it) } },
                    useMetric = isMetric,
                    onMetricToggle = { coroutineScope.launch { themePreferenceManager.setMetric(it) } },
                    steps = steps,
                    stepGoal = stepGoal,
                    onStepGoalChange = { coroutineScope.launch { themePreferenceManager.setStepGoal(it) } }
                )
            }
        }
    }

    private fun startStepCounter() {
        lifecycleScope.launch {
            stepCounter.steps.collect { sessionSteps ->
                steps = sessionSteps
            }
        }
    }
}

@Composable
fun ProjektApp(
    useDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    useMetric: Boolean,
    onMetricToggle: (Boolean) -> Unit,
    steps: Int,
    stepGoal: Int,
    onStepGoalChange: (Int) -> Unit
) {
    val currentDestinationState = rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val currentDestination = currentDestinationState.value

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            for (destination in AppDestinations.entries) {
                item(
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestinationState.value = destination }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { scaffoldPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    useMetric = useMetric,
                    steps = steps,
                    stepGoal = stepGoal,
                    modifier = Modifier.padding(scaffoldPadding)
                )
                AppDestinations.ACCOUNT -> AccountScreen(
                    currentStepGoal = stepGoal,
                    onStepGoalChange = onStepGoalChange,
                    modifier = Modifier.padding(scaffoldPadding)
                )
                AppDestinations.SETTINGS -> SettingsScreen(
                    useDarkTheme = useDarkTheme,
                    onThemeToggle = onThemeToggle,
                    useMetric = useMetric,
                    onMetricToggle = onMetricToggle,
                    modifier = Modifier.padding(scaffoldPadding)
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    ACCOUNT("Konto", Icons.Default.AccountBox),
    SETTINGS("Ustawienia", Icons.Default.Settings),
}

@PreviewScreenSizes
@Composable
fun ProjektAppPreview() {
    ProjektTheme {
        ProjektApp(
            useDarkTheme = false,
            onThemeToggle = {},
            useMetric = true,
            onMetricToggle = {},
            steps = 12345,
            stepGoal = 10000,
            onStepGoalChange = {}
        )
    }
}