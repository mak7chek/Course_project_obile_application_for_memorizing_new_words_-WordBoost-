package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.viewmodel.AuthViewModelFactory
import com.example.wordboost.viewmodel.LoginViewModel
import com.example.wordboost.viewmodel.LoginEvent


@Composable
fun LoginScreen(
    authRepo: AuthRepository,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    onNavigateToRegister: () -> Unit // <--- ДОДАЙ ЦЕЙ ПАРАМЕТР
) {
    val viewModel: LoginViewModel = viewModel(factory = AuthViewModelFactory(authRepo))

    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val message by viewModel.message.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showVerifyButton by viewModel.showVerifyButton.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loginEvent.collect { event ->
            when (event) {
                is LoginEvent.Success -> {
                    onSuccess()
                }
                is LoginEvent.Failure -> {

                }
                is LoginEvent.ShowVerificationPrompt -> {
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
            onValueChange = { viewModel.setEmail(it) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.setPassword(it) },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.loginUser() },
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Увійти")
            }
        }


        if (showVerifyButton) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.sendVerificationEmail() },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Надіслати лист повторно")
            }
        }


        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it,
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