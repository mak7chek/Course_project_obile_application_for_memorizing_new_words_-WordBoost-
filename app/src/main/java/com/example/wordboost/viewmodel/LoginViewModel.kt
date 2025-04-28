package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.ui.components.isValidEmail // Імпортуйте ваші функції валідації
import com.example.wordboost.ui.components.isValidPassword // Імпортуйте ваші функції валідації
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Визначаємо можливі події входу для одноразової обробки UI
sealed class LoginEvent {
    object Success : LoginEvent()
    data class Failure(val message: String?) : LoginEvent()
    object ShowVerificationPrompt : LoginEvent() // Подія для показу кнопки верифікації
}


class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // Стан, який UI спостерігатиме
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _showVerifyButton = MutableStateFlow(false)
    val showVerifyButton: StateFlow<Boolean> = _showVerifyButton.asStateFlow()

    // Одноразові події (наприклад, для навігації або Snackbar/Prompt)
    private val _loginEvent = Channel<LoginEvent>()
    val loginEvent = _loginEvent.receiveAsFlow() // UI буде collectAsState цього Flow

    // Функції, які UI буде викликати
    fun setEmail(newEmail: String) {
        _email.value = newEmail
        _message.value = null // Очищаємо повідомлення при зміні полів
        _showVerifyButton.value = false // Приховуємо кнопку верифікації при зміні полів
    }

    fun setPassword(newPassword: String) {
        _password.value = newPassword
        _message.value = null // Очищаємо повідомлення при зміні полів
        _showVerifyButton.value = false // Приховуємо кнопку верифікації при зміні полів
    }

    fun loginUser() {
        if (_isLoading.value) return

        _isLoading.value = true
        _message.value = null
        _showVerifyButton.value = false // Приховуємо кнопку при спробі входу

        val currentEmail = _email.value
        val currentPassword = _password.value

        // Валідація
        if (!isValidEmail(currentEmail)) {
            _message.value = "Невірний формат email"
            _isLoading.value = false
            return
        }
        if (!isValidPassword(currentPassword)) {
            _message.value = "Невірний пароль" // Або інше повідомлення, якщо валідація пароля тут інша
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            authRepository.loginUser(currentEmail, currentPassword) { success, msg ->
                _isLoading.value = false

                if (success) {
                    // Перевірка, чи користувач верифікований після успішного входу
                    val currentUser = authRepository.getCurrentUser()
                    if (currentUser != null && currentUser.isEmailVerified) {
                        viewModelScope.launch { _loginEvent.send(LoginEvent.Success) }
                    } else {
                        _message.value = "Будь ласка, верифікуйте вашу email адресу."
                        _showVerifyButton.value = true
                        viewModelScope.launch { _loginEvent.send(LoginEvent.ShowVerificationPrompt) }
                    }
                } else {
                    _message.value = msg
                    // Якщо повідомлення про помилку вказує на не верифікований email
                    if (msg?.contains("email адресу") == true) {
                        _showVerifyButton.value = true
                        viewModelScope.launch { _loginEvent.send(LoginEvent.ShowVerificationPrompt) }
                    } else {
                        viewModelScope.launch { _loginEvent.send(LoginEvent.Failure(msg)) }
                    }
                }
            }
        }
    }

    fun sendVerificationEmail() {
        _isLoading.value = true // Можливо, варто показати індикатор під час відправки листа
        _message.value = null
        _showVerifyButton.value = false // Приховуємо кнопку після натискання

        viewModelScope.launch {
            authRepository.sendEmailVerification { success, resultMsg ->
                _isLoading.value = false
                _message.value = resultMsg // Повідомлення про результат відправки
                if (success) {
                    // Лист надіслано
                } else {
                    // Помилка відправки листа
                    _showVerifyButton.value = true // Можливо, показати кнопку знову, якщо сталася помилка
                }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}