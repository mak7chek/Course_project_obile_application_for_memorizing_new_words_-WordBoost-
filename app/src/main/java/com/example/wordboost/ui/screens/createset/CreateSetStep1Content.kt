// com.example.wordboost.ui.screens.createset.CreateSetStep1Content.kt
package com.example.wordboost.ui.screens.createset // Оновлений пакет

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wordboost.viewmodel.CreateSetViewModel // Правильний шлях до ViewModel

@Composable
fun CreateSetStep1Content(viewModel: CreateSetViewModel, modifier: Modifier = Modifier) {
    val setNameUk by viewModel.setNameUk
    val setNameUkError by viewModel.setNameUkError

    Column(
        modifier = modifier // Цей Modifier має .weight(1f) від CreateSetScreen
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Введіть назву для вашого нового набору карток українською мовою.", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = setNameUk,
            onValueChange = { viewModel.onSetNameUkChanged(it) },
            label = { Text("Назва набору (українською)") },
            isError = setNameUkError != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (setNameUkError != null) {
            Text(text = setNameUkError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { viewModel.proceedToStep2() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading.value
        ) {
            Text("Продовжити")
        }
    }
}