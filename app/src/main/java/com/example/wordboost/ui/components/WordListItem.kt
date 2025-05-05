package com.example.wordboost.ui.components

import androidx.compose.foundation.Image
import com.example.wordboost.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.Word
import com.example.wordboost.viewmodel.WordDisplayItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListItem(
    item: WordDisplayItem,
    onItemClick: (Word) -> Unit,
    onEditClick: (Word) -> Unit,
    onResetClick: (Word) -> Unit,
    onDeleteClick: (Word) -> Unit,
    onPlaySound: (Word) -> Unit,
    formatDate: (Long) -> String,
    wordProgress: Float
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onItemClick(item.word) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column( // Ліва частина картки: переклад+динамік, текст слова, група, дата
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Кнопка прослуховування
                    IconButton(
                        onClick = { onPlaySound(item.word) }, // ViewModel вирішить, що озвучити
                        modifier = Modifier.size(24.dp) // Стандартний розмір кнопки-іконки
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.volume_up_svgrepo_com),
                            contentDescription = "Прослухати ${item.word.translation}", // Опис для accessibility
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp) // Розмір самої іконки
                        )
                    }
                    // !!! ДОДАНО SPACER ДЛЯ ВІДСТУПУ МІЖ ІКОНКОЮ ТА ТЕКСТОМ !!!
                    Spacer(modifier = Modifier.width(8.dp)) // Відступ після іконки

                    // Текст перекладу (ТЕПЕР ОСНОВНИЙ - як просив користувач)
                    Text(
                        text = item.word.translation,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f) // Переклад займає доступний простір у цьому Row
                    )
                }

                // Оригінальне слово (ТЕПЕР ДРУГОРЯДНИЙ)
                Text(
                    text = item.word.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp)) // Відступ між текстами та групою

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        // Група
                        Text(
                            text = "Група: ${item.groupName ?: "Без групи"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = formatDate(item.word.nextReview),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    BatteryProgressIndicator(
                        progress = wordProgress,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню для слова ${item.word.text}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Редагувати") },
                        onClick = { onEditClick(item.word); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Скинути статистику") },
                        onClick = { onResetClick(item.word); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Видалити") },
                        onClick = { onDeleteClick(item.word); showMenu = false }
                    )
                }
            }
        }
    }
}