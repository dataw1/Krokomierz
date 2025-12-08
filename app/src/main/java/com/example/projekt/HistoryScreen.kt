package com.example.projekt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val historyState by historyViewModel.historyState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (historyState.isLoading) {
            CircularProgressIndicator()
        } else if (historyState.error != null) {
            Text(
                text = "Błąd: ${historyState.error}",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text("Kroki w tym tygodniu", style = MaterialTheme.typography.headlineMedium)
            Text("${historyState.dailyStepsLastWeek.values.sum()}", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Kroki w tym miesiącu", style = MaterialTheme.typography.headlineMedium)
            Text("${historyState.weeklyStepsLastMonth.values.sum()}", style = MaterialTheme.typography.displayLarge)
        }
    }
}
