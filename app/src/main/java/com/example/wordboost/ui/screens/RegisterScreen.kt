package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.firebase.AuthRepository
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*

import com.example.wordboost.ui.components.isValidEmail
import com.example.wordboost.ui.components.isValidPassword

@Composable
fun RegisterScreen(authRepo: AuthRepository = AuthRepository()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Пароль") })

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (!isValidEmail(email)) {
                message = "Невірний формат email"
                return@Button
            }
            if (!isValidPassword(password)) {
                message = "Пароль має містити щонайменше 6 символів"
                return@Button
            }

            authRepo.registerUser(email, password) { success, msg ->
                message = msg
            }
        }) {
            Text("Зареєструватись")
        }


        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(it)
        }
    }
}