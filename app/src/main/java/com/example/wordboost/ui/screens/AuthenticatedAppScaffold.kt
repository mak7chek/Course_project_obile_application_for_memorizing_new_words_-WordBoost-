package com.example.wordboost.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
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
import com.example.wordboost.viewmodel.ArticleViewModel
import com.example.wordboost.viewmodel.ArticleViewModelFactory
import com.example.wordboost.viewmodel.WordListViewModelFactory
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost // Потрібні для NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import com.example.wordboost.ui.screens.articles.ArticlesListScreen
@Composable
fun SectionHeader(
    title: String,
    isExpanded: Boolean,
    isLoading: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Згорнути" else "Розгорнути"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCardSetSummaryItem(
    summary: SharedCardSetSummary,
    isMySet: Boolean,
    onViewClick: () -> Unit,
    onEditClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)?
) {
    Card(
        onClick = onViewClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(summary.name_uk, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Слів: ${summary.wordCount}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = summary.difficultyLevelKey?.let { DifficultyLevel.fromKey(it)?.displayName } ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                summary.authorName?.let {
                    Text("Автор: $it", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                }
                if (summary.public) {
                    Text("Публічний", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (isMySet) {
                onEditClick?.let { editClick ->
                    IconButton(onClick = editClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редагувати набір", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                onDeleteClick?.let { deleteClick ->
                    IconButton(onClick = deleteClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "Видалити набір", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetsScreen(
    viewModel: SetsViewModel,
    onNavigateToCreateSet: () -> Unit,
    onNavigateToViewPublicSet: (setId: String) -> Unit,
    onNavigateToBrowseMySet: (setId: String) -> Unit,
    onNavigateToEditSet: (setId: String) -> Unit
) {
    val mySets by viewModel.mySets.collectAsState()
    val publicSets by viewModel.publicSets.collectAsState()
    val isLoadingMySets by viewModel.isLoadingMySets.collectAsState()
    val isLoadingPublicSets by viewModel.isLoadingPublicSets.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showDeleteDialog by viewModel.showDeleteConfirmDialog.collectAsState()
    val isMySetsExpanded by viewModel.isMySetsExpanded.collectAsState()
    val isPublicSetsExpanded by viewModel.isPublicSetsExpanded.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val isOverallLoading = isLoadingMySets || isLoadingPublicSets
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(errorMessage) { errorMessage?.let { coroutineScope.launch { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short); viewModel.clearErrorMessage() } } }
    if (showDeleteDialog) { AlertDialog(onDismissRequest = { viewModel.cancelDeleteSet() }, title = { Text("Підтвердити видалення") }, text = { Text("Ви впевнені, що хочете видалити набір '${viewModel.getSetNameToDelete() ?: "цей набір"}'? Цю дію неможливо буде скасувати.") }, confirmButton = { TextButton(onClick = { viewModel.confirmDeleteSet() }) { Text("Видалити", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { viewModel.cancelDeleteSet() }) { Text("Скасувати") } }) }
    if (pullRefreshState.isRefreshing) { LaunchedEffect(true) { Log.d("SetsScreen", "Pull to refresh triggered."); viewModel.loadAllSets() } }
    LaunchedEffect(isOverallLoading) { if (!isOverallLoading && pullRefreshState.isRefreshing) { pullRefreshState.endRefresh(); Log.d("SetsScreen", "Data loading finished, calling endRefresh().") } }

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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SectionHeader(
                        title = "Мої набори",
                        isExpanded = isMySetsExpanded,
                        isLoading = isLoadingMySets && mySets.isEmpty(),
                        onToggleExpand = { viewModel.toggleMySetsExpanded() }
                    )
                }
                item {
                    AnimatedVisibility(visible = isMySetsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (mySets.isNotEmpty()) {
                                mySets.forEach { setSummary ->
                                    SharedCardSetSummaryItem(
                                        summary = setSummary,
                                        isMySet = true,
                                        onViewClick = { onNavigateToBrowseMySet(setSummary.id) },
                                        onEditClick = { onNavigateToEditSet(setSummary.id) },
                                        onDeleteClick = { viewModel.requestDeleteSet(setSummary.id, setSummary.name_uk) }
                                    )
                                }
                            } else if (!isLoadingMySets) {
                                Text(
                                    "У вас ще немає створених наборів.",
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (mySets.isEmpty() && !isLoadingMySets || mySets.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "Публічні набори",
                        isExpanded = isPublicSetsExpanded,
                        isLoading = isLoadingPublicSets && publicSets.isEmpty(),
                        onToggleExpand = { viewModel.togglePublicSetsExpanded() }
                    )
                }
                item {
                    AnimatedVisibility(visible = isPublicSetsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (publicSets.isNotEmpty()) {
                                publicSets.forEach { setSummary ->
                                    SharedCardSetSummaryItem(
                                        summary = setSummary,
                                        isMySet = false,
                                        onViewClick = { onNavigateToViewPublicSet(setSummary.id) },
                                        onEditClick = null,
                                        onDeleteClick = null
                                    )
                                }
                            } else if (!isLoadingPublicSets) {
                                Text(
                                    "Публічних наборів поки що немає.",
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (publicSets.isEmpty() && !isLoadingPublicSets || publicSets.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedAppScaffold(
    onNavigateToTranslate: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToWordList: () -> Unit,
    onNavigateToCreateSet: () -> Unit,
    onLogoutClick: () -> Unit,
    onNavigateToBrowseSet: (String) -> Unit,
    onNavigateToEditSet: (String) -> Unit,
    setsViewModelFactory: SetsViewModelFactory,
    wordListViewModelFactory: WordListViewModelFactory,
    articleViewModelFactory: ArticleViewModelFactory,
    onNavigateToViewArticle: (articleId: String) -> Unit,
    onNavigateToCreateArticle: () -> Unit,
    onNavigateToEditArticle: (articleId: String) -> Unit
) {
    val navController: NavHostController = rememberNavController()

    Scaffold(
        bottomBar = { MyBottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                // Твій AuthenticatedMainScreen, який ти надав
                AuthenticatedMainScreen(
                    navController = navController, // Передаємо, якщо AuthenticatedMainScreen має власну логіку навігації
                    onNavigateToTranslate = onNavigateToTranslate,
                    onNavigateToPractice = onNavigateToPractice,
                    onNavigateToWordList = onNavigateToWordList,
                    onLogoutClick = onLogoutClick,
                    wordListViewModelFactory = wordListViewModelFactory
                )
            }
            composable(BottomNavItem.Sets.route) {
                val viewModel: SetsViewModel = viewModel(factory = setsViewModelFactory)
                SetsScreen( // Ти вже надав цей код
                    viewModel = viewModel,
                    onNavigateToCreateSet = onNavigateToCreateSet,
                    onNavigateToViewPublicSet = { setId -> onNavigateToBrowseSet(setId) },
                    onNavigateToBrowseMySet = { setId -> onNavigateToBrowseSet(setId) },
                    onNavigateToEditSet = { setId -> onNavigateToEditSet(setId) }
                )
            }
            composable(BottomNavItem.Articles.route) {
                    val articleViewModel: ArticleViewModel = viewModel(factory = articleViewModelFactory)
                    ArticlesListScreen( // Розкоментуй та підключи
                        viewModel = articleViewModel,
                        onViewArticle = onNavigateToViewArticle,
                        onCreateArticle = onNavigateToCreateArticle,
                        onEditArticle = onNavigateToEditArticle
                    )
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("ArticlesListScreen (Section) Placeholder") // Тимчасова заглушка
                }
            }
        }
    }
}