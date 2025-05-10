package com.example.wordboost.ui.screens // Або твій правильний пакет

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wordboost.data.model.DifficultyLevel
import com.example.wordboost.data.model.SharedCardSetSummary
import com.example.wordboost.navigation.BottomNavItem
import com.example.wordboost.ui.components.MyBottomNavigationBar
import com.example.wordboost.viewmodel.SetsViewModel
import com.example.wordboost.viewmodel.SetsViewModelFactory
import com.example.wordboost.viewmodel.WordListViewModelFactory
import kotlinx.coroutines.launch

// Імпорти для Material 3 Pull-to-Refresh
// Переконайся, що ці шляхи актуальні для твоєї версії Material 3
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

// Необхідний імпорт для делегування 'by' з State
import androidx.compose.runtime.getValue

// --- Екран "Набори" (SetsScreen) ---
@OptIn(ExperimentalMaterial3Api::class) // Для Scaffold, PullToRefreshContainer, etc.
@Composable
fun SetsScreen(
    viewModel: SetsViewModel,
    onNavigateToCreateSet: () -> Unit,
    onNavigateToViewPublicSet: (setId: String) -> Unit,
    onNavigateToEditMySet: (setId: String) -> Unit
) {
    // Використовуємо getValue для делегування
    val mySets by viewModel.mySets.collectAsState()
    val publicSets by viewModel.publicSets.collectAsState()
    val isLoadingMySets by viewModel.isLoadingMySets.collectAsState()
    val isLoadingPublicSets by viewModel.isLoadingPublicSets.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val isOverallLoading = isLoadingMySets || isLoadingPublicSets

    // Використовуємо M3 PullToRefreshState
    val pullRefreshState = rememberPullToRefreshState() // Тепер має бути доступний

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                viewModel.clearErrorMessage()
            }
        }
    }

    // Обробка дії оновлення для M3 PullToRefresh
    if (pullRefreshState.isRefreshing) { // 'isRefreshing' є властивістю PullToRefreshState
        LaunchedEffect(true) { // Викликаємо тільки один раз, коли isRefreshing стає true
            Log.d("SetsScreen", "Pull to refresh triggered. Calling loadAllSets().")
            viewModel.loadAllSets()
            // Не викликаємо endRefresh() тут, оскільки isOverallLoading це зробить
        }
    }

    // Синхронізуємо стан isRefreshing з ViewModel з pullRefreshState
    // Коли isOverallLoading стає false, завершуємо анімацію оновлення
    LaunchedEffect(isOverallLoading) {
        if (!isOverallLoading) {
            pullRefreshState.endRefresh() // 'endRefresh' є методом PullToRefreshState
            Log.d("SetsScreen", "Data loading finished, calling endRefresh().")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateSet) {
                Icon(Icons.Filled.Add, contentDescription = "Створити набір")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Секція "Мої набори"
                item {
                    Text("Мої набори", style = MaterialTheme.typography.headlineSmall)
                    if (isLoadingMySets && mySets.isEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                if (mySets.isNotEmpty()) {
                    items(mySets, key = { it.id }) { setSummary ->
                        SharedCardSetSummaryItem(
                            summary = setSummary,
                            onClick = { onNavigateToEditMySet(setSummary.id) }
                        )
                    }
                } else if (!isLoadingMySets) {
                    item { Text("У вас ще немає створених наборів.", fontStyle = FontStyle.Italic, modifier = Modifier.padding(vertical = 8.dp)) }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Секція "Публічні набори"
                item {
                    Text("Публічні набори", style = MaterialTheme.typography.headlineSmall)
                    if (isLoadingPublicSets && publicSets.isEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                if (publicSets.isNotEmpty()) {
                    items(publicSets, key = { it.id }) { setSummary ->
                        SharedCardSetSummaryItem(
                            summary = setSummary,
                            onClick = { onNavigateToViewPublicSet(setSummary.id) }
                        )
                    }
                } else if (!isLoadingPublicSets) {
                    item { Text("Публічних наборів поки що немає.", fontStyle = FontStyle.Italic, modifier = Modifier.padding(vertical = 8.dp)) }
                }
            }

            // PullToRefreshContainer для M3
            PullToRefreshContainer( // Тепер має бути доступний
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

// --- SharedCardSetSummaryItem ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCardSetSummaryItem(
    summary: SharedCardSetSummary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(summary.name_uk, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Слів: ${summary.wordCount}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = summary.difficultyLevelKey?.let { DifficultyLevel.fromKey(it)?.displayName } ?: "N/A",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            summary.authorName?.let {
                Text("Автор: $it", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            }
            if(summary.isPublic){
                Text("Публічний", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}


// --- Екран "Статті" (ArticlesScreen) ---
@Composable
fun ArticlesScreen() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Екран Статей", style = MaterialTheme.typography.headlineMedium)
        Text("Ця функція в розробці.")
    }
}

// --- AuthenticatedAppScaffold --- (код без змін, як у твоєму файлі, важливо, що SetsViewModel створюється правильно)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedAppScaffold(
    onNavigateToTranslate: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToWordList: () -> Unit,
    onNavigateToCreateSet: () -> Unit,
    onLogoutClick: () -> Unit,
    onNavigateToBrowseSet: (String) -> Unit,
    setsViewModelFactory: SetsViewModelFactory,
    wordListViewModelFactory: WordListViewModelFactory
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { MyBottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                AuthenticatedMainScreen(
                    navController = navController, // Передаємо NavController для можливої внутрішньої навігації на вкладці
                    onNavigateToTranslate = onNavigateToTranslate,
                    onNavigateToPractice = onNavigateToPractice,
                    onNavigateToWordList = onNavigateToWordList,
                    onLogoutClick = onLogoutClick,
                    wordListViewModelFactory = wordListViewModelFactory
                )
            }
            composable(BottomNavItem.Sets.route) {
                val viewModel: SetsViewModel = viewModel(factory = setsViewModelFactory)
                SetsScreen(
                    viewModel = viewModel,
                    onNavigateToCreateSet = onNavigateToCreateSet,
                    onNavigateToViewPublicSet = { setId -> onNavigateToBrowseSet(setId) },
                    onNavigateToEditMySet = { setId -> onNavigateToBrowseSet(setId) } // TODO: Замінити на навігацію на екран редагування
                )
            }
            composable(BottomNavItem.Articles.route) {
                ArticlesScreen()
            }
        }
    }
}