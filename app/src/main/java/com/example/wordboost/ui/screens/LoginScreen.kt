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
fun LoginScreen(authRepo: AuthRepository = AuthRepository(), onSuccess: () -> Unit, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var showVerifyButton by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // Стан для індикатора завантаження


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center, // Вирівнюємо по центру вертикально
        horizontalAlignment = Alignment.CenterHorizontally // Вирівнюємо по центру горизонтально
    ) {
        Text(
            "Вхід до WordBoost",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(0.9f) // Поле займає 90% ширини
        )
        Spacer(Modifier.height(16.dp)) // Збільшили відступ

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(), // Приховуємо пароль
            modifier = Modifier.fillMaxWidth(0.9f) // Поле займає 90% ширини
        )
        Spacer(Modifier.height(24.dp)) // Збільшили відступ

        Button(
            onClick = {
                if (isLoading) return@Button // Забороняємо повторне натискання під час завантаження
                isLoading = true // Включаємо індикатор завантаження
                message = null // Скидаємо попередні повідомлення
                showVerifyButton = false

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

                authRepo.loginUser(email, password) { success, msg ->
                    isLoading = false // Виключаємо індикатор завантаження
                    if (success) {
                        onSuccess() // Переходимо далі
                    } else {
                        message = msg
                        showVerifyButton = msg?.contains("email адресу") == true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = !isLoading // Вимикаємо кнопку під час завантаження
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary) // Маленький індикатор в кнопці
            } else {
                Text("Увійти")
            }
        }

        if (showVerifyButton) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    authRepo.sendEmailVerification { success, resultMsg ->
                        message = resultMsg
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Надіслати лист повторно")
            }
        }

        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it,
                color = if (showVerifyButton) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, // Колір повідомлення залежить від типу
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