package com.example.wordboost

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.wordboost.ui.theme.Course_project_obile_application_for_memorizing_new_words_WordBoostTheme
import com.google.firebase.FirebaseApp
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.ui.screens.*
import com.google.firebase.auth.FirebaseAuth

// Визначаємо стани програми для навігації
enum class AppState {
    Loading,             // Початковий стан, поки перевіряємо аутентифікацію
    UnauthenticatedChoice, // Користувач не увійшов, показуємо вибір між входом і реєстрацією
    Login,               // Показуємо екран входу
    Register,            // Показуємо екран реєстрації
    AuthenticatedMain,   // Користувач увійшов, показуємо головне меню
    AuthenticatedTranslate, // Користувач увійшов, показуємо екран перекладу
    AuthenticatedPractice // Користувач увійшов, показуємо екран практики
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val firebaseRepo = FirebaseRepository()
        val practiceRepo = PracticeRepository(firebaseRepo)
        val authRepo = com.example.wordboost.data.firebase.AuthRepository() // Створюємо тут для доступності

        setContent {
            Course_project_obile_application_for_memorizing_new_words_WordBoostTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Передаємо AuthRepository
                    MainScreen(authRepo = authRepo, practiceRepo = practiceRepo)
                }
            }
        }
    }
}

@Composable
fun MainScreen(authRepo: com.example.wordboost.data.firebase.AuthRepository, practiceRepo: PracticeRepository) {
    // Стан для керування поточним екраном/станом програми
    var currentAppState by remember { mutableStateOf(AppState.Loading) }

    // Ефект для початкової перевірки аутентифікації
    LaunchedEffect(Unit) {
        // Перевіряємо, чи є поточний користувач і чи його email верифіковано
        val currentUser = authRepo.getCurrentUser()
        currentAppState = if (currentUser != null && currentUser.isEmailVerified) {
            AppState.AuthenticatedMain
        } else {
            AppState.UnauthenticatedChoice
        }
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
            AuthChoiceScreen(
                onLoginClick = { currentAppState = AppState.Login },
                onRegisterClick = { currentAppState = AppState.Register }
            )
        }
        AppState.Login -> {
            LoginScreen(
                authRepo = authRepo,
                onSuccess = { currentAppState = AppState.AuthenticatedMain },
                onBack = { currentAppState = AppState.UnauthenticatedChoice } // Додаємо можливість повернутись
            )
        }
        AppState.Register -> {
            RegisterScreen(
                authRepo = authRepo,
                onRegistrationSuccess = {
                    // Після успішної реєстрації, можливо, переходимо до екрану входу
                    currentAppState = AppState.Login
                },
                onBack = { currentAppState = AppState.UnauthenticatedChoice } // Додаємо можливість повернутись
            )
        }
        AppState.AuthenticatedMain -> {
            AuthenticatedMainScreen( // Нова композитна функція для головного меню після входу
                onTranslateClick = { currentAppState = AppState.AuthenticatedTranslate },
                onPracticeClick = { currentAppState = AppState.AuthenticatedPractice },
                onLogoutClick = {
                    authRepo.logout()
                    currentAppState = AppState.UnauthenticatedChoice // Після виходу переходимо до вибору
                }
            )
        }
        AppState.AuthenticatedTranslate -> {
            TranslateScreen(onBack = { currentAppState = AppState.AuthenticatedMain }) // Додаємо можливість повернутись
        }
        AppState.AuthenticatedPractice -> {
            PracticeScreen(practiceRepo = practiceRepo, onBack = { currentAppState = AppState.AuthenticatedMain }) // Додаємо можливість повернутись
        }
    }
}

@Composable
fun AuthChoiceScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally // Вирівнюємо по центру горизонтально
    ) {
        Text(
            "Ласкаво просимо до WordBoost!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center, // Вирівнюємо текст по центру
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(0.8f) // Кнопка займає 80% ширини
        ) {
            Text("Увійти")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth(0.8f) // Кнопка займає 80% ширини
        ) {
            Text("Зареєструватись")
        }
    }
}

@Composable
fun AuthenticatedMainScreen(
    onTranslateClick: () -> Unit,
    onPracticeClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally // Вирівнюємо по центру горизонтально
    ) {
        Text(
            "Привіт у WordBoost!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onTranslateClick,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Перекласти слово")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPracticeClick,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Практика")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLogoutClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), // Можна змінити колір для кнопки виходу
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Вийти")
        }
    }
}


