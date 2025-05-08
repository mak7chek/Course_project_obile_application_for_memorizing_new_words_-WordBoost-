// ui.components/PairingGameUI.kt
package com.example.wordboost.ui.components

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.Word
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Припускаємо, що ці Composable визначені та доступні
// import com.example.wordboost.ui.theme.lightGradientTop
// import com.example.wordboost.ui.theme.lightGradientBottom
// import com.example.wordboost.ui.theme.darkGradientTop
// import com.example.wordboost.ui.theme.darkGradientBottom
// import androidx.compose.foundation.isSystemInDarkTheme


private enum class PairingSide {
    Original,
    Translation
}

private data class PairingCardState(
    val word: Word,
    val side: PairingSide,
    val isSelected: Boolean = false,
    val isMatched: Boolean = false,
    val isMismatched: Boolean = false
)

@Composable
fun PairingGameUI(
    words: List<Word>,
    onPairMatched: (String) -> Unit,
    onPairingFinished: () -> Unit,
    onSpeakTranslationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pairingCardsState by remember(words) {
        val originalCards = words.map { PairingCardState(it, PairingSide.Original) }
        val translationCards = words.map { PairingCardState(it, PairingSide.Translation) }
        mutableStateOf((originalCards + translationCards).shuffled(Random(System.currentTimeMillis())))
    }

    var selectedCard by remember { mutableStateOf<PairingCardState?>(null) }
    var mismatchedCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val isGameCompleted = remember(pairingCardsState) {
        pairingCardsState.all { it.isMatched } && words.isNotEmpty()
    }


    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(mismatchedCardIds) {
        if (mismatchedCardIds.isNotEmpty()) {
            delay(600)
            mismatchedCardIds = emptySet()
            selectedCard = null
        }
    }

    LaunchedEffect(isGameCompleted) {
        if (isGameCompleted) {
            Log.d("PairingGameUI", "Game Completed. Calling onPairingFinished.")
            delay(800)
            onPairingFinished()
        }
    }


    fun onCardClick(card: PairingCardState) {
        if (mismatchedCardIds.isNotEmpty() || card.isMatched) {
            return
        }

        if (selectedCard != null && selectedCard?.word?.id == card.word.id && selectedCard?.side == card.side) {
            selectedCard = null
            Log.d("PairingGameUI_Click", "Clicked already selected card. Deselecting.")
            return
        }

        if (selectedCard != null && selectedCard?.side == card.side) {
            selectedCard = card
            Log.d("PairingGameUI_Click", "Clicked card of the same side as selected. Selecting new card.")
            if (card.side == PairingSide.Translation) {
                Log.d("PairingGameUI_Click", "Clicked Translation card '${card.word.translation}'. Calling onSpeakTranslationClick.")
                onSpeakTranslationClick(card.word.translation)
            } else {
                Log.d("PairingGameUI_Click", "Clicked Original card '${card.word.text}'. No speech expected.")
            }
            return
        }


        if (selectedCard == null) {
            selectedCard = card
            Log.d("PairingGameUI_Click", "First click. Selecting card.")
            if (card.side == PairingSide.Translation) {
                Log.d("PairingGameUI_Click", "First click on Translation card '${card.word.translation}'. Calling onSpeakTranslationClick.")
                onSpeakTranslationClick(card.word.translation)
            } else {
                Log.d("PairingGameUI_Click", "First click on Original card '${card.word.text}'. No speech expected.")
            }
        } else {
            val first = selectedCard!!
            val second = card

            if (first.word.id == second.word.id && first.side != second.side) {
                Log.d("PairingGameUI", "Pair Matched: ${first.word.text}")
                Log.d("PairingGameUI_Match", "Pair Matched for '${first.word.text}'.")

                pairingCardsState = pairingCardsState.map {
                    if (it.word.id == first.word.id) it.copy(isMatched = true) else it
                }
                onPairMatched(first.word.id)
                val matchedTranslation = if (first.side == PairingSide.Translation) first.word.translation else second.word.translation
                if (matchedTranslation.isNotBlank()) {
                    Log.d("PairingGameUI_Match", "Matched pair found. Speaking translation: '$matchedTranslation'. Calling onSpeakTranslationClick.")
                    onSpeakTranslationClick(matchedTranslation)
                } else {
                    Log.d("PairingGameUI_Match", "Matched pair found, but translation is blank. Not speaking.")
                }

                selectedCard = null


            } else {
                Log.d("PairingGameUI", "Pair Mismatched: ${first.word.text} (${first.side}) vs ${second.word.text} (${second.side})")
                Log.d("PairingGameUI_Match", "Pair Mismatched for '${first.word.text}' and '${second.word.text}'.")

                mismatchedCardIds = setOf(first.word.id, second.word.id)
                val clickedTranslation = if (second.side == PairingSide.Translation) second.word.translation else if (first.side == PairingSide.Translation) first.word.translation else ""
                if (clickedTranslation.isNotBlank()) {
                    Log.d("PairingGameUI_Match", "Mismatched pair. Speaking translation of one of the clicked cards: '$clickedTranslation'. Calling onSpeakTranslationClick.")
                    onSpeakTranslationClick(clickedTranslation)
                } else {
                    Log.d("PairingGameUI_Match", "Mismatched pair, but no translation found among clicked cards. Not speaking.")
                }
            }
        }
    }


    // --- UI Відображення ---
    Column(
        modifier = modifier
            .fillMaxSize()
            // !!! Додаємо градієнтний фон до колонки гри !!!
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh, // Колір зверху
                        MaterialTheme.colorScheme.background // Колір знизу
                        // Ви можете обрати інші кольори з вашої теми
                    )
                )
            )
            .padding(16.dp), // Загальний відступ навколо вмісту екрану
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Вирівнюємо вміст колонки зверху
    ) {
        Text(
            "Режим Пар",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp) // Додано нижній відступ
        )

        // !!! Видалено Spacer-и з вагою, повертаємо картки на початок вільного простору !!!
        // Spacer(modifier = Modifier.weight(1f)) // Видалено

        // Розміщуємо картки в двох стовпцях
        Row(
            modifier = Modifier
                .fillMaxWidth() // Займаємо всю доступну ширину
                // !!! Видалено вагу з Row, він займатиме місце за потребою або fillMaxHeight !!!
                // .weight(2f)
                .fillMaxHeight() // Займаємо решту доступної висоти
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top // Вирівнювання елементів Row по верхньому краю
        ) {
            // Стовпець Оригіналів
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Займає половину ширини Row
                    .padding(end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, // Центрує елементи в стовпці по горизонталі
                verticalArrangement = Arrangement.spacedBy(8.dp) // Відступ між картками в стовпці
            ) {
                items(pairingCardsState.filter { it.side == PairingSide.Original }, key = { it.word.id }) { card ->
                    val isCardSelected = selectedCard?.word?.id == card.word.id && selectedCard?.side == card.side
                    val isCardMismatched = mismatchedCardIds.contains(card.word.id)

                    val borderColor by animateColorAsState(
                        targetValue = if (isCardMismatched) MaterialTheme.colorScheme.error else if (isCardSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(300), label = "borderColor"
                    )
                    val cardColor by animateColorAsState(
                        // !!! Змінено на surfaceBright для світлішого кольору !!!
                        targetValue = if (card.isMatched) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceBright,
                        animationSpec = tween(300), label = "cardColor"
                    )


                    Card(
                        modifier = Modifier
                            .fillMaxWidth() // Картка займає всю ширину свого стовпця
                            .aspectRatio(3f / 2f) // Збереження пропорцій картки
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = !card.isMatched && !isCardMismatched) { onCardClick(card) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(2.dp) // Збільшено тінь
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(8.dp), // Додано внутрішній padding
                            contentAlignment = Alignment.Center // Центруємо текст всередині Box
                        ) {
                            Text(
                                card.word.text,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium, // Стиль тексту з теми
                                color = contentColorFor(backgroundColor = cardColor) // Колір тексту контрастний до фону
                            )
                        }
                    }
                }
            }

            // Стовпець Перекладів
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Займає половину ширини Row
                    .padding(start = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, // Центрує елементи в стовпці по горизонталі
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pairingCardsState.filter { it.side == PairingSide.Translation }, key = { it.word.id }) { card ->

                    val isCardSelected = selectedCard?.word?.id == card.word.id && selectedCard?.side == card.side
                    val isCardMismatched = mismatchedCardIds.contains(card.word.id)

                    val borderColor by animateColorAsState(
                        targetValue = if (isCardMismatched) MaterialTheme.colorScheme.error else if (isCardSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(300), label = "borderColor"
                    )
                    val cardColor by animateColorAsState(
                        // !!! Змінено на surfaceBright для світлішого кольору !!!
                        targetValue = if (card.isMatched) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceBright,
                        animationSpec = tween(300), label = "cardColor"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 2f) // Збереження пропорцій картки
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = !card.isMatched && !isCardMismatched) { onCardClick(card) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(2.dp) // Збільшено тінь
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                card.word.translation,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColorFor(backgroundColor = cardColor)
                            )
                        }
                    }
                }
            }
        }

        // !!! Видалено Spacer-и з вагою !!!
        // Spacer(modifier = Modifier.weight(1f)) // Видалено

        // Індикатор завершення гри (показується внизу, якщо гра завершена)
        if (isGameCompleted) {
            Text("Партію завершено!", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp)) // Відступ після повідомлення
        }
    }
}