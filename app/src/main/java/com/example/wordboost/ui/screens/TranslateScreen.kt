package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.wordboost.translation.*
import com.example.wordboost.data.firebase.FirebaseRepository

@Preview( showBackground =  true)
@Composable
fun TranslateScreen() {
    var input by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }

    // Створюємо залежності для ProxyTranslator:
    // Реальний перекладач. Якщо потрібен API-ключ, додай його в конструктор або налаштування.
    val realTranslator = remember { RealTranslator() }
    // Інстанція репозиторію для роботи з Firebase
    val repository = remember { FirebaseRepository() }
    // ProxyTranslator отримує залежності
    val proxy = remember { ProxyTranslator(realTranslator, repository) }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Введіть слово") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                proxy.translate(input, "EN") { translated ->
                    translation = translated ?: "Немає перекладу"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Перекласти")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Переклад: $translation")
    }
}