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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.projekt.ui.theme.ProjektTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var themePreferenceManager: ThemePreferenceManager
    private lateinit var stepCounter: StepCounter
    private lateinit var gyroscopeManager: GyroscopeManager

    private var steps by mutableIntStateOf(0)
    private var gyroscopeData by mutableStateOf(GyroscopeData(0f, 0f, 0f))

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
        gyroscopeManager = GyroscopeManager(applicationContext)

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

        if (gyroscopeManager.isGyroscopeAvailable()) {
            startGyroscope()
        }

        enableEdgeToEdge()
        setContent {
            val isDarkTheme by themePreferenceManager.isDarkTheme.collectAsState(
                initial = isSystemInDarkTheme()
            )
            val isMetric by themePreferenceManager.isMetric.collectAsState(initial = true)
            val stepGoal by themePreferenceManager.stepGoal.collectAsState(initial = 10000)
            val userName by themePreferenceManager.userName.collectAsState(initial = null)

            val coroutineScope = rememberCoroutineScope()

            ProjektTheme(darkTheme = isDarkTheme) {
                if (userName.isNullOrBlank()) {
                    WelcomeScreen { name ->
                        coroutineScope.launch {
                            themePreferenceManager.setUserName(name)
                        }
                    }
                } else {
                    ProjektApp(
                        userName = userName!!,
                        onUserNameChange = { coroutineScope.launch { themePreferenceManager.setUserName(it) } },
                        useDarkTheme = isDarkTheme,
                        onThemeToggle = { coroutineScope.launch { themePreferenceManager.setDarkTheme(it) } },
                        useMetric = isMetric,
                        onMetricToggle = { coroutineScope.launch { themePreferenceManager.setMetric(it) } },
                        steps = steps,
                        stepGoal = stepGoal,
                        onStepGoalChange = { coroutineScope.launch { themePreferenceManager.setStepGoal(it) } },
                        gyroscopeData = gyroscopeData,
                        onResetName = { coroutineScope.launch { themePreferenceManager.setUserName("") } }
                    )
                }
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

    private fun startGyroscope() {
        lifecycleScope.launch {
            gyroscopeManager.rotationData.collect { data ->
                gyroscopeData = data
            }
        }
    }
}

@Composable
fun WelcomeScreen(onNameProvided: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        var name by remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Witaj!", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Podaj swoje imię, abyśmy mogli Cię przywitać.")
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Twoje imię") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onNameProvided(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Zaczynajmy!")
            }
        }
    }
}

@Composable
fun ProjektApp(
    userName: String,
    onUserNameChange: (String) -> Unit,
    useDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    useMetric: Boolean,
    onMetricToggle: (Boolean) -> Unit,
    steps: Int,
    stepGoal: Int,
    onStepGoalChange: (Int) -> Unit,
    gyroscopeData: GyroscopeData,
    onResetName: () -> Unit
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                AppDestinations.entries.forEach { destination ->
                    NavigationBarItem(
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        selected = destination == currentDestination,
                        onClick = { currentDestination = destination }
                    )
                }
            }
        }
    ) { scaffoldPadding ->
        when (currentDestination) {
            AppDestinations.HOME -> HomeScreen(
                userName = userName,
                useMetric = useMetric,
                steps = steps,
                stepGoal = stepGoal,
                modifier = Modifier.padding(scaffoldPadding)
            )
            AppDestinations.ACCOUNT -> AccountScreen(
                userName = userName,
                onUserNameChange = onUserNameChange,
                currentStepGoal = stepGoal,
                onStepGoalChange = onStepGoalChange,
                gyroscopeData = gyroscopeData,
                modifier = Modifier.padding(scaffoldPadding)
            )
            AppDestinations.SETTINGS -> SettingsScreen(
                useDarkTheme = useDarkTheme,
                onThemeToggle = onThemeToggle,
                useMetric = useMetric,
                onMetricToggle = onMetricToggle,
                onResetName = onResetName,
                modifier = Modifier.padding(scaffoldPadding)
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Główna", Icons.Default.Home),
    ACCOUNT("Konto", Icons.Default.AccountBox),
    SETTINGS("Ustawienia", Icons.Default.Settings),
}

@Preview(showBackground = true)
@Composable
fun ProjektAppPreview() {
    ProjektTheme {
        ProjektApp(
            userName = "Użytkownik",
            onUserNameChange = {},
            useDarkTheme = false,
            onThemeToggle = {},
            useMetric = true,
            onMetricToggle = {},
            steps = 12345,
            stepGoal = 10000,
            onStepGoalChange = {},
            gyroscopeData = GyroscopeData(0.1f, 0.2f, 0.3f),
            onResetName = {}
        )
    }
}
