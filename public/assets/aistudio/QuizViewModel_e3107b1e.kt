package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import java.io.File
import java.io.OutputStream
import com.example.audio.SpeechManager
import com.example.data.GeminiQuizGenerator
import com.example.data.PdfQuestionPaperGenerator
import com.example.data.QuestionPaperSection
import com.example.data.QuizParser
import com.example.data.QuizRepository
import com.example.model.MarkingScheme
import com.example.model.QuizGrading
import com.example.model.QuizMode
import com.example.model.QuizQuestion
import com.example.model.SavedQuiz
import com.example.model.ScreenState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState(
    val screenState: ScreenState = ScreenState.HOME,
    val quizMode: QuizMode = QuizMode.PRACTICE,
    val markingScheme: MarkingScheme = MarkingScheme(1.0, 0.25),
    val inputQuizText: String = "",
    val inputQuizName: String = "",
    val inputQuizCategory: String = "General",
    val selectedCategoryFilter: String = "All",
    val savedQuizSearchQuery: String = "",
    val savedQuizzes: List<SavedQuiz> = emptyList(),
    val starredQuestions: List<QuizQuestion> = emptyList(),
    val totalParsedQuestions: List<QuizQuestion> = emptyList(),
    val activeQuiz: List<QuizQuestion> = emptyList(),
    val unplayedPool: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val score: Int = 0,
    val skipped: Int = 0,
    val wrongQuestions: List<QuizQuestion> = emptyList(),
    val isLocked: Boolean = false,
    val timeLeft: Int = 30,
    val questionTimeSeconds: Int = 30,
    val examTotalTimeSeconds: Int = 30,
    val examAnswers: Map<Int, Int> = emptyMap(),
    val bookmarkedIndices: Set<Int> = emptySet(),
    val isDarkMode: Boolean? = null,
    val isTimerPaused: Boolean = false,
    val selectedOptionIndex: Int? = null,
    val feedbackMessage: String = "",
    val isFeedbackSuccess: Boolean? = null,
    val numQuestions: Int = 0,
    val maxQuestions: Int = 0,
    val isSpeaking: Boolean = false,
    val showWrongReview: Boolean = false,
    val isPromptCopied: Boolean = false,
    val isQuizSaved: Boolean = false,
    val isGeneratingAiQuiz: Boolean = false,
    val aiErrorMessage: String? = null,
    val customApiKey: String = "",
    val snackbarMessage: String? = null
)

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuizRepository(application)
    private val soundManager = SoundManager()
    private val speechManager = SpeechManager(application) { isSpeaking ->
        _uiState.update { it.copy(isSpeaking = isSpeaking) }
    }

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var advanceJob: Job? = null
    private var copyFeedbackJob: Job? = null
    private var saveFeedbackJob: Job? = null

    init {
        val savedDarkMode = repository.getDarkModeOverride()
        val savedTime = repository.getQuestionTimeSeconds()
        val savedMode = repository.getQuizMode()
        val savedApiKey = repository.getCustomApiKey()
        val savedStarred = repository.getStarredQuestions()
        _uiState.update {
            it.copy(
                isDarkMode = savedDarkMode,
                questionTimeSeconds = savedTime,
                timeLeft = savedTime,
                quizMode = savedMode,
                markingScheme = MarkingScheme(1.0, 0.25),
                customApiKey = savedApiKey,
                starredQuestions = savedStarred
            )
        }
        loadSavedQuizzes()
        parseCurrentInputText()
    }

    fun setQuizMode(mode: QuizMode) {
        repository.setQuizMode(mode)
        _uiState.update { it.copy(quizMode = mode) }
        soundManager.playClick()
    }

    fun setMarkingScheme(positiveMarks: Double, negativeMarks: Double) {
        val scheme = MarkingScheme(
            positiveMarks = positiveMarks.coerceIn(0.25, 100.0),
            negativeMarks = negativeMarks.coerceIn(0.0, 100.0)
        )
        repository.setMarkingScheme(scheme)
        _uiState.update { it.copy(markingScheme = scheme) }
        soundManager.playClick()
    }

    fun updateCustomApiKey(apiKey: String) {
        repository.setCustomApiKey(apiKey)
        _uiState.update { it.copy(customApiKey = apiKey, aiErrorMessage = null) }
    }

    fun clearAiError() {
        _uiState.update { it.copy(aiErrorMessage = null) }
    }

    fun generateQuizWithAi(
        topicOrNotes: String,
        questionCount: Int,
        difficulty: String,
        languageStyle: String,
        onSuccess: () -> Unit
    ) {
        val cleanTopic = topicOrNotes.trim()
        if (cleanTopic.isEmpty()) {
            _uiState.update { it.copy(aiErrorMessage = "Please enter a topic or paste study notes first.") }
            return
        }

        _uiState.update { it.copy(isGeneratingAiQuiz = true, aiErrorMessage = null) }
        viewModelScope.launch {
            val result = GeminiQuizGenerator.generateQuizData(
                topicOrNotes = cleanTopic,
                questionCount = questionCount.coerceIn(3, 50),
                difficulty = difficulty,
                languageStyle = languageStyle,
                customApiKey = _uiState.value.customApiKey
            )
            result.fold(
                onSuccess = { payload ->
                    _uiState.update {
                        it.copy(
                            isGeneratingAiQuiz = false,
                            aiErrorMessage = null,
                            inputQuizText = payload.quizData,
                            inputQuizName = payload.title,
                            inputQuizCategory = payload.category.ifBlank { "General" }
                        )
                    }
                    parseCurrentInputText()
                    soundManager.playCorrect()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGeneratingAiQuiz = false,
                            aiErrorMessage = error.localizedMessage ?: "Failed to generate quiz. Please check your internet or API key."
                        )
                    }
                }
            )
        }
    }

    fun toggleDarkMode(currentlyDark: Boolean) {
        val nextDark = !currentlyDark
        repository.setDarkModeOverride(nextDark)
        _uiState.update { it.copy(isDarkMode = nextDark) }
        soundManager.playClick()
    }

    fun setQuestionTimeSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(5, 600)
        repository.setQuestionTimeSeconds(clamped)
        _uiState.update {
            it.copy(
                questionTimeSeconds = clamped,
                timeLeft = if (it.screenState == ScreenState.HOME) clamped else it.timeLeft
            )
        }
        soundManager.playClick()
    }

    fun loadSavedQuizzes() {
        val list = repository.getSavedQuizzes()
        val starred = repository.getStarredQuestions()
        _uiState.update { state ->
            val categories = list.map { it.category.ifBlank { "General" } }.toSet()
            val validFilter = if (state.selectedCategoryFilter == "All" || state.selectedCategoryFilter in categories) {
                state.selectedCategoryFilter
            } else {
                "All"
            }
            val syncedBookmarks = state.activeQuiz.indices.filter { idx ->
                val qText = state.activeQuiz[idx].q.trim()
                starred.any { it.q.trim().equals(qText, ignoreCase = true) }
            }.toSet()
            state.copy(
                savedQuizzes = list,
                starredQuestions = starred,
                bookmarkedIndices = syncedBookmarks,
                selectedCategoryFilter = validFilter
            )
        }
    }

    fun onInputTextChanged(newText: String) {
        // If a large paste occurred directly into the TextField and had <br> turned into a newline inside quotes, sanitize it
        val currentLen = _uiState.value.inputQuizText.length
        val finalText = if (kotlin.math.abs(newText.length - currentLen) > 10) {
            QuizParser.sanitizeQuizText(newText)
        } else {
            newText
        }
        _uiState.update { it.copy(inputQuizText = finalText) }
        parseCurrentInputText()
    }

    fun onInputNameChanged(newName: String) {
        _uiState.update { it.copy(inputQuizName = newName) }
    }

    fun onInputCategoryChanged(newCategory: String) {
        _uiState.update { it.copy(inputQuizCategory = newCategory) }
    }

    fun setSelectedCategoryFilter(category: String) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
        soundManager.playClick()
    }

    fun setSavedQuizSearchQuery(query: String) {
        _uiState.update { it.copy(savedQuizSearchQuery = query) }
    }

    fun pasteFromClipboard() {
        try {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                // Prefer item.text directly without coerceToText (which invokes Html.fromHtml and turns <br> into \n)
                val plainText = item.text?.toString() ?: item.coerceToText(getApplication()).toString()
                val htmlText = item.htmlText
                val sanitized = QuizParser.sanitizeQuizText(plainText, htmlText).trim()
                if (sanitized.isNotBlank()) {
                    // Check if the clipboard text is a multi-quiz export bundle (=== QUIZ: ... ===)
                    if (sanitized.contains("=== QUIZ:", ignoreCase = true)) {
                        val importedCount = importQuizzesFromText(sanitized)
                        if (importedCount > 0) {
                            return
                        }
                    }
                    _uiState.update { it.copy(inputQuizText = "") }
                    _uiState.update { it.copy(inputQuizText = sanitized) }
                    parseCurrentInputText()
                    val count = _uiState.value.totalParsedQuestions.size
                    showSnackbar("Data Pasted Successfully! ($count Questions Loaded)")
                    return
                }
            }
            showSnackbar("No Text Found in Clipboard")
        } catch (e: Exception) {
            showSnackbar("Error in Paste: ${e.localizedMessage}")
        }
    }

    private fun parseCurrentInputText() {
        val text = _uiState.value.inputQuizText
        val result = QuizParser.parse(text)
        val count = result.questions.size
        _uiState.update {
            it.copy(
                totalParsedQuestions = result.questions,
                maxQuestions = maxOf(1, count),
                // Show ALL questions by default without limiting to 3
                numQuestions = if (count > 0) count else 1
            )
        }
    }

    fun setNumQuestions(count: Int) {
        val max = _uiState.value.maxQuestions
        val clamped = count.coerceIn(1, max)
        _uiState.update { it.copy(numQuestions = clamped) }
    }

    fun loadPreset(type: String) {
        val text = when (type) {
            "EMOJI" -> QuizRepository.DEFAULT_EMOJI_DATA
            "SCIENCE" -> QuizRepository.PRESET_SCIENCE_DATA
            "TECH" -> QuizRepository.PRESET_TECH_DATA
            else -> ""
        }
        val name = when (type) {
            "EMOJI" -> "Emoji Challenge"
            "SCIENCE" -> "Science and Knowledge"
            "TECH" -> "Computers and Technology"
            else -> ""
        }
        val category = when (type) {
            "EMOJI" -> "General"
            "SCIENCE" -> "Science"
            "TECH" -> "Computer"
            else -> "General"
        }
        _uiState.update { it.copy(inputQuizText = text, inputQuizName = name, inputQuizCategory = category) }
        parseCurrentInputText()
        showSnackbar("Loaded: $name")
    }

    fun saveCurrentQuiz() {
        val text = _uiState.value.inputQuizText.trim()
        if (text.isEmpty()) {
            showSnackbar("Please enter the quiz data first.")
            return
        }
        val name = _uiState.value.inputQuizName.trim()
        val category = _uiState.value.inputQuizCategory.trim().ifEmpty { "General" }
        val saved = repository.saveQuiz(name = name, data = text, category = category)
        loadSavedQuizzes()
        soundManager.playClick()
        saveFeedbackJob?.cancel()
        saveFeedbackJob = viewModelScope.launch {
            _uiState.update { it.copy(isQuizSaved = true) }
            delay(1000)
            _uiState.update { it.copy(isQuizSaved = false) }
        }
        showSnackbar("✅ Quiz Saved: ${saved.name}")
    }

    fun deleteSavedQuiz(quizId: String) {
        repository.deleteQuiz(quizId)
        loadSavedQuizzes()
        soundManager.playClick()
        showSnackbar("Quiz deleted")
    }

    fun clearAllSavedQuizzes() {
        repository.clearAllQuizzes()
        loadSavedQuizzes()
        soundManager.playClick()
        showSnackbar("All saved quizzes & starred questions cleared")
    }

    fun openQuizDatabase() {
        loadSavedQuizzes()
        _uiState.update { it.copy(screenState = ScreenState.DATABASE) }
        soundManager.playClick()
    }

    fun selectSavedQuiz(saved: SavedQuiz, startImmediately: Boolean = false) {
        if (startImmediately) {
            val parseResult = QuizParser.parse(saved.data)
            if (parseResult.questions.isEmpty()) return
            val allCount = parseResult.questions.size
            _uiState.update {
                it.copy(
                    totalParsedQuestions = parseResult.questions,
                    unplayedPool = parseResult.questions.shuffled(),
                    maxQuestions = allCount,
                    numQuestions = allCount
                )
            }
            startQuiz()
        } else {
            _uiState.update {
                it.copy(
                    screenState = ScreenState.HOME,
                    inputQuizText = saved.data,
                    inputQuizName = saved.name,
                    inputQuizCategory = saved.category.ifBlank { "General" }
                )
            }
            parseCurrentInputText()
            soundManager.playClick()
            showSnackbar("Loaded: ${saved.name}")
        }
    }

    fun copyPromptToClipboard(
        topicOrNotes: String = "",
        questionCount: Int = 50,
        difficulty: String = "Medium",
        languageStyle: String = "Hinglish"
    ) {
        val promptText = QuizRepository.buildAiPrompt(
            topicOrNotes = topicOrNotes,
            questionCount = questionCount.coerceIn(1, 200),
            difficulty = difficulty,
            languageStyle = languageStyle
        )
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Quiz AI Prompt", promptText)
        clipboard.setPrimaryClip(clip)
        soundManager.playClick()
        copyFeedbackJob?.cancel()
        copyFeedbackJob = viewModelScope.launch {
            _uiState.update { it.copy(isPromptCopied = true) }
            delay(1200)
            _uiState.update { it.copy(isPromptCopied = false) }
        }
    }

    private fun shareAsTxtFile(fileNameBase: String, title: String, textContent: String) {
        val app = getApplication<Application>()
        try {
            val exportDir = File(app.cacheDir, "exports").apply { mkdirs() }
            val safeFileName = fileNameBase
                .replace(Regex("""[^a-zA-Z0-9_\-\u0900-\u097F ]"""), "")
                .trim()
                .replace(Regex("""\s+"""), "_")
                .ifBlank { "raman_quizzes" }
            val txtFile = File(exportDir, "$safeFileName.txt")
            txtFile.writeText(textContent, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                app,
                "${app.packageName}.fileprovider",
                txtFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, "$safeFileName.txt")
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("$safeFileName.txt", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(chooser)
        } catch (_: Exception) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, textContent)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(chooser)
        }
    }

    fun exportQuiz(saved: SavedQuiz) {
        val textContent = repository.exportQuizzesToEditableText(listOf(saved))
        shareAsTxtFile(
            fileNameBase = saved.name,
            title = "Export ${saved.name}.txt",
            textContent = textContent
        )
    }

    fun shareAllQuizzes(onlySelectedCategory: Boolean = false) {
        val allQuizzes = _uiState.value.savedQuizzes
        val starred = _uiState.value.starredQuestions
        val filter = _uiState.value.selectedCategoryFilter
        val listToExport = if (onlySelectedCategory && filter != "All") {
            allQuizzes.filter { it.category.equals(filter, ignoreCase = true) }
        } else {
            allQuizzes
        }
        val starredToExport = if (onlySelectedCategory && filter != "All") {
            emptyList()
        } else {
            starred
        }
        if (listToExport.isEmpty() && starredToExport.isEmpty()) return

        val textContent = repository.exportQuizzesToEditableText(listToExport, starredToExport)
        val fileNameBase = if (onlySelectedCategory && filter != "All") {
            "raman_quiz_${filter.lowercase()}"
        } else {
            "raman_all_quizzes"
        }
        val title = if (onlySelectedCategory && filter != "All") {
            "Export $filter Quizzes (.txt)"
        } else {
            "Export All Quizzes (${listToExport.size}) (.txt)"
        }
        shareAsTxtFile(
            fileNameBase = fileNameBase,
            title = title,
            textContent = textContent
        )
    }

    fun getAllQuizzesExportText(): String {
        return repository.exportQuizzesToEditableText(
            quizzes = _uiState.value.savedQuizzes,
            starredQuestions = _uiState.value.starredQuestions
        )
    }

    private fun sharePdfFile(pdfFile: File, title: String) {
        val app = getApplication<Application>()
        try {
            val uri = FileProvider.getUriForFile(
                app,
                "${app.packageName}.fileprovider",
                pdfFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_TITLE, pdfFile.name)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(pdfFile.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(chooser)
        } catch (e: Exception) {
            showSnackbar("Could not share PDF: ${e.localizedMessage ?: "Error"}")
        }
    }

    fun shareQuizzesAsPdf(
        quizzes: List<SavedQuiz>,
        paperTitle: String,
        subtitleCategory: String,
        includeAnswerKey: Boolean = true
    ) {
        val sections = PdfQuestionPaperGenerator.buildSectionsFromSavedQuizzes(quizzes)
        if (sections.isEmpty()) {
            showSnackbar("No valid questions found to export as PDF.")
            return
        }
        val app = getApplication<Application>()
        try {
            val pdfFile = PdfQuestionPaperGenerator.generatePdfFileInCache(
                context = app,
                fileNameBase = paperTitle,
                paperTitle = paperTitle,
                subtitleCategory = subtitleCategory,
                sections = sections,
                timePerQuestionSeconds = _uiState.value.questionTimeSeconds,
                includeAnswerKey = includeAnswerKey,
                markingScheme = _uiState.value.markingScheme
            )
            sharePdfFile(pdfFile, "Share $paperTitle (PDF)")
        } catch (e: Exception) {
            showSnackbar("PDF Export failed: ${e.localizedMessage ?: "Error"}")
        }
    }

    fun writeQuizzesPdfToStream(
        outputStream: OutputStream,
        quizzes: List<SavedQuiz>,
        paperTitle: String,
        subtitleCategory: String,
        includeAnswerKey: Boolean = true
    ): Boolean {
        val sections = PdfQuestionPaperGenerator.buildSectionsFromSavedQuizzes(quizzes)
        if (sections.isEmpty()) {
            showSnackbar("No valid questions found to export as PDF.")
            return false
        }
        return try {
            PdfQuestionPaperGenerator.writePdfToStream(
                outputStream = outputStream,
                paperTitle = paperTitle,
                subtitleCategory = subtitleCategory,
                sections = sections,
                timePerQuestionSeconds = _uiState.value.questionTimeSeconds,
                includeAnswerKey = includeAnswerKey,
                markingScheme = _uiState.value.markingScheme
            )
            showSnackbar("📄 PDF Question Paper saved!")
            true
        } catch (e: Exception) {
            showSnackbar("PDF Save failed: ${e.localizedMessage ?: "Error"}")
            false
        }
    }

    fun createPdfFileForPrinting(
        quizzes: List<SavedQuiz>,
        paperTitle: String,
        subtitleCategory: String,
        includeAnswerKey: Boolean = true
    ): File? {
        val sections = PdfQuestionPaperGenerator.buildSectionsFromSavedQuizzes(quizzes)
        if (sections.isEmpty()) {
            showSnackbar("No valid questions found to print.")
            return null
        }
        return try {
            PdfQuestionPaperGenerator.generatePdfFileInCache(
                context = getApplication(),
                fileNameBase = paperTitle,
                paperTitle = paperTitle,
                subtitleCategory = subtitleCategory,
                sections = sections,
                timePerQuestionSeconds = _uiState.value.questionTimeSeconds,
                includeAnswerKey = includeAnswerKey,
                markingScheme = _uiState.value.markingScheme
            )
        } catch (e: Exception) {
            showSnackbar("Print preparation failed: ${e.localizedMessage ?: "Error"}")
            null
        }
    }

    fun shareActiveQuizAsPdf(includeAnswerKey: Boolean = true) {
        val state = _uiState.value
        val questions = state.activeQuiz.ifEmpty { state.totalParsedQuestions }
        if (questions.isEmpty()) {
            showSnackbar("No quiz questions available for PDF export.")
            return
        }
        val title = state.inputQuizName.trim().ifBlank { "Raman Quiz Question Paper" }
        val category = state.inputQuizCategory.trim().ifBlank { "General" }
        val section = QuestionPaperSection(
            title = title,
            category = category,
            questions = questions
        )
        try {
            val pdfFile = PdfQuestionPaperGenerator.generatePdfFileInCache(
                context = getApplication(),
                fileNameBase = title,
                paperTitle = title,
                subtitleCategory = category,
                sections = listOf(section),
                timePerQuestionSeconds = state.questionTimeSeconds,
                includeAnswerKey = includeAnswerKey,
                markingScheme = state.markingScheme
            )
            sharePdfFile(pdfFile, "Share $title (PDF)")
        } catch (e: Exception) {
            showSnackbar("PDF Export failed: ${e.localizedMessage ?: "Error"}")
        }
    }

    fun importQuizzesFromText(rawText: String, fallbackName: String = "Imported Quiz"): Int {
        val count = repository.importQuizzesFromText(rawText, fallbackName)
        if (count > 0) {
            loadSavedQuizzes()
            soundManager.playCorrect()
        }
        return count
    }

    fun startQuizFromHome() {
        val sourceText = _uiState.value.inputQuizText.trim().ifEmpty {
            _uiState.value.savedQuizzes.firstOrNull()?.data.orEmpty()
        }
        val parseResult = QuizParser.parse(sourceText)
        if (parseResult.questions.isEmpty()) {
            showSnackbar("❌ No valid questions found! Please check the format.")
            return
        }

        if (parseResult.skippedCount > 0) {
            showSnackbar("${parseResult.skippedCount} invalid questions skipped, ${parseResult.questions.size} questions loaded.")
        }

        val allCount = parseResult.questions.size
        _uiState.update {
            it.copy(
                totalParsedQuestions = parseResult.questions,
                unplayedPool = parseResult.questions.shuffled(),
                maxQuestions = allCount,
                numQuestions = allCount
            )
        }
        startQuiz()
    }

    fun startQuiz() {
        advanceJob?.cancel()
        speechManager.stop()

        val state = _uiState.value
        val allQuestions = state.totalParsedQuestions
        if (allQuestions.isEmpty()) {
            showSnackbar("No Question is Available")
            return
        }

        val requestedCount = state.numQuestions.coerceIn(1, allQuestions.size)
        var pool = state.unplayedPool.toMutableList()
        val selectedForRound = mutableListOf<QuizQuestion>()

        if (pool.size >= requestedCount) {
            for (i in 0 until requestedCount) {
                selectedForRound.add(pool.removeAt(0))
            }
        } else {
            selectedForRound.addAll(pool)
            val deficit = requestedCount - pool.size
            val freshPool = allQuestions.shuffled().filter { q -> !selectedForRound.any { it.q == q.q } }.toMutableList()
            if (freshPool.size < deficit) {
                freshPool.addAll(allQuestions.shuffled())
            }
            for (i in 0 until minOf(deficit, freshPool.size)) {
                selectedForRound.add(freshPool.removeAt(0))
            }
            pool = freshPool
        }

        val prepared = prepareQuestions(selectedForRound)
        val perQDuration = state.questionTimeSeconds.coerceIn(5, 600)
        val initialTime = if (state.quizMode == QuizMode.EXAM) {
            (prepared.size * perQDuration).coerceAtLeast(perQDuration)
        } else {
            perQDuration
        }

        _uiState.update {
            it.copy(
                screenState = ScreenState.QUIZ,
                activeQuiz = prepared,
                unplayedPool = pool,
                currentIndex = 0,
                score = 0,
                skipped = 0,
                wrongQuestions = emptyList(),
                examAnswers = emptyMap(),
                bookmarkedIndices = computeStarredIndices(prepared, it.starredQuestions),
                examTotalTimeSeconds = initialTime,
                isLocked = false,
                timeLeft = initialTime,
                isTimerPaused = false,
                selectedOptionIndex = null,
                feedbackMessage = "",
                isFeedbackSuccess = null,
                showWrongReview = false
            )
        }

        startTimer(resetTime = true)
    }

    private fun computeStarredIndices(
        activeQuiz: List<QuizQuestion>,
        starredQuestions: List<QuizQuestion>
    ): Set<Int> {
        if (activeQuiz.isEmpty() || starredQuestions.isEmpty()) return emptySet()
        val starredSet = starredQuestions.map { it.q.trim().lowercase() }.toSet()
        return activeQuiz.indices.filter { idx ->
            activeQuiz[idx].q.trim().lowercase() in starredSet
        }.toSet()
    }

    private fun prepareQuestions(source: List<QuizQuestion>): List<QuizQuestion> {
        return source.map { q ->
            if (q.o.isEmpty()) return@map q
            val safeCorrectIdx = q.a.coerceIn(0, q.o.size - 1)
            val shuffledIndices = q.o.indices.shuffled()
            val shuffledOptions = shuffledIndices.map { q.o[it] }
            val newCorrectIndex = shuffledIndices.indexOf(safeCorrectIdx).coerceAtLeast(0)
            QuizQuestion(q.q, shuffledOptions, newCorrectIndex)
        }
    }

    private fun startTimer(resetTime: Boolean = true) {
        timerJob?.cancel()
        val state = _uiState.value
        val perQDuration = state.questionTimeSeconds.coerceIn(5, 600)
        if (resetTime) {
            val targetTime = if (state.quizMode == QuizMode.EXAM) {
                state.examTotalTimeSeconds.coerceAtLeast(perQDuration)
            } else {
                perQDuration
            }
            _uiState.update { it.copy(timeLeft = targetTime, isTimerPaused = false) }
        }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value
                if (current.screenState != ScreenState.QUIZ || current.isLocked) break
                val isPaused = if (current.quizMode == QuizMode.EXAM) false else current.isTimerPaused
                if (!isPaused) {
                    val remaining = current.timeLeft - 1
                    if (remaining <= 0) {
                        _uiState.update { it.copy(timeLeft = 0) }
                        if (current.quizMode == QuizMode.EXAM) {
                            submitExam()
                        } else {
                            timeUp()
                        }
                        break
                    } else {
                        _uiState.update { it.copy(timeLeft = remaining) }
                    }
                }
            }
        }
    }

    fun toggleTimer() {
        if (_uiState.value.quizMode == QuizMode.EXAM) return // No pause in Exam Mode
        val paused = !_uiState.value.isTimerPaused
        _uiState.update { it.copy(isTimerPaused = paused) }
        soundManager.playClick()
    }

    fun checkAnswer(chosenIndex: Int) {
        val state = _uiState.value
        if (state.isLocked || state.screenState != ScreenState.QUIZ) return

        // In Exam Mode: record selected answer without locking or revealing Correct/Wrong
        if (state.quizMode == QuizMode.EXAM) {
            soundManager.playClick()
            val currentIdx = state.currentIndex
            val updatedAnswers = state.examAnswers.toMutableMap()
            updatedAnswers[currentIdx] = chosenIndex
            _uiState.update {
                it.copy(
                    examAnswers = updatedAnswers,
                    selectedOptionIndex = chosenIndex
                )
            }
            return
        }

        // Practice Mode: immediate answer check & auto-advance
        timerJob?.cancel()
        speechManager.stop()

        val currentQ = state.activeQuiz.getOrNull(state.currentIndex) ?: return
        val isCorrect = chosenIndex == currentQ.a

        if (isCorrect) {
            soundManager.playCorrect()
            _uiState.update {
                it.copy(
                    isLocked = true,
                    selectedOptionIndex = chosenIndex,
                    score = it.score + 1,
                    feedbackMessage = "✅ Correct Answer",
                    isFeedbackSuccess = true
                )
            }
        } else {
            soundManager.playWrong()
            _uiState.update {
                it.copy(
                    isLocked = true,
                    selectedOptionIndex = chosenIndex,
                    wrongQuestions = it.wrongQuestions + currentQ.copy(userAnswerIndex = chosenIndex),
                    feedbackMessage = "❌ Wrong Answer",
                    isFeedbackSuccess = false
                )
            }
        }

        advanceJob = viewModelScope.launch {
            delay(1200)
            goNext()
        }
    }

    fun clearExamOption() {
        val state = _uiState.value
        if (state.quizMode != QuizMode.EXAM || state.screenState != ScreenState.QUIZ) return
        soundManager.playClick()
        val updated = state.examAnswers.toMutableMap()
        updated.remove(state.currentIndex)
        _uiState.update {
            it.copy(
                examAnswers = updated,
                selectedOptionIndex = null
            )
        }
    }

    fun toggleBookmark() {
        val state = _uiState.value
        if (state.screenState != ScreenState.QUIZ) return
        val currentQ = state.activeQuiz.getOrNull(state.currentIndex)
        if (currentQ != null) {
            toggleStarQuestion(currentQ)
        }
    }

    fun goToQuestion(targetIndex: Int) {
        val state = _uiState.value
        if (state.quizMode != QuizMode.EXAM || state.screenState != ScreenState.QUIZ) return
        val clamped = targetIndex.coerceIn(0, (state.activeQuiz.size - 1).coerceAtLeast(0))
        if (clamped == state.currentIndex) return
        speechManager.stop()
        soundManager.playClick()
        _uiState.update {
            it.copy(
                currentIndex = clamped,
                selectedOptionIndex = it.examAnswers[clamped],
                feedbackMessage = "",
                isFeedbackSuccess = null
            )
        }
    }

    fun nextExamQuestion() {
        val state = _uiState.value
        if (state.quizMode != QuizMode.EXAM) return
        if (state.currentIndex < state.activeQuiz.size - 1) {
            goToQuestion(state.currentIndex + 1)
        }
    }

    fun prevExamQuestion() {
        val state = _uiState.value
        if (state.quizMode != QuizMode.EXAM) return
        if (state.currentIndex > 0) {
            goToQuestion(state.currentIndex - 1)
        }
    }

    fun submitExam() {
        timerJob?.cancel()
        advanceJob?.cancel()
        speechManager.stop()

        val state = _uiState.value
        var correctCount = 0
        var skippedCount = 0
        val wrongs = mutableListOf<QuizQuestion>()

        state.activeQuiz.forEachIndexed { idx, q ->
            val chosen = state.examAnswers[idx]
            if (chosen == null) {
                skippedCount++
                wrongs.add(q.copy(isTimedOut = true, userAnswerIndex = null))
            } else if (chosen == q.a) {
                correctCount++
            } else {
                wrongs.add(q.copy(isTimedOut = false, userAnswerIndex = chosen))
            }
        }

        soundManager.playCorrect()
        _uiState.update {
            it.copy(
                screenState = ScreenState.RESULT,
                score = correctCount,
                skipped = skippedCount,
                wrongQuestions = wrongs,
                isLocked = false,
                showWrongReview = false
            )
        }
    }

    fun timeUp() {
        val state = _uiState.value
        if (state.isLocked || state.screenState != ScreenState.QUIZ) return
        if (state.quizMode == QuizMode.EXAM) {
            submitExam()
            return
        }

        timerJob?.cancel()
        speechManager.stop()
        soundManager.playWrong()

        val currentQ = state.activeQuiz.getOrNull(state.currentIndex) ?: return
        _uiState.update {
            it.copy(
                isLocked = true,
                selectedOptionIndex = -1,
                skipped = it.skipped + 1,
                wrongQuestions = it.wrongQuestions + currentQ.copy(isTimedOut = true),
                feedbackMessage = "⏰ Skipped!",
                isFeedbackSuccess = false
            )
        }

        advanceJob = viewModelScope.launch {
            delay(1200)
            goNext()
        }
    }

    private fun goNext() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.activeQuiz.size) {
            showResult()
        } else {
            val duration = state.questionTimeSeconds.coerceIn(5, 600)
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    isLocked = false,
                    selectedOptionIndex = null,
                    feedbackMessage = "",
                    isFeedbackSuccess = null,
                    timeLeft = duration,
                    isTimerPaused = false
                )
            }
            startTimer(resetTime = true)
        }
    }

    private fun showResult() {
        timerJob?.cancel()
        speechManager.stop()
        _uiState.update {
            it.copy(
                screenState = ScreenState.RESULT,
                showWrongReview = false
            )
        }
    }

    fun toggleWrongReview() {
        _uiState.update { it.copy(showWrongReview = !it.showWrongReview) }
        soundManager.playClick()
    }

    fun retryWrongAnswers() {
        advanceJob?.cancel()
        speechManager.stop()
        val wrongs = _uiState.value.wrongQuestions
        if (wrongs.isEmpty()) return

        val cleaned = wrongs.map { it.copy(isTimedOut = false, userAnswerIndex = null) }
        val prepared = prepareQuestions(cleaned)
        val state = _uiState.value
        val perQDuration = state.questionTimeSeconds.coerceIn(5, 600)
        val initialTime = if (state.quizMode == QuizMode.EXAM) {
            (prepared.size * perQDuration).coerceAtLeast(perQDuration)
        } else {
            perQDuration
        }

        _uiState.update {
            it.copy(
                screenState = ScreenState.QUIZ,
                activeQuiz = prepared,
                currentIndex = 0,
                score = 0,
                skipped = 0,
                wrongQuestions = emptyList(),
                examAnswers = emptyMap(),
                bookmarkedIndices = computeStarredIndices(prepared, it.starredQuestions),
                examTotalTimeSeconds = initialTime,
                isLocked = false,
                timeLeft = initialTime,
                isTimerPaused = false,
                selectedOptionIndex = null,
                feedbackMessage = "",
                isFeedbackSuccess = null,
                showWrongReview = false
            )
        }
        startTimer(resetTime = true)
    }

    fun speakCurrentQuestion() {
        val state = _uiState.value
        val q = state.activeQuiz.getOrNull(state.currentIndex)?.q ?: return
        speechManager.speak(q)
    }

    fun isQuestionStarred(question: QuizQuestion?): Boolean {
        if (question == null) return false
        val target = question.q.trim()
        return _uiState.value.starredQuestions.any { it.q.trim().equals(target, ignoreCase = true) }
    }

    fun toggleStarQuestion(question: QuizQuestion) {
        soundManager.playClick()
        val target = question.q.trim()
        if (target.isEmpty() || question.o.isEmpty()) return

        val current = _uiState.value.starredQuestions.toMutableList()
        val existingIdx = current.indexOfFirst { it.q.trim().equals(target, ignoreCase = true) }
        if (existingIdx >= 0) {
            current.removeAt(existingIdx)
        } else {
            val cleanQuestion = QuizQuestion(
                q = target,
                o = question.o,
                a = question.a.coerceIn(0, (question.o.size - 1).coerceAtLeast(0))
            )
            current.add(0, cleanQuestion)
        }
        repository.saveStarredQuestions(current)
        _uiState.update {
            it.copy(
                starredQuestions = current,
                bookmarkedIndices = computeStarredIndices(it.activeQuiz, current)
            )
        }
    }

    fun removeStarredQuestion(questionText: String) {
        soundManager.playClick()
        val target = questionText.trim()
        val updated = _uiState.value.starredQuestions.filterNot {
            it.q.trim().equals(target, ignoreCase = true)
        }
        repository.saveStarredQuestions(updated)
        _uiState.update {
            it.copy(
                starredQuestions = updated,
                bookmarkedIndices = computeStarredIndices(it.activeQuiz, updated)
            )
        }
    }

    fun clearAllStarredQuestions() {
        soundManager.playClick()
        repository.saveStarredQuestions(emptyList())
        _uiState.update {
            it.copy(
                starredQuestions = emptyList(),
                bookmarkedIndices = emptySet()
            )
        }
    }

    fun startStarredQuestionsQuiz() {
        val starred = _uiState.value.starredQuestions
        if (starred.isEmpty()) return
        advanceJob?.cancel()
        speechManager.stop()
        soundManager.playClick()

        val cleaned = starred.map { it.copy(isTimedOut = false, userAnswerIndex = null) }
        val prepared = prepareQuestions(cleaned)
        val state = _uiState.value
        val perQDuration = state.questionTimeSeconds.coerceIn(5, 600)
        val initialTime = if (state.quizMode == QuizMode.EXAM) {
            (prepared.size * perQDuration).coerceAtLeast(perQDuration)
        } else {
            perQDuration
        }

        _uiState.update {
            it.copy(
                screenState = ScreenState.QUIZ,
                totalParsedQuestions = cleaned,
                activeQuiz = prepared,
                unplayedPool = emptyList(),
                numQuestions = prepared.size,
                maxQuestions = prepared.size,
                currentIndex = 0,
                score = 0,
                skipped = 0,
                wrongQuestions = emptyList(),
                examAnswers = emptyMap(),
                bookmarkedIndices = computeStarredIndices(prepared, starred),
                examTotalTimeSeconds = initialTime,
                isLocked = false,
                timeLeft = initialTime,
                isTimerPaused = false,
                selectedOptionIndex = null,
                feedbackMessage = "",
                isFeedbackSuccess = null,
                showWrongReview = false
            )
        }
        startTimer(resetTime = true)
    }

    fun loadStarredQuestionsIntoEditor() {
        val starred = _uiState.value.starredQuestions
        if (starred.isEmpty()) return
        soundManager.playClick()
        val formattedText = repository.formatQuestionsAsQuizText(starred)
        _uiState.update {
            it.copy(
                screenState = ScreenState.HOME,
                inputQuizName = "Starred Questions",
                inputQuizCategory = "Starred",
                inputQuizText = formattedText
            )
        }
        parseCurrentInputText()
    }

    fun getStarredSavedQuiz(): SavedQuiz {
        val starred = _uiState.value.starredQuestions
        val formatted = repository.formatQuestionsAsQuizText(starred)
        return SavedQuiz(
            id = "starred_questions_bank",
            name = "Starred Questions Bank",
            category = "Starred",
            data = formatted,
            timestamp = System.currentTimeMillis(),
            questionCount = starred.size
        )
    }

    fun goHome() {
        advanceJob?.cancel()
        timerJob?.cancel()
        speechManager.stop()
        _uiState.update {
            it.copy(
                screenState = ScreenState.HOME,
                isLocked = false,
                feedbackMessage = "",
                showWrongReview = false
            )
        }
    }

    fun showSnackbar(message: String) {
        // Bottom notifications removed per user preference
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun playClickSound() {
        soundManager.playClick()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        advanceJob?.cancel()
        copyFeedbackJob?.cancel()
        speechManager.shutdown()
    }
}
