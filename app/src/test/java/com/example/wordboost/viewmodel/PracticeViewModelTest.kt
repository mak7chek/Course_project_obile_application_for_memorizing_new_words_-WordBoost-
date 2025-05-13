package com.example.wordboost.viewmodel

// ... (всі імпорти залишаються такими ж) ...
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.tts.TextToSpeechService
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

@ExperimentalCoroutinesApi
class PracticeViewModelTest {
    private lateinit var testScheduler: TestCoroutineScheduler
    private lateinit var testDispatcher: TestDispatcher

    private lateinit var practiceRepositoryMock: PracticeRepository
    private lateinit var ttsServiceMock: TextToSpeechService
    private lateinit var authRepositoryMock: AuthRepository
    private lateinit var viewModel: PracticeViewModel

    private val word1 = Word(id = "id1", text = "apple", translation = "яблуко", nextReview = 0L, status = "new")
    private val word2 = Word(id = "id2", text = "banana", translation = "банан", nextReview = 0L, status = "new")
    private val word3 = Word(id = "id3", text = "cherry", translation = "вишня", nextReview = 0L, status = "new")
    private val word4 = Word(id = "id4", text = "coconut", translation = "кокос", nextReview = 0L, status = "new")
    private val word5 = Word(id = "id5", text = "apricot", translation = "абрикос", nextReview = 0L, status = "new")
    private val word6 = Word(id = "id6", text = "fig", translation = "інжир", nextReview = 0L, status = "new")

    private val initialBatchSize = 5

    @Before
    fun setUp() {
        testScheduler = TestCoroutineScheduler()
        testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        practiceRepositoryMock = mockk()
        ttsServiceMock = mockk(relaxed = true)
        authRepositoryMock = mockk(relaxed = true)

        every { practiceRepositoryMock.getWordsNeedingPracticeFlow() } returns flowOf(emptyList()) // Default
        coEvery { practiceRepositoryMock.getWordById(any()) } returns null // Default
        every { practiceRepositoryMock.saveWord(any(), any()) } answers { // Default
            secondArg<(Boolean) -> Unit>().invoke(true)
        }

        viewModel = PracticeViewModel(practiceRepositoryMock, ttsServiceMock, authRepositoryMock)
        testScheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun setupViewModelForPractice(wordsForSession: List<Word>, makeBatchRegular: Boolean = true) {
        every { practiceRepositoryMock.getWordsNeedingPracticeFlow() } returns flowOf(wordsForSession)
        viewModel.startOrRefreshSession()
        testScheduler.advanceUntilIdle() // Дуже важливо!

        if (wordsForSession.isNotEmpty() && makeBatchRegular) {
            viewModel.onPairingFinished()
            testScheduler.advanceUntilIdle() // І тут також!
        }
    }

    @Test
    fun `initial state - phase is Loading initially, then Empty if no words on start`() = runTest(testScheduler) {
        every { practiceRepositoryMock.getWordsNeedingPracticeFlow() } returns flowOf(emptyList())
        val freshViewModel = PracticeViewModel(practiceRepositoryMock, ttsServiceMock, authRepositoryMock)
        assertTrue(freshViewModel.practicePhase.value is PracticePhase.Loading)

        freshViewModel.startOrRefreshSession()
        testScheduler.advanceUntilIdle()

        assertTrue(freshViewModel.practicePhase.value is PracticePhase.Empty)
    }

    @Test
    fun `startOrRefreshSession - with words - phase is BatchPairing, batch populated`() = runTest(testScheduler) {
        val testWordsForSession = listOf(word1, word2, word3)
        every { practiceRepositoryMock.getWordsNeedingPracticeFlow() } returns flowOf(testWordsForSession)

        viewModel.startOrRefreshSession()
        testScheduler.advanceUntilIdle()

        assertTrue("Phase should be BatchPairing, but was ${viewModel.practicePhase.value}", viewModel.practicePhase.value is PracticePhase.BatchPairing)
        val batch = viewModel.currentBatch.value
        assertEquals(testWordsForSession.take(initialBatchSize), batch)
        assertEquals(0, viewModel.currentWordIndexInBatch.value)
        assertEquals(CardState.Prompt, viewModel.currentCardState.value)
    }

    @Test
    fun `startOrRefreshSession - no words - phase is Empty`() = runTest(testScheduler) {
        every { practiceRepositoryMock.getWordsNeedingPracticeFlow() } returns flowOf(emptyList())

        viewModel.startOrRefreshSession()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.practicePhase.value is PracticePhase.Empty)
        assertTrue(viewModel.currentBatch.value.isEmpty())
    }

    @Test
    fun `flipCard - toggles card state and stops TTS`() = runTest(testScheduler) {
        setupViewModelForPractice(listOf(word1))
        testScheduler.advanceUntilIdle() // Переконуємось, що setup завершено

        assertEquals(CardState.Prompt, viewModel.currentCardState.value)
        clearMocks(ttsServiceMock, answers = false, recordedCalls = true) // Очищаємо виклики TTS від setup

        viewModel.flipCard()
        testScheduler.advanceUntilIdle()
        assertEquals(CardState.Answer, viewModel.currentCardState.value)
        verify(exactly = 1) { ttsServiceMock.stop() }

        viewModel.flipCard()
        testScheduler.advanceUntilIdle()
        assertEquals(CardState.Prompt, viewModel.currentCardState.value)
        verify(exactly = 2) { ttsServiceMock.stop() }
    }

