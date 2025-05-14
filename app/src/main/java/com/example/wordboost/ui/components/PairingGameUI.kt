package com.example.wordboost.ui.components

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
            return
        }

        if (selectedCard != null && selectedCard?.side == card.side) {
            selectedCard = card
            if (card.side == PairingSide.Translation) {
                onSpeakTranslationClick(card.word.translation)
            }
            return
        }


        if (selectedCard == null) {
            selectedCard = card
            if (card.side == PairingSide.Translation) {
                onSpeakTranslationClick(card.word.translation)
            }
        } else {
            val first = selectedCard!!
            val second = card

            if (first.word.id == second.word.id && first.side != second.side) {
                pairingCardsState = pairingCardsState.map {
                    if (it.word.id == first.word.id) it.copy(isMatched = true) else it
                }
                onPairMatched(first.word.id)
                val matchedTranslation = if (first.side == PairingSide.Translation) first.word.translation else second.word.translation
                if (matchedTranslation.isNotBlank()) {
                    onSpeakTranslationClick(matchedTranslation)
                }

                selectedCard = null

            } else {
                mismatchedCardIds = setOf(first.word.id, second.word.id)
                val clickedTranslation = if (second.side == PairingSide.Translation) second.word.translation else if (first.side == PairingSide.Translation) first.word.translation else ""
                if (clickedTranslation.isNotBlank()) {
                    onSpeakTranslationClick(clickedTranslation)
                }
            }
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            "Режим Пар",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pairingCardsState.filter { it.side == PairingSide.Original }, key = { it.word.id }) { card ->
                    val isCardSelected = selectedCard?.word?.id == card.word.id && selectedCard?.side == card.side
                    val isCardMismatched = mismatchedCardIds.contains(card.word.id)

                    val borderColor by animateColorAsState(
                        targetValue = if (isCardMismatched) MaterialTheme.colorScheme.error else if (isCardSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(300), label = "borderColor"
                    )
                    val cardColor by animateColorAsState(
                        targetValue = if (card.isMatched) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceBright,
                        animationSpec = tween(300), label = "cardColor"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 2f)
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = !card.isMatched && !isCardMismatched) { onCardClick(card) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                card.word.text,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColorFor(backgroundColor = cardColor)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
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
                        targetValue = if (card.isMatched) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceBright,
                        animationSpec = tween(300), label = "cardColor"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 2f)
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = !card.isMatched && !isCardMismatched) { onCardClick(card) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(2.dp)
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

        if (isGameCompleted) {
            Text("Партію завершено!", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}