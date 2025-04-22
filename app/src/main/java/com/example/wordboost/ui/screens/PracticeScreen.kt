package com.example.wordboost.ui.screens


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.repository.PracticeRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

enum class DragAnchors {
    Start,
    Know,
    DontKnow
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PracticeScreen(practiceRepo: PracticeRepository) {
    var words by remember { mutableStateOf<List<Word>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var isFlipped by remember { mutableStateOf(false) }
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

    val currentMoveToNextWord by rememberUpdatedState { known: Boolean ->
        if (words.isEmpty()) return@rememberUpdatedState

        val wordToUpdate = words.getOrNull(currentIndex) ?: return@rememberUpdatedState

        practiceRepo.updateWordAfterPractice(wordToUpdate, known) {
            statusMessage = if (known) "Чудово!" else "Не біда, вивчимо!"
            val nextIndex = if (words.isNotEmpty()) (currentIndex + 1) % words.size else 0
            currentIndex = nextIndex
            isFlipped = false
            coroutineScope.launch {
                anchoredDraggableState.updateAnchors(DraggableAnchors<DragAnchors> { DragAnchors.Start at 0f })
                anchoredDraggableState.snapTo(DragAnchors.Start)
            }
        }
    }


    LaunchedEffect(Unit) {
        practiceRepo.getWordsForPractice { fetchedWords ->
            words = fetchedWords
            currentIndex = 0
            isFlipped = false
            statusMessage = null
            coroutineScope.launch {
                anchoredDraggableState.updateAnchors(DraggableAnchors<DragAnchors> { DragAnchors.Start at 0f })
                anchoredDraggableState.snapTo(DragAnchors.Start)
            }
        }
    }

    LaunchedEffect(anchoredDraggableState.targetValue) {
        val target = anchoredDraggableState.targetValue
        when (target) {
            DragAnchors.Know -> currentMoveToNextWord(true)
            DragAnchors.DontKnow -> currentMoveToNextWord(false)
            DragAnchors.Start -> { /* No-op */ }
        }
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Практика слів") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (words.isEmpty()) {
                CircularProgressIndicator()
            } else {
                words.getOrNull(currentIndex)?.let { word ->
                    FlippableSwipeablePracticeCard(
                        word = word,
                        isFlipped = isFlipped,
                        onFlip = { isFlipped = !isFlipped },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .offset {
                                IntOffset(
                                    x = anchoredDraggableState
                                        .requireOffset()
                                        .roundToInt(),
                                    y = 0
                                )
                            }
                            .anchoredDraggable(
                                state = anchoredDraggableState,
                                orientation = Orientation.Horizontal,
                            )
                    )
                } ?: Text("Помилка: Не вдалося отримати слово")

                statusMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 60.dp)
                    )
                }
                if (words.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / words.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    )
                    Text(
                        text = "${currentIndex + 1} / ${words.size}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FlippableSwipeablePracticeCard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density

    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "FlipAnimation"
    )

    val frontOpacity by animateFloatAsState(
        targetValue = if (rotationY <= 90f) 1f else 0f,
        animationSpec = tween(100),
        label = "frontOpacity"
    )
    val backOpacity by animateFloatAsState(
        targetValue = if (rotationY > 90f) 1f else 0f,
        animationSpec = tween(100),
        label = "backOpacity"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                this.rotationY = rotationY
                this.cameraDistance = 12f * density
            }
            .noRippleClickable(enabled = !isFlipped) {
                onFlip()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotationY <= 90f) {
                CardContent(
                    title = word.text,
                    modifier = Modifier.graphicsLayer { alpha = frontOpacity }
                )
            } else {
                CardContent(
                    title = word.text,
                    subtitle = word.translation,
                    modifier = Modifier.graphicsLayer {
                        this.rotationY = 180f
                        alpha = backOpacity
                    }
                )
            }
        }
    }
}

@Composable
fun CardContent(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        subtitle?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun Modifier.noRippleClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    clickable(
        enabled = enabled,
        indication = null,
        interactionSource = interactionSource,
        onClick = onClick
    )
}