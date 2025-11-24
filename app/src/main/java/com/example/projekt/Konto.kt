package com.example.projekt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.projekt.ui.theme.ProjektTheme

@Composable
fun AccountScreen(
    userName: String,
    onUserNameChange: (String) -> Unit,
    currentStepGoal: Int,
    onStepGoalChange: (Int) -> Unit,
    gyroscopeData: GyroscopeData,
    modifier: Modifier = Modifier
) {
    var nameValue by remember(userName) { mutableStateOf(userName) }
    var stepGoalValue by remember(currentStepGoal) { mutableStateOf(currentStepGoal.toString()) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // User Name Section
        Text("Zmień Imię")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = nameValue,
            onValueChange = { nameValue = it },
            label = { Text("Twoje imię") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onUserNameChange(nameValue) },
            enabled = nameValue.isNotBlank()
        ) {
            Text("Zapisz Imię")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Divider()
        Spacer(modifier = Modifier.height(32.dp))

        // Step Goal Section
        Text("Ustaw Cel Kroków")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = stepGoalValue,
            onValueChange = { stepGoalValue = it.filter { char -> char.isDigit() } },
            label = { Text("Dzienny cel kroków") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { stepGoalValue.toIntOrNull()?.let { onStepGoalChange(it) } },
            enabled = stepGoalValue.isNotBlank()
        ) {
            Text("Zapisz Cel")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Divider()
        Spacer(modifier = Modifier.height(32.dp))

        // Gyroscope Section
        Text("Dane z Żyroskopu")
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Oś X: %.2f rad/s".format(gyroscopeData.x))
        Text(text = "Oś Y: %.2f rad/s".format(gyroscopeData.y))
        Text(text = "Oś Z: %.2f rad/s".format(gyroscopeData.z))
    }
}

@Preview(showBackground = true)
@Composable
fun AccountScreenPreview() {
    ProjektTheme {
        AccountScreen(
            userName = "Użytkownik",
            onUserNameChange = {},
            currentStepGoal = 10000, 
            onStepGoalChange = {},
            gyroscopeData = GyroscopeData(0.123f, 0.456f, 0.789f)
        )
    }
}