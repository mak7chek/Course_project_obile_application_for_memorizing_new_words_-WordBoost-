package com.example.wordboost.ui.components
import android.util.Log // Імпорт
import androidx.compose.animation.animateColorAsState // Імпорт для анімації кольору
import androidx.compose.animation.core.animateFloatAsState // Може знадобитись, але зараз не використовується
import androidx.compose.animation.core.tween // Імпорт для анімації
import androidx.compose.foundation.border // Імпорт для рамки
import androidx.compose.foundation.clickable // Імпорт
import androidx.compose.foundation.layout.* // Імпорт
import androidx.compose.foundation.lazy.LazyColumn // Імпорт
import androidx.compose.foundation.lazy.items // Імпорт
import androidx.compose.foundation.shape.RoundedCornerShape // Імпорт
import androidx.compose.material3.* // Імпорт Material3
import androidx.compose.runtime.* // Імпорт Composable та станів
import androidx.compose.ui.Alignment // Імпорт
import androidx.compose.ui.Modifier // Імпорт
import androidx.compose.ui.graphics.Color // Імпорт Color
import androidx.compose.ui.text.style.TextAlign // Імпорт
import androidx.compose.ui.unit.dp // Імпорт dp
import com.example.wordboost.data.model.Word // Імпорт Word
import kotlinx.coroutines.delay // Імпорт delay для затримки
import kotlinx.coroutines.launch // Імпорт launch для корутин
import kotlin.random.Random


private enum class PairingSide {
    Original,
    Translation
}

private data class PairingCardState(
    val word: Word, // Слово, до якого відноситься елемент
    val side: PairingSide, // Сторона (Original чи Translation)
    val isSelected: Boolean = false, // Чи вибрана ця картка
    val isMatched: Boolean = false, // Чи співпала ця пара
    val isMismatched: Boolean = false // Чи була ця картка в неправильній парі
)

