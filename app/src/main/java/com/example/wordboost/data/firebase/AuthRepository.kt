package com.example.wordboost.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log
class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()


    fun registerUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Реєстрація успішна, тепер намагаємось надіслати лист верифікації
                    sendEmailVerification { sendSuccess, sendMsg ->
                        if (sendSuccess) {
                            // Повідомляємо про успішну реєстрацію та надсилання листа
                            onResult(true, "Реєстрація успішна. Будь ласка, підтвердьте ваш email. Лист надіслано.")
                        } else {
                            // Повідомляємо про успішну реєстрацію, але невдале надсилання листа
                            onResult(true, "Реєстрація успішна, але не вдалося надіслати лист верифікації: $sendMsg")
                        }
                    }
                } else {
                    // Реєстрація провалилася, обробляємо помилки
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "Користувач з таким email вже існує."
                        // Можна додати інші типи помилок реєстрації, якщо потрібно
                        else -> task.exception?.localizedMessage ?: "Помилка реєстрації."
                    }
                    onResult(false, errorMessage)
                }
            }
    }
    fun getAuthState(): Flow<FirebaseUser?> = callbackFlow {
        Log.d("AuthRepository", "Starting AuthState listener flow.")
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.d("AuthRepository", "AuthState changed: User ID = ${user?.uid}")
            trySend(user).isSuccess
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose {
            Log.d("AuthRepository", "Stopping AuthState listener flow.")
            auth.removeAuthStateListener(authStateListener)
        }
    }
    fun sendEmailVerification(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user != null && !user.isEmailVerified) {
            user.sendEmailVerification()
                .addOnSuccessListener { onResult(true, "Лист верифікації надіслано.") }
                .addOnFailureListener {
                    val errorMessage = it.localizedMessage ?: "Не вдалося надіслати лист верифікації."
                    onResult(false, errorMessage)
                }
        } else if (user != null && user.isEmailVerified) {
            onResult(false, "Email вже підтверджений.")
        }
        else {
            onResult(false, "Користувач не знайдений.")
        }
    }

    fun isUserVerified(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            user.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Після перезавантаження, перевіряємо статус верифікації
                    onResult(user.isEmailVerified, null)
                } else {
                    // Помилка перезавантаження даних користувача
                    onResult(false, "Не вдалося оновити статус верифікації: ${task.exception?.localizedMessage}")
                }
            }
        } else {
            onResult(false, "Користувач не знайдений.")
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser


    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        user.reload().addOnCompleteListener { reloadTask ->
                            if (reloadTask.isSuccessful) {
                                if (user.isEmailVerified) {
                                    Log.d("AuthRepository", "User ${user.email} logged in and email IS VERIFIED.")
                                    onResult(true, "Успішний вхід.")
                                } else {
                                    Log.d("AuthRepository", "User ${user.email} logged in but email IS NOT VERIFIED.")
                                    onResult(false, "Будь ласка, підтвердьте вашу email адресу перед входом.")
                                }
                            } else {
                                Log.w("AuthRepository", "Failed to reload user data after login: ${reloadTask.exception?.message}")
                                onResult(false, "Помилка входу: не вдалося перевірити статус верифікації email.")
                            }
                        }
                    } else {
                        Log.e("AuthRepository", "Login task successful, but currentUser is null.")
                        onResult(false, "Невідома помилка після входу (користувач null).")
                    }
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> "Невірний email або пароль."
                        is FirebaseAuthInvalidUserException -> "Користувача з таким email не знайдено."
                        else -> task.exception?.localizedMessage ?: "Помилка входу."
                    }
                    Log.w("AuthRepository", "Login failed: $errorMessage")
                    onResult(false, errorMessage)
                }
            }
    }

    fun logout() {
        auth.signOut()
    }
}