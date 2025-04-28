package com.example.wordboost.ui.screens
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Імпортуємо репозиторії (вони будуть передаватися як параметри)
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.repository.TranslationRepository

// Імпортуємо ваші інші Composable екрани з ЦЬОГО Ж пакету (presentation.ui.screens)
// Розкоментуйте та переконайтесь, що package назва правильна
import com.example.wordboost.ui.screens.AuthChoiceScreen
import com.example.wordboost.ui.screens.LoginScreen
import com.example.wordboost.ui.screens.RegisterScreen
import com.example.wordboost.ui.screens.AuthenticatedMainScreen
import com.example.wordboost.ui.screens.TranslateScreen
import com.example.wordboost.ui.screens.PracticeScreen
import com.example.wordboost.ui.screens.WordListScreen


// TODO: Додайте імпорт для вашої теми, якщо вона використовується в цьому файлі
// import com.example.wordboost.ui.theme.Course_project_obile_application_for_memorizing_new_words_WordBoostTheme


// Enum для керування станом програми (перемикання екранів)
enum class AppState {
    Loading,
    UnauthenticatedChoice,
    Login,
    Register,
    AuthenticatedMain,
    AuthenticatedTranslate,
    AuthenticatedPractice,
    AuthenticatedWordList
    // TODO: Додати стан для редагування слова, наприклад AuthenticatedEditWord(val wordId: String)
}


// Головний Composable, який відповідає за навігацію/перемикання екранів
@Composable
fun MainScreen(
    // Приймаємо всі необхідні репозиторії як залежності
    authRepo: AuthRepository,
    practiceRepo: PracticeRepository,
    firebaseRepo: FirebaseRepository,
    translationRepo: TranslationRepository // <-- Додали translationRepo
    // TODO: Додайте інші репозиторії, якщо ваші інші екрани їх потребують
) {
    // Стан для керування поточним екраном/станом програми
    var currentAppState by remember { mutableStateOf<AppState>(AppState.Loading) } // Явно вказуємо тип <AppState>


    // Ефект для початкової перевірки аутентифікації при першому запуску Composable
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

    // Вибираємо, який екран показувати залежно від поточного стану
    when (currentAppState) {
        AppState.Loading -> {
            // Можна показати спінер або екран завантаження
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AppState.UnauthenticatedChoice -> {
            AuthChoiceScreen( // <-- Тепер імпортується
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
            LoginScreen( // <-- Тепер імпортується
                authRepo = authRepo, // Передаємо залежність
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
            RegisterScreen( // <-- Тепер імпортується
                authRepo = authRepo, // Передаємо залежність
                onRegistrationSuccess = {
                    Log.d("MainScreen", "Registration Success, Navigating to Login")
                    // Після успішної реєстрації, можливо, переходимо до екрану входу
                    currentAppState = AppState.Login
                },
                onBack = {
                    Log.d("MainScreen", "Register Back, Navigating to UnauthenticatedChoice")
                    currentAppState = AppState.UnauthenticatedChoice
                }
            )
        }
        AppState.AuthenticatedMain -> {
            AuthenticatedMainScreen( // <-- Тепер імпортується
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
                    authRepo.logout() // Викликаємо метод виходу з AuthRepo
                    currentAppState = AppState.UnauthenticatedChoice // Після виходу переходимо до вибору
                }
            )
        }
        AppState.AuthenticatedTranslate -> {
            TranslateScreen( // <-- Тепер імпортується
                firebaseRepo = firebaseRepo, // Передаємо залежність
                translationRepo = translationRepo, // Передаємо залежність
                onBack = {
                    Log.d("MainScreen", "Translate Back, Navigating to AuthenticatedMain")
                    currentAppState = AppState.AuthenticatedMain
                }
            )
        }
        AppState.AuthenticatedPractice -> {
            PracticeScreen( // <-- Тепер імпортується
                practiceRepo = practiceRepo, // Передаємо залежність
                onBack = {
                    Log.d("MainScreen", "Practice Back, Navigating to AuthenticatedMain")
                    currentAppState = AppState.AuthenticatedMain
                }
            )
        }
        AppState.AuthenticatedWordList -> {
            WordListScreen( // <-- Тепер імпортується
                repository = firebaseRepo, // Передаємо залежність
                onWordEdit = { wordId ->
                    Log.d("MainScreen", "Attempt to edit word: $wordId")
                    // TODO: Реалізуйте перехід на екран редагування
                    // Наприклад: currentAppState = AppState.AuthenticatedEditWord(wordId)
                    // Для прикладу, повертаємось назад:
                    currentAppState = AppState.AuthenticatedMain // Тимчасово
                },
                onBack = {
                    Log.d("MainScreen", "WordList Back, Navigating to AuthenticatedMain")
                    currentAppState = AppState.AuthenticatedMain
                }
            )
        }
        // TODO: Додати кейс для редагування слова
        /*
        is AppState.AuthenticatedEditWord -> {
            EditWordScreen(
                wordId = currentAppState.wordId, // Передаємо ID слова
                repository = firebaseRepo, // Передаємо репозиторій
                onSaveSuccess = { currentAppState = AppState.AuthenticatedWordList }, // Повернутись до списку після збереження
                onBack = { currentAppState = AppState.AuthenticatedWordList } // Повернутись до списку без збереження
            )
        }
        */
    }
}

