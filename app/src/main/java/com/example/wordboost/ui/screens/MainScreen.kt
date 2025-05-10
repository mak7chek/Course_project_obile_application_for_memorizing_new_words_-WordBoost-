package com.example.wordboost.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel // Важливо для viewModel()
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.data.tts.TextToSpeechService
import com.example.wordboost.viewmodel.* // Твої factories
// import com.example.wordboost.viewmodel.CreateSetViewModel // Тепер це ui.viewmodels.CreateSetViewModel
import com.example.wordboost.viewmodel.CreateSetViewModel // Правильний імпорт

// Оновлений AppState для керування верхньорівневими екранами
enum class TopLevelScreenState {
    Loading,
    AuthFlow,
    AuthenticatedApp, // Показує AuthenticatedAppScaffold (з BottomNav)
    TranslateWordFullScreen,
    PracticeSessionFullScreen,
    WordListFullScreen,
    EditWordFullScreen,
    CreateSetWizardFullScreen
}

// Допоміжний enum для внутрішньої навігації в AuthFlow
enum class AuthSubState {
    AuthChoice, Login, Register
}

@Composable
fun MainScreen(
    authRepo: AuthRepository,
    practiceRepo: PracticeRepository,
    firebaseRepo: FirebaseRepository,
    translationRepo: TranslationRepository,
    ttsService: TextToSpeechService
) {
    var currentTopLevelScreenState by remember { mutableStateOf(TopLevelScreenState.Loading) }
    var currentAuthSubState by remember { mutableStateOf(AuthSubState.AuthChoice) }
    var editingWordIdForFullScreen by remember { mutableStateOf<String?>(null) }

    val practiceViewModelFactory = remember(practiceRepo, ttsService, authRepo) {
        PracticeViewModelFactory(repository = practiceRepo, ttsService = ttsService, authRepository = authRepo)
    }
    val wordListViewModelFactory = remember(firebaseRepo, authRepo, ttsService) {
        WordListViewModelFactory(repository = firebaseRepo, authRepository = authRepo, ttsService = ttsService)
    }
    val editWordViewModelFactoryBuilder = remember(firebaseRepo) {
        { wordId: String? -> EditWordViewModelFactory(repository = firebaseRepo, wordId = wordId) }
    }
    // translateViewModelFactory не потрібен тут, якщо TranslateScreen викликається напряму з усіма залежностями
    val createSetViewModelFactory = remember(firebaseRepo, translationRepo, authRepo) {
        CreateSetViewModelFactory(firebaseRepository = firebaseRepo, translationRepository = translationRepo, authRepository = authRepo)
    }

    LaunchedEffect(authRepo) {
        authRepo.getAuthState().collect { user ->
            val previousState = currentTopLevelScreenState // Зберігаємо попередній стан
            if (user != null && user.isEmailVerified) {
                Log.d("MainScreen", "User authenticated and verified.")
                // Переходимо до AuthenticatedApp тільки якщо ми були в Loading або AuthFlow
                if (previousState == TopLevelScreenState.Loading || previousState == TopLevelScreenState.AuthFlow) {
                    currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                }
                // Якщо користувач вже був у якомусь автентифікованому стані, не змінюємо його тут,
                // щоб не перервати, наприклад, сесію практики при оновленні токена.
            } else if (user != null && !user.isEmailVerified) {
                Log.d("MainScreen", "User authenticated but NOT verified.")
                currentTopLevelScreenState = TopLevelScreenState.AuthFlow
                currentAuthSubState = AuthSubState.Login
            } else { // user is null
                Log.d("MainScreen", "User is null (logged out).")
                currentTopLevelScreenState = TopLevelScreenState.AuthFlow
                currentAuthSubState = AuthSubState.AuthChoice
            }
            Log.d("MainScreen", "TopLevelScreenState updated to: $currentTopLevelScreenState, AuthSubState: $currentAuthSubState")
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (currentTopLevelScreenState) {
            TopLevelScreenState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            TopLevelScreenState.AuthFlow -> {
                when (currentAuthSubState) {
                    AuthSubState.AuthChoice -> AuthChoiceScreen(
                        onLoginClick = { currentAuthSubState = AuthSubState.Login },
                        onRegisterClick = { currentAuthSubState = AuthSubState.Register }
                    )
                    AuthSubState.Login -> LoginScreen(
                        authRepo = authRepo,
                        onSuccess = { /* Стан зміниться через LaunchedEffect */ },
                        onBack = { currentAuthSubState = AuthSubState.AuthChoice },
                        onNavigateToRegister = { currentAuthSubState = AuthSubState.Register } // <--- Тепер LoginScreen має цей параметр
                    )
                    AuthSubState.Register -> RegisterScreen(
                        authRepo = authRepo,
                        onRegistrationSuccess = { currentAuthSubState = AuthSubState.Login },
                        onBack = { currentAuthSubState = AuthSubState.AuthChoice }
                    )
                }
            }
            TopLevelScreenState.AuthenticatedApp -> {
                AuthenticatedAppScaffold(
                    // Передаємо колбеки, які змінюють currentTopLevelScreenState
                    onNavigateToTranslate = { currentTopLevelScreenState = TopLevelScreenState.TranslateWordFullScreen },
                    onNavigateToPractice = { currentTopLevelScreenState = TopLevelScreenState.PracticeSessionFullScreen },
                    onNavigateToWordList = { currentTopLevelScreenState = TopLevelScreenState.WordListFullScreen },
                    onNavigateToCreateSet = { currentTopLevelScreenState = TopLevelScreenState.CreateSetWizardFullScreen },
                    onLogoutClick = { authRepo.logout() /* Стан зміниться через LaunchedEffect */ },
                    // Передаємо тільки ті factories, які потрібні екранам всередині AuthenticatedAppScaffold
                    wordListViewModelFactory = wordListViewModelFactory
                )
            }
            TopLevelScreenState.TranslateWordFullScreen -> {
                TranslateScreen(
                    firebaseRepo = firebaseRepo,
                    translationRepo = translationRepo,
                    onBack = { currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp }
                )
            }
            TopLevelScreenState.PracticeSessionFullScreen -> {
                PracticeScreen(
                    factory = practiceViewModelFactory,
                    onBack = { currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp }
                )
            }
            TopLevelScreenState.WordListFullScreen -> {
                WordListScreen(
                    repository = firebaseRepo,
                    ttsService = ttsService,
                    authRepository = authRepo,
                    onWordEdit = { wordId ->
                        editingWordIdForFullScreen = wordId
                        currentTopLevelScreenState = TopLevelScreenState.EditWordFullScreen
                    },
                    onBack = { currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp }
                )
            }
            TopLevelScreenState.EditWordFullScreen -> {
                EditWordScreen(
                    wordId = editingWordIdForFullScreen,
                    factory = editWordViewModelFactoryBuilder(editingWordIdForFullScreen),
                    onBack = {
                        editingWordIdForFullScreen = null
                        currentTopLevelScreenState = TopLevelScreenState.WordListFullScreen
                    }
                )
            }
            TopLevelScreenState.CreateSetWizardFullScreen -> {
                val createSetViewModel: CreateSetViewModel = viewModel(factory = createSetViewModelFactory)
                CreateSetScreen( // Цей екран керує своїми внутрішніми кроками
                    viewModel = createSetViewModel,
                    onCloseOrNavigateBack = {
                        createSetViewModel.resetAllState()
                        currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                    }
                )
            }
        }
    }
}