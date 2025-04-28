package com.example.wordboost.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    /**
     * Реєструє нового користувача за допомогою email та пароля.
     * Після успішної реєстрації автоматично намагається надіслати лист верифікації.
     *
     * @param email Email користувача.
     * @param password Пароль користувача.
     * @param onResult Колбек, що повертає результат реєстрації: Boolean (успіх/невдача) та String (повідомлення).
     */
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

    /**
     * Надсилає лист верифікації на email поточного користувача.
     *
     * @param onResult Колбек, що повертає результат надсилання: Boolean (успіх/невдача) та String (повідомлення).
     */
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

    /**
     * Перевіряє, чи поточний користувач верифікував свій email.
     * Виконує перезавантаження даних користувача для отримання актуального стану.
     *
     * @param onResult Колбек, що повертає Boolean (true, якщо верифіковано) та можливе повідомлення про помилку/стан.
     */
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

    /**
     * Повертає поточного аутентифікованого користувача Firebase.
     * Повертає null, якщо користувач не увійшов.
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Виконує вхід користувача за допомогою email та пароля.
     * Перевіряє статус верифікації email після успішного входу.
     *
     * @param email Email користувача.
     * @param password Пароль користувача.
     * @param onResult Колбек, що повертає результат входу: Boolean (успіх/невдача) та String (повідомлення).
     */
    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // Перевіряємо верифікацію email одразу після входу
                    if (user != null && user.isEmailVerified) {
                        onResult(true, "Успішний вхід.")
                    } else if (user != null && !user.isEmailVerified) {
                        // Вхід успішний, але email не верифіковано
                        onResult(false, "Будь ласка, підтвердьте вашу email адресу перед входом.")
                        // Можна запропонувати повторне надсилання листа верифікації
                        // або зробити це автоматично (якщо ви вважаєте це доцільним в UI)
                    } else {
                        // Цей випадок малоймовірний після task.isSuccessful, але для повноти
                        onResult(false, "Невідома помилка після входу.")
                    }
                } else {
                    // Вхід провалився, обробляємо помилки
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> "Невірний email або пароль."
                        is FirebaseAuthInvalidUserException -> "Користувача з таким email не знайдено."
                        // Можна додати інші типи помилок входу, якщо потрібно
                        else -> task.exception?.localizedMessage ?: "Помилка входу."
                    }
                    onResult(false, errorMessage)
                }
            }
    }

    /**
     * Виконує вихід поточного користувача з облікового запису.
     */
    fun logout() {
        auth.signOut()
    }
}