@Composable
fun PairingGameUI(
    words: List<Word>, // Список слів для гри в цій партії
    onPairMatched: (String) -> Unit, // Колбек при співпадінні пари (передаємо ID слова)
    onPairingFinished: () -> Unit, // Колбек при завершенні гри для партії
    onSpeakTranslationClick: (String) -> Unit, // Колбек для кліку на переклад для озвучення
    modifier: Modifier = Modifier // Приймаємо модифікатор ззовні
) {
    // --- Стан гри "Пари" ---
    // Список всіх карткових елементів (оригінали + переклади), перемішані
    var pairingCardsState by remember(words) {
        val originalCards = words.map { PairingCardState(it, PairingSide.Original) }
        val translationCards = words.map { PairingCardState(it, PairingSide.Translation) }
        // Перемішуємо обидва списки разом
        mutableStateOf((originalCards + translationCards).shuffled(Random(System.currentTimeMillis())))
    }

    // Стан вибраної картки (тільки одна може бути вибрана одночасно)
    var selectedCard by remember { mutableStateOf<PairingCardState?>(null) }

    // Стан для показу неправильної пари (для анімації червоного мигання)
    var mismatchedCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Відстежуємо, чи гра завершена (щоб показати кнопку продовження)
    val isGameCompleted = remember(pairingCardsState) {
        pairingCardsState.all { it.isMatched } && words.isNotEmpty()
    }


    // Використовуємо LaunchedEffect для скидання mismatchedCardIds після затримки (для червоного мигання)
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(mismatchedCardIds) {
        if (mismatchedCardIds.isNotEmpty()) {
            delay(600) // Затримка на 0.6 секунди для візуалізації помилки
            mismatchedCardIds = emptySet() // Скидаємо стан неправильної пари
            // Після неправильної пари скидаємо вибрану картку
            selectedCard = null
        }
    }

    // Коли всі пари співпали, повідомляємо ViewModel про завершення режиму пар
    LaunchedEffect(isGameCompleted) {
        if (isGameCompleted) {
            Log.d("PairingGameUI", "Game Completed. Calling onPairingFinished.")
            delay(800) // Коротка затримка перед переходом
            onPairingFinished() // Повідомляємо про завершення гри
        }
    }


    // --- Обробка кліку на картку ---
    fun onCardClick(card: PairingCardState) {
        // Ігноруємо кліки, якщо вже є неправильна пара, або картка вже співпала
        if (mismatchedCardIds.isNotEmpty() || card.isMatched) {
            return
        }

        // Якщо клікнули на вже вибрану картку, скидаємо вибір
        if (selectedCard != null && selectedCard?.word?.id == card.word.id && selectedCard?.side == card.side) {
            selectedCard = null
            // Log added
            Log.d("PairingGameUI_Click", "Clicked already selected card. Deselecting.")
            return
        }

        // Якщо клікнули на картку того ж типу (Оригінал/Переклад), що й вибрана перша
        if (selectedCard != null && selectedCard?.side == card.side) {
            // Скидаємо попередній вибір і вибираємо нову картку того ж типу
            selectedCard = card
            // Log added
            Log.d("PairingGameUI_Click", "Clicked card of the same side as selected. Selecting new card.")
            // Озвучуємо переклад, якщо нова вибрана картка - переклад
            if (card.side == PairingSide.Translation) {
                Log.d("PairingGameUI_Click", "Clicked Translation card '${card.word.translation}'. Calling onSpeakTranslationClick.")
                onSpeakTranslationClick(card.word.translation)
            } else {
                Log.d("PairingGameUI_Click", "Clicked Original card '${card.word.text}'. No speech expected.")
            }
            return // Виходимо після вибору
        }


        // Якщо картка ще не вибрана (перший клік у парі)
        if (selectedCard == null) {
            selectedCard = card
            // Log added
            Log.d("PairingGameUI_Click", "First click. Selecting card.")
            // Озвучуємо переклад, якщо клікнули на сторону перекладу
            if (card.side == PairingSide.Translation) {
                Log.d("PairingGameUI_Click", "First click on Translation card '${card.word.translation}'. Calling onSpeakTranslationClick.")
                onSpeakTranslationClick(card.word.translation)
            } else {
                Log.d("PairingGameUI_Click", "First click on Original card '${card.word.text}'. No speech expected.")
            }
        } else {
            // Друга картка вже вибрана, перевіряємо співпадіння
            val first = selectedCard!!
            val second = card

            if (first.word.id == second.word.id && first.side != second.side) {
                // !!! Співпадіння !!!
                Log.d("PairingGameUI", "Pair Matched: ${first.word.text}")
                Log.d("PairingGameUI_Match", "Pair Matched for '${first.word.text}'.")

                // Оновлюємо стан карток як співпавших
                pairingCardsState = pairingCardsState.map {
                    if (it.word.id == first.word.id) it.copy(isMatched = true) else it
                }
                // Повідомляємо ViewModel про співпадіння
                onPairMatched(first.word.id)
                // Озвучуємо переклад ЗНАЙДЕНОЇ пари (переклад будь-якої сторони)
                val matchedTranslation = if (first.side == PairingSide.Translation) first.word.translation else second.word.translation
                if (matchedTranslation.isNotBlank()) {
                    Log.d("PairingGameUI_Match", "Matched pair found. Speaking translation: '$matchedTranslation'. Calling onSpeakTranslationClick.")
                    onSpeakTranslationClick(matchedTranslation) // <<< Спеціально викликаємо озвучення перекладу ЗНАЙДЕНОЇ пари
                } else {
                    Log.d("PairingGameUI_Match", "Matched pair found, but translation is blank. Not speaking.")
                }


                // Скидаємо вибрану картку
                selectedCard = null


            } else {
                // !!! Неправильна Пара !!!
                Log.d("PairingGameUI", "Pair Mismatched: ${first.word.text} (${first.side}) vs ${second.word.text} (${second.side})")
                Log.d("PairingGameUI_Match", "Pair Mismatched for '${first.word.text}' and '${second.word.text}'.")

                // Встановлюємо стан неправильної пари для анімації червоного мигання
                mismatchedCardIds = setOf(first.word.id, second.word.id)
                // Озвучуємо переклад другої клікнутої картки, якщо це переклад. Або переклад першої, якщо друга - оригінал.
                val clickedTranslation = if (second.side == PairingSide.Translation) second.word.translation else if (first.side == PairingSide.Translation) first.word.translation else ""
                if (clickedTranslation.isNotBlank()) {
                    Log.d("PairingGameUI_Match", "Mismatched pair. Speaking translation of one of the clicked cards: '$clickedTranslation'. Calling onSpeakTranslationClick.")
                    onSpeakTranslationClick(clickedTranslation) // <<< Озвучуємо переклад однієї з клікнутих карток
                } else {
                    Log.d("PairingGameUI_Match", "Mismatched pair, but no translation found among clicked cards. Not speaking.")
                }


                // selectedCard скидається через LaunchedEffect після затримки
            }
        }
    }


    // --- UI Відображення ---
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally, // Центруємо вміст по горизонталі
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Режим Пар", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(8.dp))

        // Розміщуємо картки в двох стовпцях
        Row(
            modifier = Modifier
                .fillMaxWidth() // Займаємо всю доступну ширину
                .weight(1f), // Займаємо доступний простір по вертикалі
            horizontalArrangement = Arrangement.SpaceEvenly // Розподіляє простір рівномірно між стовпцями
        ) {
            // Стовпець Оригіналів
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Займає половину ширини Row
                    .padding(end = 4.dp) // Відступ між стовпцями
                ,
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
                        targetValue = if (card.isMatched) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                        animationSpec = tween(300), label = "cardColor"
                    )


                    Card(
                        modifier = Modifier
                            .fillMaxWidth() // Картка займає всю ширину свого стовпця
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = !card.isMatched && !isCardMismatched) { onCardClick(card) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Box(
                            // Збільшено padding для більшого розміру блоку
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp).fillMaxSize(), // Збільшено вертикальний padding
                            contentAlignment = Alignment.Center // Центруємо текст всередині Box
                        ) {
                            Text(card.word.text, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Стовпець Перекладів
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Займає половину ширини Row
                    .padding(start = 4.dp) // Відступ між стовпцями
                ,
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
                        targetValue = if (card.isMatched) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                        animationSpec = tween(300), label = "cardColor"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth() // Картка займає всю ширину свого стовпця
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = !card.isMatched && !isCardMismatched) { onCardClick(card) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp).fillMaxSize(), // Збільшено вертикальний padding
                            contentAlignment = Alignment.Center // Центруємо текст всередині Box
                        ) {
                            Text(card.word.translation, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


    }
}