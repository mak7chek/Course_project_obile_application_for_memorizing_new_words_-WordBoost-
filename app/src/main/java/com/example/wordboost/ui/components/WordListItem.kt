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
    formatDate: (Long) -> String
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