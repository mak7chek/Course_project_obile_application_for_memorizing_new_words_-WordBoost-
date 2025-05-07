package com.example.wordboost.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.wordboost.data.firebase.AuthRepository // Переконайтесь в імпорті
import com.example.wordboost.data.firebase.FirebaseRepository // Переконайтесь в імпорті
import com.example.wordboost.data.repository.PracticeRepository // Переконайтесь в імпорті
import com.example.wordboost.data.repository.TranslationRepository // Переконайтесь в імпорті
import com.example.wordboost.data.tts.TextToSpeechService // Переконайтесь в імпорті
import com.example.wordboost.viewmodel.EditWordViewModelFactory
import com.example.wordboost.viewmodel.WordListViewModelFactory
import com.example.wordboost.viewmodel.PracticeViewModelFactory
import com.example.wordboost.ui.screens.PracticeScreen

enum class AppState {
    Loading,
    UnauthenticatedChoice,
    Login,
    Register,
    AuthenticatedMain,
    AuthenticatedTranslate,
    AuthenticatedPractice,
    AuthenticatedWordList,
    AuthenticatedEditWord
}


@Composable
fun MainScreen(
    authRepo: AuthRepository,
    practiceRepo: PracticeRepository,
    firebaseRepo: FirebaseRepository,
    translationRepo: TranslationRepository,
    ttsService: TextToSpeechService
) {
    var currentAppState by remember { mutableStateOf<AppState>(AppState.Loading) }
    var editingWordId by remember { mutableStateOf<String?>(null) }


    // --- Створюємо Factory для ViewModel Практики ---
    // Factory потребує PracticeRepository, TtsService та AuthRepository
    val practiceViewModelFactory = remember(practiceRepo, ttsService, authRepo) {
        // Передаємо practiceRepo як repository до PracticeViewModelFactory
        PracticeViewModelFactory(repository = practiceRepo, ttsService = ttsService, authRepository = authRepo)
    }

    // --- Створюємо Factory для ViewModel Списку Слів (якщо вона тут потрібна) ---
    val wordListViewModelFactory = remember(firebaseRepo, authRepo, ttsService) {
        // Передаємо firebaseRepo як repository до WordListViewModelFactory
        WordListViewModelFactory(repository = firebaseRepo, authRepository = authRepo, ttsService = ttsService)
    }


    LaunchedEffect(authRepo) { // Запускається при зміні authRepo (або один раз при старті)
        Log.d("MainScreen", "Collecting auth state...")
        authRepo.getAuthState().collect { user ->
            Log.d("MainScreen", "Auth state changed in collect: User ID = ${user?.uid}")
            currentAppState = if (user != null && user.isEmailVerified) {
                Log.d("MainScreen", "User authenticated and verified.")
                AppState.AuthenticatedMain
            } else if (user != null && !user.isEmailVerified) {
                Log.d("MainScreen", "User authenticated but NOT verified.")
                AppState.UnauthenticatedChoice // Або повертаємо на вибір автентифікації
            }
            else {
                Log.d("MainScreen", "User is null (logged out).")
                AppState.UnauthenticatedChoice // Якщо користувач null, переходимо на екран вибору автентифікації
            }
            Log.d("MainScreen", "AppState updated to: $currentAppState")
        }
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
                },
                wordListViewModelFactory = wordListViewModelFactory
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
                factory = practiceViewModelFactory, // <<< Передаємо Factory
                onBack = { // Колбек повернення з PracticeScreen
                    Log.d("MainScreen", "Practice Back, Navigating to AuthenticatedMain")
                    currentAppState = AppState.AuthenticatedMain // Повертаємось до головного
                }
            )
        }


        AppState.AuthenticatedWordList -> {
            WordListScreen(
                repository = firebaseRepo,
                ttsService = ttsService,
                authRepository = authRepo,
                onWordEdit = { wordId ->
                    Log.d("MainScreen", "Handling onWordEdit. Navigating to AuthenticatedEditWord for word ID: $wordId")
                    editingWordId = wordId
                    currentAppState = AppState.AuthenticatedEditWord
                },
                onBack = {
                    Log.d("MainScreen", "WordList Back, Navigating to AuthenticatedMain")
                    currentAppState = AppState.AuthenticatedMain
                }
            )
        }
        AppState.AuthenticatedEditWord -> {
            if (editingWordId != null) {
                EditWordScreen(
                    wordId = editingWordId,
                    factory = EditWordViewModelFactory(repository = firebaseRepo, wordId = editingWordId),
                    onBack = {
                        Log.d("MainScreen", "Handling onBack from EditWordScreen. Navigating back to AuthenticatedWordList.")
                        editingWordId = null
                        currentAppState = AppState.AuthenticatedWordList
                    }
                )
            } else {

                Log.e("MainScreen", "Attempted to navigate to EditWord with null editingWordId. Redirecting to WordList.")
                currentAppState = AppState.AuthenticatedWordList
            }
        }
    }
}
