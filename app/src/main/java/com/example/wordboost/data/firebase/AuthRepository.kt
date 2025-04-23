package com.example.wordboost.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun registerUser(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sendEmailVerification(callback)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }
    fun sendEmailVerification(callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user != null && !user.isEmailVerified) {
            user.sendEmailVerification()
                .addOnSuccessListener { callback(true, "Лист верифікації надіслано") }
                .addOnFailureListener { callback(false, it.message) }
        } else {
            callback(false, "Користувач не знайдений або вже підтверджений")
        }
    }

    fun isUserVerified(callback: (Boolean) -> Unit) {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener {
            callback(user.isEmailVerified)
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun loginUser(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val isVerified = auth.currentUser?.isEmailVerified ?: false
                    if (isVerified) {
                        callback(true, "Успішний вхід")
                    } else {
                        callback(false, "Підтвердіть свою email адресу перед входом")
                    }
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

}