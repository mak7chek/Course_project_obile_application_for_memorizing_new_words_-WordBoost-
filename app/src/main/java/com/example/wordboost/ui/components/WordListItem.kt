package com.example.wordboost.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.Word // Імпорт моделі даних
// Імпорт WordDisplayItem з ViewModel пакета, де він визначений
import com.example.wordboost.viewmodel.WordDisplayItem
// Імпорт вашої теми
import com.example.wordboost.ui.theme.Course_project_obile_application_for_memorizing_new_words_WordBoostTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListItem(
    item: WordDisplayItem,
    onItemClick: (Word) -> Unit, // Клік на елемент
    onEditClick: (Word) -> Unit, // Клік "Редагувати"
    onResetClick: (Word) -> Unit, // Клік "Скинути статистику"
    onDeleteClick: (Word) -> Unit, // Клік "Видалити"
    formatDate: (Long) -> String // Функція для форматування дати з ViewModel
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onItemClick(item.word) }, // Клік на картку викликає onItemClick
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
            Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text(
                    text = item.word.text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.word.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Група: ${item.groupName ?: "Без групи"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDate(item.word.nextReview), // Використовуємо передану функцію форматування
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box { // Box для позиціонування меню відносно іконки
                IconButton(onClick = { showMenu = true }) { // Клік на іконку відкриває меню
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
                    // DropdownMenuItem викликають відповідні колбеки та закривають меню
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