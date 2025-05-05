package com.example.wordboost.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.tts.TextToSpeechService
// Імпортуємо ViewModel та Factory
import com.example.wordboost.viewmodel.PracticeViewModel
import com.example.wordboost.viewmodel.PracticeViewModelFactory
// Імпортуємо компоненти
import com.example.wordboost.ui.components.PracticeCard

import kotlinx.coroutines.launch
import kotlin.math.roundToInt
// import kotlin.random.Random // Random тепер використовується у ViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Для Back button
import androidx.compose.ui.text.style.TextAlign


enum class DragAnchors {
    Start,
    Know,
    DontKnow
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PracticeScreen(
    practiceRepo: PracticeRepository,
    ttsService: TextToSpeechService,
    onBack: () -> Unit
) {
    val viewModel: PracticeViewModel = viewModel(factory = PracticeViewModelFactory(practiceRepo,ttsService))

    val words by viewModel.words.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isFlipped by viewModel.isFlipped.collectAsState()
    val isReverse by viewModel.isReverse.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val defaultActionSize = 100.dp
    val actionSizePx = with(density) { defaultActionSize.toPx() }

    val anchoredDraggableState = remember {
        AnchoredDraggableState<DragAnchors>(
            initialValue = DragAnchors.Start,
            anchors = DraggableAnchors<DragAnchors> { DragAnchors.Start at 0f },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = exponentialDecay()
        )
    }

    LaunchedEffect(isFlipped, actionSizePx) {
        val newAnchors = if (isFlipped) {
            DraggableAnchors<DragAnchors> {
                DragAnchors.Start at 0f
                DragAnchors.Know at actionSizePx
                DragAnchors.DontKnow at -actionSizePx
            }
        } else {
            DraggableAnchors<DragAnchors> {
                DragAnchors.Start at 0f
            }
        }
        anchoredDraggableState.updateAnchors(newAnchors)
        if (!isFlipped && anchoredDraggableState.currentValue != DragAnchors.Start) {
            coroutineScope.launch { anchoredDraggableState.snapTo(DragAnchors.Start) }
        }
    }
    LaunchedEffect(anchoredDraggableState.targetValue) {
        val target = anchoredDraggableState.targetValue
        if (isFlipped) {
            when (target) {
                DragAnchors.Know -> {
                    viewModel.processSwipeResult(5)
                }
                DragAnchors.DontKnow -> {
                    viewModel.processSwipeResult(0)
                }
                DragAnchors.Start -> {

                }
            }
        }
    }

    BackHandler(enabled = true) { onBack() }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScopeSnackbar = rememberCoroutineScope()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            coroutineScopeSnackbar.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearStatusMessage()
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Практика слів") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {

            if (isLoading && words.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (words.isEmpty() && !isLoading) {
                Text("Слів для практики поки немає.\nДодайте слова або змініть статус існуючих.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else {
                words.getOrNull(currentIndex)?.let { word ->
                    PracticeCard(
                        word = word,
                        isFlipped = isFlipped,
                        isReverse = isReverse,
                        onFlip = { viewModel.flipCard() },
                        onReplaySound = { wordToSpeak -> viewModel.replayWordSound(wordToSpeak) }, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .offset {
                                IntOffset(
                                    x = anchoredDraggableState.requireOffset().roundToInt(),
                                    y = 0
                                )
                            }
                            .anchoredDraggable(
                                state = anchoredDraggableState,
                                orientation = Orientation.Horizontal,
                                enabled = isFlipped
                            )
                    )
                }
            }
            if (words.isNotEmpty() && !isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 0.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / words.size.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${currentIndex + 1} / ${words.size}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}