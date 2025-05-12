package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordboost.viewmodel.BrowseSharedSetViewModel
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.unit.IntOffset
import  com.example.wordboost.ui.components.SharedWordDisplayCard



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseSharedSetScreen(
    viewModel: BrowseSharedSetViewModel,
    onBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val setDetailsWithWords by viewModel.setDetailsWithWords.collectAsState()
    val currentDisplayWord by viewModel.currentDisplayWordItem.collectAsState()
    val showTranslation by viewModel.showTranslation.collectAsState()
    val isSetCompleted by viewModel.isSetCompleted.collectAsState()

    val totalWords by viewModel.totalWordsInSet.collectAsState()
    val currentIndexDisplay by viewModel.currentWordListPositionDisplay.collectAsState()
    val progress = if (totalWords > 0) {
        ((currentIndexDisplay.toFloat()) / totalWords.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }


    Scaffold(
        topBar = {
            TopAppBar(
               title = { Text(setDetailsWithWords?.setInfo?.name_uk ?: "Завантаження...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && currentDisplayWord == null && !isSetCompleted) { // Початкове завантаження
                CircularProgressIndicator()
            } else if (isSetCompleted || (currentDisplayWord == null && !isLoading && setDetailsWithWords != null)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Набір пройдено",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (setDetailsWithWords?.words.isNullOrEmpty()) "Цей набір порожній." else "Набір пройдено!",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    setDetailsWithWords?.setInfo?.let {
                        Text("Назва: ${it.name_uk}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                    Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) {
                        Text("Повернутися до списку")
                    }
                }
            } else if (currentDisplayWord != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (totalWords > 0) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Text(
                            // +1 до currentIndexDisplay для відображення 1-based індексації
                            "Слово ${currentIndexDisplay + 1} з $totalWords",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.1f))

                    SharedWordDisplayCard(
                        wordItem = currentDisplayWord!!,
                        showTranslation = showTranslation,
                        onSwipeUp = { viewModel.onWordSwipedUp(currentDisplayWord!!) },
                        onSwipeDown = { viewModel.onWordSwipedDown(currentDisplayWord!!) },
                        onReplaySound = { viewModel.replayWordSound() },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.weight(0.1f))

                    Text(
                        "Свайп ВГОРУ, щоб додати до вивчення.\nСвайп ВНИЗ, щоб проігнорувати.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp)) // Невеликий відступ знизу
                }
            } else {
                Text("Немає слів для відображення або сталася помилка.", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

