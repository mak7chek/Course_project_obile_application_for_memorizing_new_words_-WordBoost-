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
import androidx.room.Room
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.local.AppDatabase
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.translation.RealTranslator
import com.example.wordboost.ui.components.GroupSelectorDialog
import com.example.wordboost.data.firebase.Group

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

    val firebaseRepo = remember { FirebaseRepository() }
    val realTranslator = remember { RealTranslator() }
    val translationRepo = remember {
        TranslationRepository(firebaseRepo, cacheDao, realTranslator)
    }

    var ukText by remember { mutableStateOf("") }
    var enText by remember { mutableStateOf("") }
    var translationResult by remember { mutableStateOf("") }

    var groups by remember { mutableStateOf(listOf<Group>()) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        firebaseRepo.getGroups { fetched ->
            groups = fetched
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("WordBoost") })
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ukText,
                            onValueChange = {
                                ukText = it
                                if (it.isNotBlank()) enText = ""
                            },
                            label = { Text("Українське слово") },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enText.isBlank()
                        )
                        OutlinedTextField(
                            value = enText,
                            onValueChange = {
                                enText = it
                                if (it.isNotBlank()) ukText = ""
                            },
                            label = { Text("English word") },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = ukText.isBlank()
                        )
                        Button(
                            onClick = {
                                statusMessage = null
                                val textToTranslate = if (ukText.isNotBlank()) ukText else enText
                                val lang = if (ukText.isNotBlank()) "EN" else "UK"
                                translationRepo.translate(textToTranslate, lang) { result ->
                                    translationResult = result ?: "Немає перекладу"
                                    if (ukText.isNotBlank()) enText = translationResult
                                    else ukText = translationResult
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = (ukText.isNotBlank() xor enText.isNotBlank())
                        ) {
                            Text("Перекласти")
                        }
                    }
                }

                if (translationResult.isNotBlank()) {
                    Button(
                        onClick = { showGroupDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Додати до словника")
                    }
                }

                statusMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }

                if (showGroupDialog) {
                    GroupSelectorDialog(
                        groups = groups,
                        selectedGroupId = selectedGroupId,
                        onGroupSelected = { selectedGroupId = it },
                        onCreateGroup = { name ->
                            firebaseRepo.createGroup(name) { success ->
                                if (success) {
                                    firebaseRepo.getGroups { groups = it }
                                }
                            }
                        },
                        onConfirm = {
                            val original = if (ukText.isNotBlank()) ukText else enText
                            val translated = translationResult
                            firebaseRepo.saveTranslation(original, translated, selectedGroupId)
                            statusMessage = "Слово додано до групи"
                        },
                        onDismissRequest = { showGroupDialog = false }
                    )
                }
            }
        }
    )
}
