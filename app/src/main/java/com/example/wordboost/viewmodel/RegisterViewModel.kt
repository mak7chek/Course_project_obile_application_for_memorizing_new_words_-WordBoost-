package com.example.wordboost.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.ui.components.isValidEmail
import com.example.wordboost.ui.components.isValidPassword
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Визначаємо можливі події реєстрації для одноразової обробки UI
sealed class RegistrationEvent {
    object Success : RegistrationEvent()
    data class Failure(val message: String?) : RegistrationEvent()
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // Стан, який UI спостерігатиме
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Одноразові події (наприклад, для навігації або Snackbar)
    private val _registrationEvent = Channel<RegistrationEvent>()
    val registrationEvent = _registrationEvent.receiveAsFlow() // UI буде collectAsState цього Flow

    // Функції, які UI буде викликати у відповідь на дії користувача
    fun setEmail(newEmail: String) {
        _email.value = newEmail
        _message.value = null // Очищаємо повідомлення при зміні полів
    }

    fun setPassword(newPassword: String) {
        _password.value = newPassword
        _message.value = null // Очищаємо повідомлення при зміні полів
    }

    fun registerUser() {
        // Не запускаємо, якщо вже завантажуємо
        if (_isLoading.value) return

        _isLoading.value = true
        _message.value = null // Скидаємо попередні повідомлення

        val currentEmail = _email.value
        val currentPassword = _password.value

        // Валідація перенесена у ViewModel
        if (!isValidEmail(currentEmail)) {
            _message.value = "Невірний формат email"
            _isLoading.value = false
            return
        }
        if (!isValidPassword(currentPassword)) {
            _message.value = "Пароль має містити щонайменше 6 символів"
            _isLoading.value = false
            return
        }

        // Викликаємо репозиторій у coroutine у viewModelScope
        viewModelScope.launch {
            authRepository.registerUser(currentEmail, currentPassword) { success, msg ->
                _isLoading.value = false // Виключаємо індикатор завантаження
                _message.value = msg // Встановлюємо повідомлення (успіх або помилка)
                if (success) {
                    // Надсилаємо подію успіху через Channel
                    viewModelScope.launch { _registrationEvent.send(RegistrationEvent.Success) }
                } else {
                    // Надсилаємо подію помилки через Channel
                    viewModelScope.launch { _registrationEvent.send(RegistrationEvent.Failure(msg)) }
                }
            }
        }
    }

    // Функція для очищення повідомлення після того, як UI його відобразив (опціонально, якщо не використовуєте Snackbar)
    fun clearMessage() {
        _message.value = null
    }
}