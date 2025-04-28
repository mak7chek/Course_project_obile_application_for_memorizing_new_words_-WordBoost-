package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Імпортуємо функцію viewModel
import com.example.wordboost.data.firebase.AuthRepository // Імпортуємо AuthRepository
// Імпортуємо ViewModel та Factory з нових пакетів
import com.example.wordboost.viewmodel.AuthViewModelFactory
import com.example.wordboost.viewmodel.LoginViewModel
import com.example.wordboost.viewmodel.LoginEvent


@Composable
fun LoginScreen(
    authRepo: AuthRepository, // Отримуємо репозиторій як залежність для Factory
    onSuccess: () -> Unit, // Колбек для навігації після успіху
    onBack: () -> Unit // Колбек для кнопки "Назад"
) {
    // Отримуємо ViewModel за допомогою Factory
    val viewModel: LoginViewModel = viewModel(factory = AuthViewModelFactory(authRepo))

    // Спостерігаємо за станом з ViewModel
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val message by viewModel.message.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showVerifyButton by viewModel.showVerifyButton.collectAsState()

    // Обробляємо одноразові події з ViewModel (наприклад, для навігації)
    LaunchedEffect(Unit) {
        viewModel.loginEvent.collect { event ->
            when (event) {
                is LoginEvent.Success -> {
                    onSuccess() // Викликаємо колбек для навігації
                    // Можна показати Snackbar з повідомленням про успіх тут
                }
                is LoginEvent.Failure -> {
                    // Повідомлення про помилку вже встановлене у _message ViewModel
                    // Можна додатково показати Snackbar, якщо потрібно
                }
                is LoginEvent.ShowVerificationPrompt -> {
                    // Повідомлення та кнопка вже встановлені у відповідних StateFlow
                    // Можна показати Snackbar з додатковою інформацією, якщо потрібно
                }
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Вхід до WordBoost",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.setEmail(it) }, // Передаємо зміни у ViewModel
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.setPassword(it) }, // Передаємо зміни у ViewModel
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.loginUser() }, // Викликаємо функцію входу у ViewModel
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = !isLoading // Кнопка вимкнена під час завантаження
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Увійти")
            }
        }

        // Кнопка "Надіслати лист повторно" тепер контролюється ViewModel
        if (showVerifyButton) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.sendVerificationEmail() }, // Викликаємо функцію ViewModel
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Надіслати лист повторно")
            }
        }

        // Повідомлення контролюється ViewModel
        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it,
                // Колір повідомлення: зелений для успіху/верифікації, червоний для помилки
                color = if (showVerifyButton || it.contains("успішно", ignoreCase = true) || it.contains("надіслано", ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Назад")
        }
    }
}