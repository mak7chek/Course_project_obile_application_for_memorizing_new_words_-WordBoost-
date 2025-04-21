package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.repository.PracticeRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(practiceRepo: PracticeRepository) {
    var words by remember { mutableStateOf<List<Word>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var showTranslation by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Завантаження слів
    LaunchedEffect(Unit) {
        practiceRepo.getWordsForPractice { fetchedWords ->
            words = fetchedWords
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Практика") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (words.isEmpty()) {
                Text("Немає слів для практики")
            } else {
                val word = words[currentIndex]

                PracticeCard(
                    word = word,
                    showTranslation = showTranslation,
                    onToggleTranslation = { showTranslation = !showTranslation },
                    onRemember = {
                        practiceRepo.updateWordAfterPractice(word, true) {
                            statusMessage = "Молодець!"
                            moveToNext(words.size, currentIndex) {
                                currentIndex = it
                                showTranslation = false
                            }
                        }
                    },
                    onForget = {
                        practiceRepo.updateWordAfterPractice(word, false) {
                            statusMessage = "Спробуй ще раз!"
                            moveToNext(words.size, currentIndex) {
                                currentIndex = it
                                showTranslation = false
                            }
                        }
                    },
                    statusMessage = statusMessage
                )
            }
        }
    }
}

@Composable
fun PracticeCard(
    word: Word,
    showTranslation: Boolean,
    onToggleTranslation: () -> Unit,
    onRemember: () -> Unit,
    onForget: () -> Unit,
    statusMessage: String?
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = word.text,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                if (showTranslation) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = word.translation,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Button(onClick = onToggleTranslation) {
            Text(if (showTranslation) "Сховати переклад" else "Показати переклад")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onForget,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Не пам’ятаю")
            }

            Button(
                onClick = onRemember,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Пам’ятаю")
            }
        }

        statusMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

private fun moveToNext(size: Int, index: Int, onUpdate: (Int) -> Unit) {
    onUpdate((index + 1) % size)
}