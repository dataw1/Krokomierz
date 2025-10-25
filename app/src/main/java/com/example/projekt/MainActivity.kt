package com.example.projekt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme // Upewnij się, że ten import jest
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.projekt.Konto.AccountScreen
import com.example.projekt.home.HomeScreen
import com.example.projekt.Ustawienia.SettingsScreen
import com.example.projekt.ui.theme.ProjektTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 1. Stan motywu "wyniesiony" (hoisted) na najwyższy poziom
            val systemIsDark = isSystemInDarkTheme()
            var useDarkTheme by rememberSaveable { mutableStateOf(systemIsDark) }

            // 2. Przekazanie stanu do ProjektTheme, aby zastosować motyw
            ProjektTheme(darkTheme = useDarkTheme) {
                ProjektApp(
                    // 3. Przekazanie stanu i funkcji do jego zmiany do aplikacji
                    useDarkTheme = useDarkTheme,
                    onThemeToggle = { useDarkTheme = it }
                )
            }
        }
    }
}

@Composable
fun ProjektApp(
    useDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit // Funkcja do zmiany motywu
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

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
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // 4. Wyświetlanie innego ekranu w zależności od wybranej nawigacji
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
