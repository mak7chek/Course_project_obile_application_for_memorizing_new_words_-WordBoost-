package com.example.wordboost.ui.screens
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.repository.TranslationRepository



enum class AppState {
    Loading,
    UnauthenticatedChoice,
    Login,
    Register,
    AuthenticatedMain,
    AuthenticatedTranslate,
    AuthenticatedPractice,
    AuthenticatedWordList
}


@Composable
fun MainScreen(
    authRepo: AuthRepository,
    practiceRepo: PracticeRepository,
    firebaseRepo: FirebaseRepository,
    translationRepo: TranslationRepository
) {

    var currentAppState by remember { mutableStateOf<AppState>(AppState.Loading) }

    LaunchedEffect(Unit) {
        Log.d("MainScreen", "Checking authentication status...")
        val currentUser = authRepo.getCurrentUser()
        currentAppState = if (currentUser != null && currentUser.isEmailVerified) {
            Log.d("MainScreen", "User authenticated and verified.")
            AppState.AuthenticatedMain
        } else {
            Log.d("MainScreen", "User not authenticated or not verified.")
            AppState.UnauthenticatedChoice
        }
        Log.d("MainScreen", "Initial AppState: $currentAppState")
    }

    when (currentAppState) {
        AppState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AppState.UnauthenticatedChoice -> {
            AuthChoiceScreen(
                onLoginClick = {
                    Log.d("MainScreen", "Navigating to Login")
                    currentAppState = AppState.Login
                },
                onRegisterClick = {
                    Log.d("MainScreen", "Navigating to Register")
                    currentAppState = AppState.Register
                }
            )
        }
        AppState.Login -> {
            LoginScreen(
                authRepo = authRepo,
                onSuccess = {
                    Log.d("MainScreen", "Login Success, Navigating to AuthenticatedMain")
                    currentAppState = AppState.AuthenticatedMain
                },
                onBack = {
                    Log.d("MainScreen", "Login Back, Navigating to UnauthenticatedChoice")
                    currentAppState = AppState.UnauthenticatedChoice
                }
            )
        }
        AppState.Register -> {
            RegisterScreen(
                authRepo = authRepo,
                onRegistrationSuccess = {
                    Log.d("MainScreen", "Registration Success, Navigating to Login")
                    currentAppState = AppState.Login
                },
                onBack = {
                    Log.d("MainScreen", "Register Back, Navigating to UnauthenticatedChoice")
                    currentAppState = AppState.UnauthenticatedChoice
                }
            )
        }
        AppState.AuthenticatedMain -> {
            AuthenticatedMainScreen(
                onTranslateClick = {
                    Log.d("MainScreen", "Navigating to Translate")
                    currentAppState = AppState.AuthenticatedTranslate
                },
                onPracticeClick = {
                    Log.d("MainScreen", "Navigating to Practice")
                    currentAppState = AppState.AuthenticatedPractice
                },
                onWordListClick = {
                    Log.d("MainScreen", "Navigating to WordList")
                    currentAppState = AppState.AuthenticatedWordList
                },
                onLogoutClick = {
                    Log.d("MainScreen", "Logging out")
                    authRepo.logout()
                    currentAppState = AppState.UnauthenticatedChoice
                }
            )
        }
        AppState.AuthenticatedTranslate -> {
            TranslateScreen(
                firebaseRepo = firebaseRepo,
                translationRepo = translationRepo,
                onBack = {
                    Log.d("MainScreen", "Translate Back, Navigating to AuthenticatedMain")
                    currentAppState = AppState.AuthenticatedMain
                }
            )
        }
        AppState.AuthenticatedPractice -> {
            PracticeScreen(
                practiceRepo = practiceRepo,
                onBack = {
                    Log.d("MainScreen", "Practice Back, Navigating to AuthenticatedMain")
                    currentAppState = AppState.AuthenticatedMain
                }
            )
        }
        AppState.AuthenticatedWordList -> {
            WordListScreen(
                repository = firebaseRepo,
                onWordEdit = { wordId ->
                    Log.d("MainScreen", "Attempt to edit word: $wordId")
                    currentAppState = AppState.AuthenticatedMain
                },
                onBack = {
                    Log.d("MainScreen", "WordList Back, Navigating to AuthenticatedMain")
                    currentAppState = AppState.AuthenticatedMain
                }
            )
        }

    }
}
