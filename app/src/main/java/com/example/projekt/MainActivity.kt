package com.example.projekt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import kotlinx.coroutines.launch
import com.example.projekt.ui.theme.ProjektTheme

// Importy dla Compose Delegating (konieczne, jeśli nie używasz jawnego dostępu .value)
import androidx.compose.runtime.setValue


class MainActivity : ComponentActivity() {

    // Używamy lateinit do bezpiecznej inicjalizacji (Omija problem 'by lazy')
    private lateinit var themePreferenceManager: ThemePreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Menedżer preferencji jest tworzony w onCreate
        // Zakładamy, że masz plik ThemePreferenceManager.kt w tym samym pakiecie
        themePreferenceManager = ThemePreferenceManager(applicationContext)

        enableEdgeToEdge()
        setContent {

            // TRWAŁY ODCZYT: Odczytanie zapisanego stanu motywu
            val isDarkTheme by themePreferenceManager.isDarkTheme.collectAsState(
                initial = isSystemInDarkTheme()
            )

            // UTWORZENIE SCOPE: Potrzebne do wywołania funkcji zapisu DataStore
            val coroutineScope = rememberCoroutineScope()

            // Przekazanie stanu do ProjektTheme
            ProjektTheme(darkTheme = isDarkTheme) {
                ProjektApp(
                    // Przekazujemy odczytany TRWAŁY stan:
                    useDarkTheme = isDarkTheme,

                    // Funkcja do trwałego ZAPISU stanu
                    onThemeToggle = { newThemeState ->
                        coroutineScope.launch {
                            themePreferenceManager.setDarkTheme(newThemeState)
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun ProjektApp(
    useDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    // Używamy jawnego dostępu do stanu (val/val) zamiast delegowania (by),
    // aby uniknąć problemów z getValue/setValue
    val currentDestinationState = rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val currentDestination = currentDestinationState.value

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestinationState.value = it } // Zapis przez .value
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.ACCOUNT -> AccountScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.SETTINGS -> SettingsScreen(
                    useDarkTheme = useDarkTheme,
                    onThemeToggle = onThemeToggle,
                    modifier = Modifier.padding(innerPadding)
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
            onThemeToggle = {}
        )
    }
}