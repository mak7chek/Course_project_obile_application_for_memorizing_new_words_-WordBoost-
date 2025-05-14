package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.ui.components.isValidEmail
import com.example.wordboost.ui.components.isValidPassword
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class RegistrationEvent {
    object Success : RegistrationEvent()
    data class Failure(val message: String?) : RegistrationEvent()
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _registrationEvent = Channel<RegistrationEvent>()
    val registrationEvent = _registrationEvent.receiveAsFlow()

    fun setEmail(newEmail: String) {
        _email.value = newEmail
        _message.value = null
    }

    fun setPassword(newPassword: String) {
        _password.value = newPassword
        _message.value = null
    }

    fun registerUser() {
        if (_isLoading.value) return

        _isLoading.value = true
        _message.value = null

        val currentEmail = _email.value
        val currentPassword = _password.value
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

        viewModelScope.launch {
            authRepository.registerUser(currentEmail, currentPassword) { success, msg ->
                _isLoading.value = false
                _message.value = msg
                if (success) {
                    viewModelScope.launch { _registrationEvent.send(RegistrationEvent.Success) }
                } else {
                    viewModelScope.launch { _registrationEvent.send(RegistrationEvent.Failure(msg)) }
                }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}