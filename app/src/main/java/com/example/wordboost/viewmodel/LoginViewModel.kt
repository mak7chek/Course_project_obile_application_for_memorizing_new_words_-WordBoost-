package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.ui.components.isValidEmail
import com.example.wordboost.ui.components.isValidPassword
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class LoginEvent {
    object Success : LoginEvent()
    data class Failure(val message: String?) : LoginEvent()
    object ShowVerificationPrompt : LoginEvent()
}


class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

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

    private val _loginEvent = Channel<LoginEvent>()
    val loginEvent = _loginEvent.receiveAsFlow()
    fun setEmail(newEmail: String) {
        _email.value = newEmail
        _message.value = null
        _showVerifyButton.value = false
    }

    fun setPassword(newPassword: String) {
        _password.value = newPassword
        _message.value = null
        _showVerifyButton.value = false
    }

    fun loginUser() {
        if (_isLoading.value) return

        _isLoading.value = true
        _message.value = null
        _showVerifyButton.value = false

        val currentEmail = _email.value
        val currentPassword = _password.value

        // Валідація
        if (!isValidEmail(currentEmail)) {
            _message.value = "Невірний формат email"
            _isLoading.value = false
            return
        }
        if (!isValidPassword(currentPassword)) {
            _message.value = "Невірний пароль"
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            authRepository.loginUser(currentEmail, currentPassword) { success, msg ->
                _isLoading.value = false

                if (success) {
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
        _isLoading.value = true
        _message.value = null
        _showVerifyButton.value = false

        viewModelScope.launch {
            authRepository.sendEmailVerification { success, resultMsg ->
                _isLoading.value = false
                _message.value = resultMsg
                if (success) {
                } else {
                    _showVerifyButton.value = true
                }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}