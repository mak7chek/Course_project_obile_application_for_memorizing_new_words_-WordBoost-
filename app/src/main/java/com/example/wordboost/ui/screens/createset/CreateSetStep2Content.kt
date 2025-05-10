// com.example.wordboost.ui.screens.createset.CreateSetStep2Content.kt
package com.example.wordboost.ui.screens.createset

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.DifficultyLevel // Імпорт моделі
import com.example.wordboost.viewmodel.CreateSetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSetStep2Content(viewModel: CreateSetViewModel, modifier: Modifier = Modifier) {
    val setNameEn by viewModel.setNameEn
    val selectedDifficulty by viewModel.selectedDifficulty
    val isLoadingNameTranslation by viewModel.isLoading

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Введіть англійську назву (або залиште авто-переклад) та виберіть рівень складності.", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = setNameEn,
            onValueChange = { viewModel.onSetNameEnChanged(it) },
            label = { Text("Назва набору (англійською)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoadingNameTranslation,
            trailingIcon = {
                if (isLoadingNameTranslation && viewModel.currentStep.value == 2 && setNameEn.isBlank() && viewModel.setNameUk.value.isNotBlank()) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        )

        Text("Рівень складності:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DifficultyLevel.values().forEach { level ->
                FilterChip(
                    selected = selectedDifficulty == level,
                    onClick = { viewModel.onDifficultyChanged(level) },
                    label = { Text(level.displayName) }
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { viewModel.proceedToStep3() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoadingNameTranslation
        ) {
            Text("Продовжити до додавання слів")
        }
    }
}