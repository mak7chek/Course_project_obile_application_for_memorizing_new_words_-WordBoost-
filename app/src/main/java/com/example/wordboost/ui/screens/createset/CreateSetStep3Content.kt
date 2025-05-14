package com.example.wordboost.ui.screens.createset

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.wordboost.viewmodel.CreateSetViewModel

@Composable
fun CreateSetStep3Content(viewModel: CreateSetViewModel, modifier: Modifier = Modifier) {
    val currentOriginalWord by viewModel.currentOriginalWord
    val currentTranslationWord by viewModel.currentTranslationWord
    val editingWordUiId by viewModel.editingWordUiId
    val isLoadingWordTranslation by viewModel.isLoading

    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = currentOriginalWord,
            onValueChange = { viewModel.onOriginalWordChanged(it) },
            label = { Text("Англійське слово (Оригінал)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = currentTranslationWord,
            onValueChange = { viewModel.onTranslationWordChanged(it) },
            label = { Text("Український переклад") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                viewModel.addOrUpdateTemporaryWord()
                focusManager.clearFocus()
            }),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { viewModel.translateSetCreationInputFields() },
            modifier = Modifier.fillMaxWidth(),
            enabled = (currentOriginalWord.isNotBlank() xor currentTranslationWord.isNotBlank()) && !isLoadingWordTranslation
        ) {
            if (isLoadingWordTranslation && (currentOriginalWord.isNotBlank() xor currentTranslationWord.isNotBlank())) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Перекласти")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                viewModel.addOrUpdateTemporaryWord()
                focusManager.clearFocus()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = currentOriginalWord.isNotBlank() && currentTranslationWord.isNotBlank() && !isLoadingWordTranslation
        ) {
            Text(if (editingWordUiId == null) "Додати слово до набору" else "Зберегти зміни")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.temporaryWordsList.isNotEmpty()) {
            Text("Додані слова (${viewModel.temporaryWordsList.size}):", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(
                    items = viewModel.temporaryWordsList.toList(),
                    key = { wordItem -> wordItem.id }
                ) { wordItem ->
                    TemporaryWordListItem(
                        wordItem = wordItem,
                        onEdit = { viewModel.startEditTemporaryWord(wordItem) },
                        onDelete = { viewModel.deleteTemporaryWord(wordItem) },
                        isBeingEdited = editingWordUiId == wordItem.id
                    )
                    Divider()
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Ще не додано жодного слова.", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.proceedToStep4() },
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel.temporaryWordsList.isNotEmpty() && !isLoadingWordTranslation
        ) {
            Text("Далі до налаштувань видимості")
        }
    }
}