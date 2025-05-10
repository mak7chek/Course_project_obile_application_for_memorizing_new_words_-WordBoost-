// com.example.wordboost.ui.screens.createset.CreateSetStep4Content.kt
package com.example.wordboost.ui.screens.createset

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wordboost.viewmodel.CreateSetViewModel

@Composable
fun CreateSetStep4Content(
    viewModel: CreateSetViewModel,
    setNameUkDisplay: String,
    onCloseFlow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSetPublic by viewModel.isSetPublic
    val isLoadingSaveSet by viewModel.isLoading
    val currentOperationMessage by viewModel.operationMessage
    val isSuccessfullyCreated = currentOperationMessage?.contains("успішно створено", ignoreCase = true) == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSuccessfullyCreated) {
            Text("Набір '$setNameUkDisplay' успішно створено!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        } else {
            Text("Налаштуйте видимість для набору '$setNameUkDisplay' та збережіть його.", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text("Зробити набір публічним?", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = isSetPublic,
                    onCheckedChange = { viewModel.onVisibilityChanged(it) },
                    enabled = !isLoadingSaveSet && !isSuccessfullyCreated // Блокуємо, якщо вже збережено
                )
            }
            Text(
                if (isSetPublic) "Цей набір буде видно іншим користувачам."
                else "Цей набір буде видно тільки вам у вашій бібліотеці.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isSuccessfullyCreated) {
            Button(
                onClick = { viewModel.resetAllState() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Створити ще один набір")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCloseFlow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Завершити")
            }
        } else {
            Button(
                onClick = { viewModel.saveFullSet() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingSaveSet
            ) {
                Text("Зберегти набір карток")
            }
        }
    }
}