    @Test
    fun `onPairingFinished - transitions to BatchRegular, resets index and card state, enables undo`() = runTest(testScheduler) {
        every { practiceRepositoryMock.getWordsNeedingPracticeFlow() } returns flowOf(listOf(word1, word2))
        viewModel.startOrRefreshSession()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.practicePhase.value is PracticePhase.BatchPairing)
        assertFalse("Undo should be disabled before onPairingFinished (if stack empty)", viewModel.canUndo.value)

        viewModel.onPairingFinished()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.practicePhase.value is PracticePhase.BatchRegular)
        assertEquals(0, viewModel.currentWordIndexInBatch.value)
        assertEquals(CardState.Prompt, viewModel.currentCardState.value)
        assertTrue("Undo should be enabled after onPairingFinished", viewModel.canUndo.value)
    }


    @Test
    fun `onCardSwipedRight - processes answer, saves word, moves to next`() = runTest(testScheduler) {
        setupViewModelForPractice(listOf(word1, word2))
        testScheduler.advanceUntilIdle()

        assertNotNull("Current word should not be null", viewModel.currentWordInBatch.value)
        assertEquals("Current word should be word1", word1.id, viewModel.currentWordInBatch.value?.id)

        clearMocks(practiceRepositoryMock, answers = false, recordedCalls = true)
        val wordSlot = slot<Word>()
        every { practiceRepositoryMock.saveWord(capture(wordSlot), any()) } answers {
            secondArg<(Boolean) -> Unit>().invoke(true)
        }

        viewModel.onCardSwipedRight()
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { practiceRepositoryMock.saveWord(any(), any()) }
        val savedWord = wordSlot.captured
        assertEquals(word1.id, savedWord.id)
        assertTrue(savedWord.repetition > word1.repetition)

        assertNotNull("Next word (word2) should not be null", viewModel.currentWordInBatch.value)
        assertEquals("Should move to word2", word2.id, viewModel.currentWordInBatch.value?.id)
        assertEquals(1, viewModel.currentWordIndexInBatch.value)
        assertEquals(CardState.Prompt, viewModel.currentCardState.value)
    }

    @Test
    fun `onCardSwipedLeft - processes answer, saves word, moves to next`() = runTest(testScheduler) {
        setupViewModelForPractice(listOf(word1, word2))
        testScheduler.advanceUntilIdle()

        assertNotNull("Current word should not be null", viewModel.currentWordInBatch.value)
        assertEquals("Current word should be word1", word1.id, viewModel.currentWordInBatch.value?.id)

        clearMocks(practiceRepositoryMock, answers = false, recordedCalls = true)
        val wordSlot = slot<Word>()
        every { practiceRepositoryMock.saveWord(capture(wordSlot), any()) } answers {
            secondArg<(Boolean) -> Unit>().invoke(true)
        }

        viewModel.onCardSwipedLeft()
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { practiceRepositoryMock.saveWord(any(), any()) }
        val savedWord = wordSlot.captured
        assertEquals(word1.id, savedWord.id)
        assertEquals("Repetition should reset", 0, savedWord.repetition)

        assertNotNull("Next word (word2) should not be null", viewModel.currentWordInBatch.value)
        assertEquals("Should move to word2", word2.id, viewModel.currentWordInBatch.value?.id)
        assertEquals(1, viewModel.currentWordIndexInBatch.value)
    }

    @Test
    fun `onPairMatched - in BatchPairing - processes answer, saves word, no index change`() = runTest(testScheduler) {
        setupViewModelForPractice(listOf(word1, word2), makeBatchRegular = false)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.practicePhase.value is PracticePhase.BatchPairing)
        assertNotNull(viewModel.currentWordInBatch.value)
        assertEquals(word1.id, viewModel.currentWordInBatch.value?.id)
        assertFalse(viewModel.canUndo.value)

        clearMocks(practiceRepositoryMock, answers = false, recordedCalls = true)
        val wordSlot = slot<Word>()
        every { practiceRepositoryMock.saveWord(capture(wordSlot), any()) } answers {
            secondArg<(Boolean) -> Unit>().invoke(true)
        }

        viewModel.onPairMatched(word1.id)
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { practiceRepositoryMock.saveWord(any(), any()) }
        val savedWord = wordSlot.captured
        assertEquals(word1.id, savedWord.id)
        assertTrue("Repetition should be > 0 for quality 4 (assumed by onPairMatched)", savedWord.repetition > 0)

        assertEquals("Index should not change", 0, viewModel.currentWordIndexInBatch.value)
        assertFalse("canUndo should remain false as onPairMatched does not push to undoStack", viewModel.canUndo.value)
    }

    @Test
    fun `processAnswer - last word in entire session - phase becomes Finished`() = runTest(testScheduler) {
        val wordForSession = Word(id = "single", text = "single_word", translation = "одне_слово")
        val wordsFlowEmitter = MutableStateFlow(listOf(wordForSession))
        every { practiceRepositoryMock.getWordsNeedingPracticeFlow() } returns wordsFlowEmitter

        viewModel.startOrRefreshSession()
        testScheduler.runCurrent()

        viewModel.onPairingFinished()
        testScheduler.runCurrent()

        assertEquals(wordForSession, viewModel.currentWordInBatch.value)

        clearMocks(practiceRepositoryMock, answers = false, recordedCalls = true)
        val wordSlot = slot<Word>()
        val lambdaSlot = slot<(Boolean) -> Unit>()

        every { practiceRepositoryMock.saveWord(capture(wordSlot), capture(lambdaSlot)) } returns Unit // Просто фіксуємо виклик, колбек викликаємо вручну

        viewModel.onCardSwipedRight()
        testScheduler.runCurrent() // Дозволяємо викликати saveWord

        wordsFlowEmitter.value = emptyList() // Симулюємо, що слова закінчились
        testScheduler.runCurrent() // Даємо collect зреагувати і оновити _allWordsForSession

        lambdaSlot.captured.invoke(true)
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { practiceRepositoryMock.saveWord(any(), any()) }
        val finalPhase = viewModel.practicePhase.value
        assertTrue(
            "Practice phase should be Finished, but was $finalPhase. Batch: ${viewModel.currentBatch.value}, AllWords: ${viewModel.allWordsForSessionSnapshotForDebug}",
            finalPhase is PracticePhase.Finished
        )
    }

    @Test
    fun `processAnswer - when current batch finishes AND more words in session - starts next batch (BatchPairing)`() = runTest(testScheduler) {
        val wordsForFirstBatch = listOf(word1, word2, word3, word4, word5)
        val wordForSecondBatch = word6
        val initialSessionWords = wordsForFirstBatch + listOf(wordForSecondBatch)

        val wordsFlowEmitter = MutableStateFlow(initialSessionWords)
        every { practiceRepositoryMock.getWordsNeedingPracticeFlow() } returns wordsFlowEmitter

        viewModel.startOrRefreshSession()
        testScheduler.runCurrent()
        viewModel.onPairingFinished()
        testScheduler.runCurrent()

        assertEquals("Initial batch should be wordsForFirstBatch", wordsForFirstBatch, viewModel.currentBatch.value)

        // Проходимо всі слова першого батчу
        wordsForFirstBatch.forEachIndexed { index, wordToProcess ->
            // Мокуємо saveWord для кожного слова
            clearMocks(practiceRepositoryMock, answers = false, recordedCalls = true) // Очищаємо для saveWord
            val wordSlot = slot<Word>()
            val saveCallbackSlot = slot<(Boolean) -> Unit>()
            every { practiceRepositoryMock.saveWord(capture(wordSlot), capture(saveCallbackSlot)) } returns Unit

            // Дія
            viewModel.onCardSwipedRight()
            testScheduler.runCurrent()
            val remainingWordsInFlow = wordsFlowEmitter.value.toMutableList()
            remainingWordsInFlow.removeIf { it.id == wordToProcess.id }
            wordsFlowEmitter.value = remainingWordsInFlow
            testScheduler.runCurrent() // Даємо час _allWordsForSession оновитися у ViewModel

            // Тепер викликаємо колбек saveWord, щоб продовжити логіку
            saveCallbackSlot.captured.invoke(true)
            testScheduler.advanceUntilIdle() // Дозволяємо processAnswerAndUpdateWord завершитись
            // і, якщо це останнє слово батчу, викликати startNextPracticeBatch
        }

        testScheduler.advanceUntilIdle()
        val finalPhase = viewModel.practicePhase.value
        val currentBatch = viewModel.currentBatch.value

        assertTrue(
            "Phase should be BatchPairing for the new batch, but was $finalPhase. Current batch: $currentBatch, All words snapshot: ${viewModel.allWordsForSessionSnapshotForDebug}",
            finalPhase is PracticePhase.BatchPairing
        )
        assertEquals(
            "New batch should contain the remaining word. Current batch: $currentBatch, All words snapshot: ${viewModel.allWordsForSessionSnapshotForDebug}",
            listOf(wordForSecondBatch),
            currentBatch
        )
        assertEquals("Index should be 0 for the new batch", 0, viewModel.currentWordIndexInBatch.value)
    }
    @Test
    fun `undoLastAction - when stack is empty initially - canUndo is false, error message`() = runTest(testScheduler) {
        viewModel.startOrRefreshSession() // Потрапить в Empty, бо потік слів порожній
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.practicePhase.value is PracticePhase.Empty)
        assertFalse(viewModel.canUndo.value)

        viewModel.undoLastAction()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.canUndo.value)
        assertNotNull(viewModel.errorMessage.value)
        assertEquals("Немає дій для скасування.", viewModel.errorMessage.value)
    }
}