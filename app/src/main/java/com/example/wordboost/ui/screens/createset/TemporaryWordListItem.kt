package com.example.wordboost.ui.screens.createset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.SharedSetWordItem

@Composable
fun TemporaryWordListItem(
    wordItem: SharedSetWordItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isBeingEdited: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isBeingEdited) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(wordItem.originalText, style = MaterialTheme.typography.titleMedium)
            Text(wordItem.translationText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row {
            IconButton(onClick = onEdit, enabled = !isBeingEdited) {
                Icon(Icons.Default.Edit, contentDescription = "Редагувати слово")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Видалити слово", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}