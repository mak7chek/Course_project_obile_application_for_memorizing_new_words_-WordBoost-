package com.example.wordboost.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController // NavController тут потрібен для внутрішньої навігації, якщо буде
import com.example.wordboost.viewmodel.WordListViewModel
import com.example.wordboost.viewmodel.WordListViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedMainScreen(
    navController: NavController,
    onNavigateToTranslate: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToWordList: () -> Unit,
    onLogoutClick: () -> Unit,
    wordListViewModelFactory: WordListViewModelFactory
) {
    val wordListViewModel: WordListViewModel = viewModel(factory = wordListViewModelFactory)
    val wordsToLearnNowCount by wordListViewModel.wordsToLearnNowCount.collectAsState()
    val wordsInShortTermMemoryCount by wordListViewModel.wordsInShortTermMemoryCount.collectAsState()
    val learnedWordsCount by wordListViewModel.learnedWordsCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WordBoost Головна") },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Вийти")
                    }
                }
            )
        } ,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 16.dp), // Горизонтальний паддінг для всього контенту
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp)) // Відступ зверху від TopAppBar

                Text(
                    text = "Вітаємо у WordBoost!",
                    style = MaterialTheme.typography.headlineMedium,
                    // Відступ знизу буде керуватися Arrangement.spacedBy або наступним Spacer
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp) // Зменшений відступ знизу
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatisticCard(modifier = Modifier.weight(1f), title = "Вчити зараз", count = wordsToLearnNowCount, containerColor = MaterialTheme.colorScheme.primaryContainer)
                    StatisticCard(modifier = Modifier.weight(1f), title = "Знаю", count = wordsInShortTermMemoryCount, containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    StatisticCard(modifier = Modifier.weight(1f), title = "Навчився", count = learnedWordsCount, containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onNavigateToPractice,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(72.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Почати практику",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Розміщення FAB та кнопки "Мій словник" внизу екрану
            // Цей Box буде поверх Column з основним контентом
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Притискаємо до низу по центру Box від Scaffold
                    .padding(bottom = 16.dp) // Відступ від краю екрану або нижньої навігації
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, // Розносимо по краях
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка "Мій словник" - тепер зліва
                    TextButton(
                        onClick = onNavigateToWordList,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Мій словник",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Мій словник", style = MaterialTheme.typography.titleMedium)
                    }

                    // FAB "Додати слово" - тепер справа
                    FloatingActionButton(
                        onClick = onNavigateToTranslate,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Швидкий переклад/додавання")
                    }
                }
            }
        }
    }
}

// StatisticCard залишається без змін
@Composable
fun StatisticCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = contentColorFor(backgroundColor = containerColor)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = contentColorFor(backgroundColor = containerColor)
            )
        }
    }
}