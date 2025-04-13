package com.example.wordboost.ui.translate

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun TranslateScreen() {
    var input by remember { mutableStateOf(TextFieldValue("")) }

    var translation by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Переклад слів",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Введіть слово") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(

            label = { Text("Переклад слова") },
            modifiler = Modifier.fillMaxSize()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            translation = "Перекладене слово: ${input.text} (псевдо)"
        }) {
            Text("Перекласти")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (translation.isNotEmpty()) {
            Text(
                text = translation,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
