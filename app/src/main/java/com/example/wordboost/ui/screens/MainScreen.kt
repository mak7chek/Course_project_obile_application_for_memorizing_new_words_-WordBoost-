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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.data.tts.TextToSpeechService
import com.example.wordboost.viewmodel.CreateSetViewModel
import com.example.wordboost.viewmodel.CreateSetViewModelFactory
import com.example.wordboost.viewmodel.EditWordViewModelFactory
import com.example.wordboost.viewmodel.PracticeViewModelFactory
import com.example.wordboost.viewmodel.WordListViewModelFactory
import com.example.wordboost.viewmodel.SetsViewModelFactory
import com.example.wordboost.ui.screens.createset.CreateSetScreen
import com.example.wordboost.viewmodel.BrowseSharedSetViewModel
import com.example.wordboost.viewmodel.BrowseSetViewModelFactory
import com.example.wordboost.viewmodel.ArticleViewModel
import com.example.wordboost.viewmodel.ArticleViewModelFactory
import com.example.wordboost.ui.screens.articles.*

enum class TopLevelScreenState {
    Loading,
    AuthFlow,
    AuthenticatedApp,
    TranslateWordFullScreen,
    PracticeSessionFullScreen,
    WordListFullScreen,
    EditWordFullScreen,
    CreateSetWizardFullScreen,
    BrowseSharedSetFullScreen,
    ViewArticleFullScreen,
    CreateEditArticleFullScreen
}

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
    var currentSetIdForBrowse by remember { mutableStateOf<String?>(null) }
    var editingSetId by remember { mutableStateOf<String?>(null) }
    var viewingArticleId by remember { mutableStateOf<String?>(null) }
    var editingArticleId by remember { mutableStateOf<String?>(null) }
    var enTextForTranslateScreen by remember { mutableStateOf<String?>(null) }
    var ukTextForTranslateScreen by remember { mutableStateOf<String?>(null) }
    var previousScreenBeforeTranslate by remember { mutableStateOf<TopLevelScreenState?>(null) }

    val practiceViewModelFactory = remember(practiceRepo, ttsService, authRepo) { PracticeViewModelFactory(repository = practiceRepo, ttsService = ttsService, authRepository = authRepo) }
    val wordListViewModelFactory = remember(firebaseRepo, authRepo, ttsService) { WordListViewModelFactory(repository = firebaseRepo, authRepository = authRepo, ttsService = ttsService) }
    val editWordViewModelFactoryBuilder = remember(firebaseRepo) { { wordId: String? -> EditWordViewModelFactory(repository = firebaseRepo, wordId = wordId) } }
    val createSetViewModelFactory = remember(firebaseRepo, translationRepo, authRepo) { CreateSetViewModelFactory(firebaseRepository = firebaseRepo, translationRepository = translationRepo, authRepository = authRepo) }
    val setsViewModelFactory = remember(firebaseRepo, authRepo) { SetsViewModelFactory(firebaseRepository = firebaseRepo, authRepository = authRepo) }
    val articleViewModelFactory = remember(firebaseRepo, translationRepo, ttsService, authRepo) {
        ArticleViewModelFactory(
            firebaseRepository = firebaseRepo,
            translationRepository = translationRepo,
            ttsService = ttsService,
            authRepository = authRepo
        )
    }
    val navigateToTranslateScreenWithData: (originalText: String, translatedText: String, articleLanguageCode: String) -> Unit =
        { original, translated, langCode ->
            if (langCode.startsWith("en", ignoreCase = true)) {
                enTextForTranslateScreen = original
                ukTextForTranslateScreen = translated
            } else {
                ukTextForTranslateScreen = original
                enTextForTranslateScreen = translated
            }
            previousScreenBeforeTranslate = TopLevelScreenState.ViewArticleFullScreen
            currentTopLevelScreenState = TopLevelScreenState.TranslateWordFullScreen
        }

    val navigateToGenericTranslateScreen: () -> Unit = {
        enTextForTranslateScreen = null
        ukTextForTranslateScreen = null
        previousScreenBeforeTranslate = TopLevelScreenState.AuthenticatedApp
        currentTopLevelScreenState = TopLevelScreenState.TranslateWordFullScreen
    }
    LaunchedEffect(key1 = authRepo) {
        Log.d("MainScreen", "Auth state listener collection started.")
        authRepo.getAuthState().collect { user ->
            val previousState = currentTopLevelScreenState
            Log.d("MainScreen", "Auth state changed: User UID = ${user?.uid}, Verified = ${user?.isEmailVerified}. Previous TopLevelState = $previousState")
            if (user != null && user.isEmailVerified) {
                Log.d("MainScreen", "User authenticated and verified.")
                if (previousState == TopLevelScreenState.Loading || previousState == TopLevelScreenState.AuthFlow) {
                    currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                    Log.d("MainScreen", "Transitioning to AuthenticatedApp")
                } else {
                    Log.d("MainScreen", "User already in an authenticated session state ($previousState), no state change needed here.")
                }
            } else if (user != null && !user.isEmailVerified) {
                Log.d("MainScreen", "User authenticated but NOT verified. Directing to AuthFlow/Login.")
                currentTopLevelScreenState = TopLevelScreenState.AuthFlow
                currentAuthSubState = AuthSubState.Login
            } else {
                Log.d("MainScreen", "User is null (logged out). Directing to AuthFlow/AuthChoice.")
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
                    Log.d("MainScreen", "Showing Loading state")
                }
            }
            TopLevelScreenState.AuthFlow -> {
                Log.d("MainScreen", "Showing AuthFlow, subState: $currentAuthSubState")
                when (currentAuthSubState) {
                    AuthSubState.AuthChoice -> AuthChoiceScreen(
                        onLoginClick = { currentAuthSubState = AuthSubState.Login },
                        onRegisterClick = { currentAuthSubState = AuthSubState.Register }
                    )
                    AuthSubState.Login -> LoginScreen(
                        authRepo = authRepo,
                        onSuccess = { },
                        onBack = { currentAuthSubState = AuthSubState.AuthChoice },
                        onNavigateToRegister = { currentAuthSubState = AuthSubState.Register }
                    )
                    AuthSubState.Register -> RegisterScreen(
                        authRepo = authRepo,
                        onRegistrationSuccess = { currentAuthSubState = AuthSubState.Login },
                        onBack = { currentAuthSubState = AuthSubState.AuthChoice }
                    )
                }
            }
            TopLevelScreenState.AuthenticatedApp -> {
                Log.i("MainScreen_Render", "Showing AuthenticatedAppScaffold UI")
                AuthenticatedAppScaffold(
                    onNavigateToTranslate = navigateToGenericTranslateScreen, // Оновлений колбек
                    onNavigateToPractice = { currentTopLevelScreenState = TopLevelScreenState.PracticeSessionFullScreen },
                    onNavigateToWordList = { currentTopLevelScreenState = TopLevelScreenState.WordListFullScreen },

                    onNavigateToCreateSet = {
                        editingSetId = null
                        currentTopLevelScreenState = TopLevelScreenState.CreateSetWizardFullScreen
                    },
                    onLogoutClick = { authRepo.logout() },
                    onNavigateToBrowseSet = { setId ->
                        Log.i("MainScreen_Nav", "Action to browse set ID: '$setId'. Changing TopLevelScreenState to BrowseSharedSetFullScreen.")
                        currentSetIdForBrowse = setId
                        currentTopLevelScreenState = TopLevelScreenState.BrowseSharedSetFullScreen
                    },
                    onNavigateToEditSet = { setIdToEdit ->
                        Log.i("MainScreen_Nav", "Action to EDIT set ID: '$setIdToEdit'. Changing TopLevelScreenState to CreateSetWizardFullScreen for editing.")
                        editingSetId = setIdToEdit
                        currentTopLevelScreenState = TopLevelScreenState.CreateSetWizardFullScreen
                    },
                    wordListViewModelFactory = wordListViewModelFactory,
                    setsViewModelFactory = setsViewModelFactory,
                    articleViewModelFactory = articleViewModelFactory,
                    onNavigateToViewArticle = { articleId ->
                        viewingArticleId = articleId
                        currentTopLevelScreenState = TopLevelScreenState.ViewArticleFullScreen
                    },
                    onNavigateToCreateArticle = {
                        editingArticleId = null
                        currentTopLevelScreenState = TopLevelScreenState.CreateEditArticleFullScreen
                    },
                    onNavigateToEditArticle = { articleId ->
                        editingArticleId = articleId
                        currentTopLevelScreenState = TopLevelScreenState.CreateEditArticleFullScreen
                    }
                )
            }

            TopLevelScreenState.ViewArticleFullScreen -> {
                viewingArticleId?.let { articleId ->
                    val articleViewModel: ArticleViewModel = viewModel(key = "ViewArticle_$articleId", factory = articleViewModelFactory)
                    ViewArticleScreen(
                        articleId = articleId,
                        viewModel = articleViewModel,
                        onBack = {
                            viewingArticleId = null
                            currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                        },
                        onNavigateToTranslateScreenWithData = navigateToTranslateScreenWithData
                    )


                } ?: LaunchedEffect(Unit) {
                    if (currentTopLevelScreenState == TopLevelScreenState.ViewArticleFullScreen) {
                        currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                    }
                }
            }
            TopLevelScreenState.CreateEditArticleFullScreen -> {
                Log.d("MainScreen", "Showing CreateEditArticleFullScreen for articleId: $editingArticleId")
                val articleViewModel: ArticleViewModel = viewModel(
                    key = editingArticleId ?: "CreateArticle", // Унікальний ключ
                    factory = articleViewModelFactory
                )
                CreateEditArticleScreen(
                    editingArticleId = editingArticleId,
                    viewModel = articleViewModel,
                    onSaveSuccess = {
                        editingArticleId = null
                        currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp // Повернення на екран зі списком статей (вкладку)
                    },
                    onBack = {
                        editingArticleId = null
                        currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp // Повернення на екран зі списком статей (вкладку)
                    }
                )
            }
            TopLevelScreenState.TranslateWordFullScreen -> {
                TranslateScreen(
                    firebaseRepo = firebaseRepo,
                    translationRepo = translationRepo,
                    initialEn = enTextForTranslateScreen,
                    initialUk = ukTextForTranslateScreen,
                    onBack = {
                        currentTopLevelScreenState = previousScreenBeforeTranslate ?: TopLevelScreenState.AuthenticatedApp
                        previousScreenBeforeTranslate = null
                        enTextForTranslateScreen = null
                        ukTextForTranslateScreen = null
                    }
                )
            }
            TopLevelScreenState.PracticeSessionFullScreen -> {
                Log.d("MainScreen", "Showing PracticeSessionFullScreen")
                PracticeScreen(
                    factory = practiceViewModelFactory,
                    onBack = { currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp }
                )
            }
            TopLevelScreenState.WordListFullScreen -> {
                Log.d("MainScreen", "Showing WordListFullScreen")
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
                Log.d("MainScreen", "Showing EditWordFullScreen for wordId: $editingWordIdForFullScreen")
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
                Log.i("MainScreen_Render", "Showing CreateSetWizardFullScreen. Current editingSetId: $editingSetId")

                val currentCreateEditSetId = editingSetId
                val createSetViewModel: CreateSetViewModel = viewModel(
                    key = currentCreateEditSetId ?: "create_new_set_mode",
                    factory = createSetViewModelFactory
                )

                LaunchedEffect(currentCreateEditSetId, createSetViewModel) {
                    if (currentCreateEditSetId != null) {
                        Log.d("MainScreen_CreateSet", "editingSetId is '$currentCreateEditSetId', calling loadSetForEditing.")
                        createSetViewModel.loadSetForEditing(currentCreateEditSetId)
                    } else {
                        Log.d("MainScreen_CreateSet", "editingSetId is null, resetting ViewModel for new set creation (if needed).")

                    }
                }

                CreateSetScreen(
                    viewModel = createSetViewModel,
                    isEditing = currentCreateEditSetId != null,
                    onCloseOrNavigateBack = {
                        Log.d("MainScreen_CreateSet", "Closing CreateSetWizard. Resetting ViewModel and editingSetId.")
                        createSetViewModel.resetAllState()
                        editingSetId = null
                        currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                    }
                )
            }
            TopLevelScreenState.BrowseSharedSetFullScreen -> {
                val setIdForView = currentSetIdForBrowse
                Log.i("MainScreen_Nav", "Composing content for BrowseSharedSetFullScreen with setId: $setIdForView")

                if (setIdForView != null) {
                    Log.d("MainScreen_Nav", "Set ID '$setIdForView' is not null, proceeding to create ViewModel and Screen.")
                    val browseSetViewModelFactory = remember(firebaseRepo, authRepo, ttsService, setIdForView) {
                        Log.d("MainScreen_Nav", "Creating BrowseSetViewModelFactory for setId: $setIdForView")
                        BrowseSetViewModelFactory(
                            sharedSetId = setIdForView,
                            firebaseRepository = firebaseRepo,
                            authRepository = authRepo,
                            ttsService = ttsService
                        )
                    }
                    val browseSetViewModel: BrowseSharedSetViewModel = viewModel(key = "BrowseVM_$setIdForView", factory = browseSetViewModelFactory)
                    Log.d("MainScreen_Nav", "BrowseSharedSetViewModel instance obtained for setId: $setIdForView. ViewModel: $browseSetViewModel")

                    BrowseSharedSetScreen(
                        viewModel = browseSetViewModel,
                        onBack = {
                            Log.i("MainScreen_Nav", "Back pressed from BrowseSharedSetScreen. Set ID was: $setIdForView")
                            currentSetIdForBrowse = null
                            currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                        }
                    )
                    Log.d("MainScreen_Nav", "BrowseSharedSetScreen composed for $setIdForView.")
                } else {
                    Log.e("MainScreen_Nav", "Error: currentSetIdForBrowse is NULL when trying to display BrowseSharedSetFullScreen. Navigating back to AuthenticatedApp.")
                    LaunchedEffect(Unit) {
                        currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                    }
                }
            }

        }
    }
}