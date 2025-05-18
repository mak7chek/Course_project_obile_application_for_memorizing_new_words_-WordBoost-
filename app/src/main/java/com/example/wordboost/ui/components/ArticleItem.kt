package com.example.wordboost.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.wordboost.viewmodel.ArticleUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListItem(
    articleUiModel: ArticleUiModel,
    isMyArticle: Boolean,
    onViewClick: () -> Unit,
    onEditClick: (() -> Unit)?, // Може бути null для публічних не моїх статей
    onDeleteClick: (() -> Unit)? // Може бути null для публічних не моїх статей
) {
    val article = articleUiModel.article
    Card(
        onClick = onViewClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(article.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                article.authorName?.let {
                    Text("Автор: $it", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                }
                // Можна додати короткий опис або перші рядки статті, якщо потрібно
                // Text(article.content.take(100) + "...", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (articleUiModel.isRead) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (articleUiModel.isRead) "Прочитано" else "Не прочитано",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (articleUiModel.isRead) "Прочитано" else "Не прочитано",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


                if (article.published) {
                    Text("Публічна", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (isMyArticle) {
                Column {
                    onEditClick?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Filled.Edit, contentDescription = "Редагувати статтю", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    onDeleteClick?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Filled.Delete, contentDescription = "Видалити статтю", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}