package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


@Composable
fun AuthenticatedMainScreen(
    onTranslateClick: () -> Unit,
    onPracticeClick: () -> Unit,
    onWordListClick: () -> Unit, // <-- Доданий колбек
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Привіт у WordBoost!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onTranslateClick,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Перекласти слово")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPracticeClick,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Практика")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onWordListClick,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Мій Словник")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLogoutClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Вийти")
        }
    }
}