package com.example.projekt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.projekt.ui.theme.ProjektTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var themePreferenceManager: ThemePreferenceManager
    private lateinit var gyroscopeManager: GyroscopeManager
    private val authManager = AuthManager()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            authManager.getCurrentUser()?.uid?.let { startStepCounterService(it) }
        }
    }

    private val postNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            authManager.getCurrentUser()?.uid?.let { checkActivityRecognitionPermission(it) }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
            }
            else -> {
                // No location access granted.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themePreferenceManager = ThemePreferenceManager(applicationContext)
        gyroscopeManager = GyroscopeManager(applicationContext)

        enableEdgeToEdge()
        setContent {
            val isDarkTheme by themePreferenceManager.isDarkTheme.collectAsState(initial = isSystemInDarkTheme())
            val isMetric by themePreferenceManager.isMetric.collectAsState(initial = true)
            val stepGoal by themePreferenceManager.stepGoal.collectAsState(initial = 10000)
            val userName by themePreferenceManager.userName.collectAsState(initial = null)
            val steps by themePreferenceManager.steps.collectAsState(initial = 0)
            var gyroscopeData by remember { mutableStateOf(GyroscopeData(0f, 0f, 0f)) }
            var currentUser by remember { mutableStateOf(authManager.getCurrentUser()) }

            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(currentUser) {
                if (currentUser == null) {
                    themePreferenceManager.setLastUserId(null)
                    stopService(Intent(this@MainActivity, StepCounterService::class.java))
                } else {
                    val userId = currentUser!!.uid
                    val currentUserName = authManager.getUserName(userId)
                    themePreferenceManager.setLastUserId(userId)
                    if (currentUserName != null) {
                        themePreferenceManager.setUserName(currentUserName)
                    }
                    checkAndStartStepCounter(userId)
                    checkLocationPermissions()
                    try {
                        gyroscopeManager.rotationData.collect { gyroscopeData = it }
                    } catch (e: IllegalStateException) {
                        Log.w("MainActivity", "Gyroscope not available on this device.")
                    }
                }
            }

            ProjektTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (currentUser == null) {
                        var showRegisterScreen by remember { mutableStateOf(false) }
                        var authError by remember { mutableStateOf<String?>(null) }

                        if (showRegisterScreen) {
                            RegisterScreen(
                                onRegister = { name, email, password ->
                                    coroutineScope.launch {
                                        val error = authManager.register(name, email, password)
                                        if (error == null) {
                                            currentUser = authManager.getCurrentUser()
                                        } else {
                                            authError = error
                                        }
                                    }
                                },
                                onGoToLogin = { showRegisterScreen = false },
                                error = authError
                            )
                        } else {
                            LoginScreen(
                                onLogin = { email, password ->
                                    coroutineScope.launch {
                                        val error = authManager.login(email, password)
                                        if (error == null) {
                                            currentUser = authManager.getCurrentUser()
                                        } else {
                                            authError = error
                                        }
                                    }
                                },
                                onGoToRegister = { showRegisterScreen = true },
                                error = authError
                            )
                        }
                    } else {
                        ProjektApp(
                            userName = userName ?: "Użytkownik",
                            onUserNameChange = { coroutineScope.launch { themePreferenceManager.setUserName(it) } },
                            useDarkTheme = isDarkTheme,
                            onThemeToggle = { coroutineScope.launch { themePreferenceManager.setDarkTheme(it) } },
                            useMetric = isMetric,
                            onMetricToggle = { coroutineScope.launch { themePreferenceManager.setMetric(it) } },
                            steps = steps,
                            stepGoal = stepGoal,
                            onStepGoalChange = { coroutineScope.launch { themePreferenceManager.setStepGoal(it) } },
                            gyroscopeData = gyroscopeData,
                            onLogout = { coroutineScope.launch { authManager.logout(); currentUser = null } }
                        )
                    }
                }
            }
        }
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun checkAndStartStepCounter(userId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkActivityRecognitionPermission(userId)
            }
        } else {
            checkActivityRecognitionPermission(userId)
        }
    }

    private fun checkActivityRecognitionPermission(userId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                startStepCounterService(userId)
            }
        } else {
            startStepCounterService(userId)
        }
    }

    private fun startStepCounterService(userId: String) {
        val intent = Intent(this, StepCounterService::class.java).apply {
            putExtra("USER_ID", userId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onGoToRegister: () -> Unit,
    error: String?
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isEmailValid by remember { mutableStateOf(true) }

    fun validateEmail() {
        isEmailValid = email.contains("@")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Logowanie", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; validateEmail() },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = !isEmailValid,
            supportingText = { if (!isEmailValid) Text("Niepoprawny format e-mail") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onLogin(email, password) }, enabled = isEmailValid) {
            Text("Zaloguj się")
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onGoToRegister) {
            Text("Nie masz konta? Zarejestruj się")
        }
    }
}

@Composable
fun RegisterScreen(
    onRegister: (String, String, String) -> Unit,
    onGoToLogin: () -> Unit,
    error: String?
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isEmailValid by remember { mutableStateOf(true) }

    fun validateEmail() {
        isEmailValid = email.contains("@")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rejestracja", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Imię") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; validateEmail() },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = !isEmailValid,
            supportingText = { if (!isEmailValid) Text("Niepoprawny format e-mail") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onRegister(name, email, password) }, enabled = isEmailValid) {
            Text("Zarejestruj się")
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onGoToLogin) {
            Text("Masz już konto? Zaloguj się")
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
    onLogout: () -> Unit
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
            AppDestinations.MAP -> MapRouteScreen(
                modifier = Modifier.padding(scaffoldPadding)
            )
            AppDestinations.HISTORY -> HistoryScreen(
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
                onLogout = onLogout,
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
    MAP("Mapa", Icons.Default.Place),
    HISTORY("Historia", Icons.Default.List),
    ACCOUNT("Konto", Icons.Default.AccountBox),
    SETTINGS("Ustawienia", Icons.Default.Settings),
}
