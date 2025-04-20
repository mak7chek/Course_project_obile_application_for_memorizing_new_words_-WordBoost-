package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.local.AppDatabase
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.translation.RealTranslator
import com.example.wordboost.ui.components.GroupSelectorDialog
import com.example.wordboost.data.firebase.Group
import androidx.compose.runtime.rememberCoroutineScope
import androidx.room.Room
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen() {
    val context = LocalContext.current

    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "wordboost-db")
            .fallbackToDestructiveMigration(true)
            .build()
    }
    val cacheDao = db.cacheDao()

    val firebaseRepo    = remember { FirebaseRepository() }
    val realTranslator  = remember { RealTranslator() }
    val translationRepo = remember { TranslationRepository(firebaseRepo, cacheDao, realTranslator) }
    val scope = rememberCoroutineScope()

    // Input fields
    var ukText by remember { mutableStateOf("") }
    var enText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Groups
    var groups by remember { mutableStateOf(listOf<Group>()) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }

    // Load groups once
    LaunchedEffect(Unit) {
        firebaseRepo.getGroups { fetched -> groups = fetched }
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("WordBoost") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ukrainian input
            OutlinedTextField(
                value = ukText,
                onValueChange = { ukText = it },
                label = { Text("Українське слово") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            // English input
            OutlinedTextField(
                value = enText,
                onValueChange = { enText = it },
                label = { Text("English word") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            // Translate button: enabled only when exactly one field is non-empty
            Button(
                onClick = {
                    statusMessage = null
                    val originalText = ukText.ifBlank { enText }
                    val targetLang = if (ukText.isNotBlank()) "EN" else "UK"
                    translationRepo.translate(originalText, targetLang) { result ->
                        result?.let { translated ->
                            if (ukText.isNotBlank()) {
                                enText = translated
                            } else {
                                ukText = translated
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (ukText.isNotBlank() xor enText.isNotBlank())
            ) {
                Text("Перекласти")
            }

            // Status message
            statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            // Save buttons row - only when both fields have text
            if (ukText.isNotBlank() && enText.isNotBlank()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Add to dictionary
                    Button(
                        onClick = {
                            statusMessage = null
                            val original = ukText
                            val translated = enText
                            firebaseRepo.getTranslation(original) { existing ->
                                if (existing != null) {
                                    statusMessage = "Слово вже в словнику"
                                } else {
                                    val id = UUID.nameUUIDFromBytes((original + translated).toByteArray()).toString()
                                    val word = Word(
                                        id = id,
                                        text = original,
                                        translation = translated,
                                        dictionaryId = "",
                                        knowledgeLevel = 0,
                                        status = "new",
                                        lastReviewed = 0L,
                                        nextReview = 0L
                                    )
                                    firebaseRepo.saveWord(word) { success ->
                                        statusMessage = if (success) "Додано до словника" else "Помилка збереження"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Додати в словник")
                    }

                    // Add to group
                    Button(
                        onClick = { showGroupDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Додати до групи")
                    }
                }
            }

            // Group selector dialog
            if (showGroupDialog) {
                GroupSelectorDialog(
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    onGroupSelected = { selectedGroupId = it },
                    onCreateGroup = { name ->
                        firebaseRepo.createGroup(name) { success -> if (success) firebaseRepo.getGroups { groups = it } }
                    },
                    onConfirm = {
                        statusMessage = null
                        val original = ukText
                        val translated = enText
                        val groupId = selectedGroupId.orEmpty()
                        firebaseRepo.getTranslation(original) { existing ->
                            if (existing != null) {
                                firebaseRepo.getWordObject(original) { wordObj ->
                                    wordObj?.let { existingWord ->
                                        val updated = existingWord.copy(dictionaryId = groupId)
                                        firebaseRepo.saveWord(updated) { success ->
                                            statusMessage = if (success) "Група оновлена" else "Помилка оновлення"
                                        }
                                    }
                                }
                            } else {
                                val id = UUID.nameUUIDFromBytes((original + translated + groupId).toByteArray()).toString()
                                val newWord = Word(id, original, translated, groupId, 0, "new", 0L, 0L)
                                firebaseRepo.saveWord(newWord) { success ->
                                    statusMessage = if (success) "Додано до групи" else "Помилка збереження"
                                }
                            }
                        }
                        showGroupDialog = false
                        selectedGroupId = null
                    },
                    onDismissRequest = { showGroupDialog = false; selectedGroupId = null }
                )
            }
        }
    }
}