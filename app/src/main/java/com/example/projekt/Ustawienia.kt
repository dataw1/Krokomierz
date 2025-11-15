package com.example.projekt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.projekt.ui.theme.ProjektTheme

@Composable
fun SettingsScreen(
    useDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit, // Oczekiwana funkcja zapisu
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Ustawienia", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Tryb Ciemny")

            Switch(
                checked = useDarkTheme,
                onCheckedChange = onThemeToggle // To wywołuje coroutine zapisu w MainActivity!
            )
        }

        // Można dodać inne opcje ustawień tutaj
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ProjektTheme {
        SettingsScreen(useDarkTheme = false, onThemeToggle = {})
    }
}