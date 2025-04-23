package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.ui.components.isValidEmail
import com.example.wordboost.ui.components.isValidPassword

@Composable
fun RegisterScreen(authRepo: AuthRepository = AuthRepository(), onRegistrationSuccess: () -> Unit, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) } // Стан для індикатора завантаження


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center, // Вирівнюємо по центру вертикально
        horizontalAlignment = Alignment.CenterHorizontally // Вирівнюємо по центру горизонтально
    ) {
        Text(
            "Реєстрація у WordBoost",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(), // Приховуємо пароль
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (isLoading) return@Button // Забороняємо повторне натискання під час завантаження
                isLoading = true // Включаємо індикатор завантаження
                message = null // Скидаємо попередні повідомлення

                if (!isValidEmail(email)) {
                    message = "Невірний формат email"
                    isLoading = false
                    return@Button
                }
                if (!isValidPassword(password)) {
                    message = "Пароль має містити щонайменше 6 символів"
                    isLoading = false
                    return@Button
                }

                authRepo.registerUser(email, password) { success, msg ->
                    isLoading = false // Виключаємо індикатор завантаження
                    message = msg
                    if (success) {
                        // Після успішної реєстрації викликаємо callback
                        onRegistrationSuccess()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = !isLoading // Вимикаємо кнопку під час завантаження
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary) // Маленький індикатор в кнопці
            } else {
                Text("Зареєструватись")
            }
        }


        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it,
                color = if (it.contains("успішно")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, // Колір повідомлення
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Кнопка назад
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Назад")
        }
    }
}