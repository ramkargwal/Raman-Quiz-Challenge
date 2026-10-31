package com.example.data

import android.content.Context
import com.example.model.QuizQuestion
import com.example.model.SavedQuiz
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class QuizRepository(context: Context) {

    private val prefs = context.getSharedPreferences("raman_quiz_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SAVED_QUIZZES = "saved_quizzes"
        const val KEY_DARK_MODE = "dark_mode_override"
        const val KEY_QUESTION_TIME = "question_time_seconds"
        const val KEY_QUIZ_MODE = "quiz_mode"
        const val KEY_POSITIVE_MARKS = "marking_positive_marks"
        const val KEY_NEGATIVE_MARKS = "marking_negative_marks"
        const val KEY_CUSTOM_GEMINI_API_KEY = "custom_gemini_api_key"
        const val KEY_STARRED_QUESTIONS = "starred_questions_bank"
        const val DEFAULT_QUESTION_TIME = 30

        val DEFAULT_EMOJI_DATA = """
            { q: "यह 📌 इमोजी किस काम के लिए इस्तेमाल किया जाता है?", o: ["मैसेज डिलीट करने के लिए", "मैसेज को पिन (Pin) करने के लिए", "लोकेशन भेजने के लिए", "सिलाई करने के लिए"], a: 1 },
            { q: "यह इमोजी ♻️ किस प्रक्रिया को दर्शाता है?", o: ["वीडियो प्ले करना", "रीसायकल या पुनर्चक्रण (Recycle)", "रास्ता भटकना", "डिलीट करना"], a: 1 },
            { q: "टेक्स्ट करते समय 🚫 इमोजी का क्या अर्थ होता है?", o: ["आगे बढ़ें", "निषेध या मनाही (Prohibited)", "पार्किंग", "खतरा"], a: 1 }
        """.trimIndent()

        val PRESET_SCIENCE_DATA = """
            { q: "सूर्य के प्रकाश से हमें मुख्य रूप से कौन सा विटामिन प्राप्त होता है?", o: ["विटामिन A", "विटामिन B", "विटामिन C", "विटामिन D (Vitamin D)"], a: 3 },
            { q: "मानव शरीर में सबसे बड़ी और मजबूत हड्डी (Bone) कौन सी होती है?", o: ["फीमर (Femur)", "टिबिया (Tibia)", "ह्यूमरस (Humerus)", "स्टेपीज (Stapes)"], a: 0 },
            { q: "कंप्यूटर का मस्तिष्क (Brain of Computer) किसे कहा जाता है?", o: ["RAM", "हार्ड डिस्क", "CPU (Central Processing Unit)", "GPU"], a: 2 },
            { q: "शुद्ध जल का रासायनिक सूत्र (Chemical Formula) क्या है?", o: ["CO2", "H2O", "NaCl", "O2"], a: 1 },
            { q: "भारतीय अंतरिक्ष अनुसंधान संगठन (ISRO) का मुख्यालय कहाँ स्थित है?", o: ["नई दिल्ली", "मुंबई", "बेंगलुरु (Bengaluru)", "हैदराबाद"], a: 2 }
        """.trimIndent()

        val PRESET_TECH_DATA = """
            { q: "Android ऐप डेवलपमेंट के लिए Google द्वारा अनुशंसित मुख्य भाषा कौन सी है?", o: ["Kotlin", "Python", "PHP", "Swift"], a: 0 },
            { q: "वेबसाइट को सुंदर और आकर्षक बनाने (Styling) के लिए क्या उपयोग होता है?", o: ["HTML", "CSS (Cascading Style Sheets)", "SQL", "C++"], a: 1 },
            { q: "Git में अपने कोड को रिमोट सर्वर पर अपलोड करने के लिए कौन सी कमांड है?", o: ["git clone", "git commit", "git push", "git pull"], a: 2 },
            { q: "URL का पूरा नाम (Full Form) क्या होता है?", o: ["Universal Radio Link", "Uniform Resource Locator", "Unified Record Line", "Unique Route Link"], a: 1 },
            { q: "कंप्यूटर नेटवर्क में IP का क्या अर्थ होता है?", o: ["Internet Protocol", "Internal Program", "Instant Packet", "Interface Port"], a: 0 }
        """.trimIndent()

        fun buildAiPrompt(
            topicOrNotes: String = "",
            questionCount: Int = 50,
            difficulty: String = "Medium",
            languageStyle: String = "Hinglish"
        ): String {
            val difficultyDescription = when (difficulty) {
                "Easy" -> "Easy (Foundational concepts, direct definitions, and basic formula recall)"
                "Hard" -> "Hard (Challenging multi-step analytical, numerical, and deep conceptual questions)"
                "Exam" -> "Competitive Exam Level (SSC, Railway, Banking, JEE/NEET, UPSC/State PCS style high-yield exam questions with close distractors)"
                else -> "Medium (Standard conceptual and application-based questions suitable for school/board and competitive exams)"
            }

            val languageRules = when (languageStyle) {
                "English" -> """
                    * Write all questions and options in clear, accurate English.
                    * Keep all technical terms, scientific names, programming syntax, HTML/code tags (e.g., <br>, <div>, <p>), units, and mathematical formulas in clean standard notation.
                """.trimIndent()
                "Hindi" -> """
                    * Write all questions and options in clear Hindi (Devanagari script).
                    * Keep mathematical symbols, formulas, chemical equations, and code/HTML tags (e.g., <br>, <div>, <p>) in standard English/mathematical notation.
                """.trimIndent()
                else -> """
                    * Write all questions primarily in Hindi (Devanagari script).
                    * Keep all technical terms, scientific names, mathematical terms, programming concepts, HTML/code tags (e.g., <br>, <div>, <p>), tools, software, acronyms, units, and formulas in English.
                """.trimIndent()
            }

            val cleanTopic = topicOrNotes.trim()
            val topicSection = if (cleanTopic.isNotEmpty()) {
                """
                ### TOPIC / STUDY NOTES TO GENERATE QUIZ FROM:
                $cleanTopic

                Now generate all $questionCount questions immediately inside a single ```text code block using the exact format above.
                """.trimIndent()
            } else {
                """If you understand all rules above, respond ONLY with: "Ready for your topic/content!" and wait for the user to provide the topic or study notes."""
            }

            val d = "$"
            return """You are an Expert Quiz Master, Educational Content Analyst, and Assessment Designer.
Your task is to carefully analyze the topic, notes, or study material provided and generate a high-quality multiple-choice quiz.

### CRITICAL REQUIREMENT 1: Question Quantity & Difficulty
* Question Quantity: Generate EXACTLY $questionCount UNIQUE multiple-choice questions.
* Difficulty Level: $difficultyDescription.

### CRITICAL REQUIREMENT 2: Language Rules ($languageStyle)
$languageRules

### CRITICAL REQUIREMENT 3: Pure LaTeX Math & Formula Formatting (${d}...${d})
The quiz app uses a dedicated LaTeX Math Engine. Write ALL mathematical expressions, equations, fractions, powers, subscripts, roots, calculus limits, matrices, vectors, and chemical formulas in standard LaTeX wrapped in `${d}...${d}` (do NOT use Markdown formatting):
* Fractions (भिन्न / बटा): Always use `\frac{a}{b}` inside `${d}...${d}`, e.g., `${d}\frac{1}{\sin\theta}${d}`, `${d}\frac{\sin\theta}{\cos\theta}${d}`, `${d}\frac{x + 1}{x - 1}${d}`, `${d}\frac{\sqrt{3}}{2}${d}`.
* Exponents / Powers (Superscripts): Always use `^{...}` in LaTeX, e.g., `${d}z^n + z^{-n}${d}`, `${d}x^{n+1}${d}`, `${d}10^{-6}${d}`, `${d}e^{i\theta}${d}`, `${d}\sin^2\theta + \cos^2\theta = 1${d}`, `${d}\left(\frac{a}{b}\right)^n${d}`.
* Subscripts & Chemical Formulas: Always use `_{...}` in LaTeX, e.g., `${d}a_n${d}`, `${d}a_{n-1}${d}`, `${d}\log_{10}(x)${d}`, `${d}\text{H}_2\text{O}${d}`, `${d}\text{CO}_2${d}`, `${d}\text{H}_2\text{SO}_4${d}`.
* Square & Nth Roots (वर्गमूल): Always use `\sqrt{...}` or `\sqrt[n]{...}`, e.g., `${d}\sqrt{3}${d}`, `${d}\sqrt{x^2 + y^2}${d}`, `${d}\sqrt[3]{x}${d}`, `${d}\sqrt{\frac{1 - \cos\theta}{1 + \cos\theta}}${d}`.
* Calculus, Limits, Vectors & Binomials: Use standard LaTeX commands such as `${d}\lim_{x \to 0} \frac{\sin x}{x}${d}`, `${d}\int_{0}^{\pi/2} \sin x \, dx${d}`, `${d}\sum_{k=1}^{n} k^2${d}`, `${d}\binom{n}{r}${d}`, `${d}\vec{a} \cdot \vec{b}${d}`, `${d}\overline{AB}${d}`, `${d}\hat{i}${d}`.
* Greek Letters & Math Symbols: Use standard LaTeX commands: `\theta`, `\pi`, `\alpha`, `\beta`, `\gamma`, `\Delta`, `\Omega`, `^\circ`, `\pm`, `\times`, `\div`, `\cdot`, `\leq`, `\geq`, `\neq`, `\approx`, `\equiv`, `\infty`, `\rightarrow`, `\Rightarrow`, `\rightleftharpoons`.
* Keep every question and option on a SINGLE line (use inline `${d}...${d}`, do NOT use multiline `${d}${d}` blocks).

### CRITICAL REQUIREMENT 4: Options & Escaping Quality
* Exactly 4 non-empty options per question. Only one correct answer index (0-indexed: 0, 1, 2, or 3).
* NEVER leave any option blank or empty.
* Keep HTML tags like <br>, <hr>, <p>, <div> as literal text on a single line (do NOT render them as actual line breaks).
* Inside double-quoted strings, always use single quotes ('...') instead of unescaped double quotes.

### CRITICAL REQUIREMENT 5: Strict Output Format
Return ONLY the raw quiz objects wrapped inside a single ```text code block so that LaTeX commands, backslashes, and tags are preserved verbatim:
```text
{ q: "यदि ${d}z = \cos\theta + i\sin\theta${d} है, तो ${d}z^n + z^{-n}${d} किसके बराबर है?", o: ["${d}2\cos(n\theta)${d}", "${d}2\sin(n\theta)${d}", "${d}2i\sin(n\theta)${d}", "${d}2i\cos(n\theta)${d}"], a: 0 },
```

$topicSection"""
        }

        val AI_PROMPT_TEMPLATE: String = buildAiPrompt()
    }

    init {
        // Clear any old default demo quizzes so saved quizzes list is completely clean
        val json = prefs.getString(KEY_SAVED_QUIZZES, "[]") ?: "[]"
        if (json.contains("इमोजी मास्टर") || json.contains("Default") || json.contains("Science & GK")) {
            prefs.edit().putString(KEY_SAVED_QUIZZES, "[]").apply()
        }
    }

    fun getDarkModeOverride(): Boolean? {
        return if (prefs.contains(KEY_DARK_MODE)) {
            prefs.getBoolean(KEY_DARK_MODE, false)
        } else {
            null
        }
    }

    fun setDarkModeOverride(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun getQuestionTimeSeconds(): Int {
        return prefs.getInt(KEY_QUESTION_TIME, DEFAULT_QUESTION_TIME).coerceIn(5, 600)
    }

    fun setQuestionTimeSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_QUESTION_TIME, seconds.coerceIn(5, 600)).apply()
    }

    fun getQuizMode(): com.example.model.QuizMode {
        val raw = prefs.getString(KEY_QUIZ_MODE, com.example.model.QuizMode.PRACTICE.name)
        return try {
            com.example.model.QuizMode.valueOf(raw ?: com.example.model.QuizMode.PRACTICE.name)
        } catch (_: Exception) {
            com.example.model.QuizMode.PRACTICE
        }
    }

    fun setQuizMode(mode: com.example.model.QuizMode) {
        prefs.edit().putString(KEY_QUIZ_MODE, mode.name).apply()
    }

    fun getMarkingScheme(): com.example.model.MarkingScheme {
        val pos = prefs.getString(KEY_POSITIVE_MARKS, "1.0")?.toDoubleOrNull()?.coerceIn(0.25, 100.0) ?: 1.0
        val neg = prefs.getString(KEY_NEGATIVE_MARKS, "0.25")?.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.25
        return com.example.model.MarkingScheme(positiveMarks = pos, negativeMarks = neg)
    }

    fun setMarkingScheme(scheme: com.example.model.MarkingScheme) {
        prefs.edit()
            .putString(KEY_POSITIVE_MARKS, scheme.positiveMarks.coerceIn(0.25, 100.0).toString())
            .putString(KEY_NEGATIVE_MARKS, scheme.negativeMarks.coerceIn(0.0, 100.0).toString())
            .apply()
    }

    fun getCustomApiKey(): String {
        return prefs.getString(KEY_CUSTOM_GEMINI_API_KEY, "") ?: ""
    }

    fun setCustomApiKey(apiKey: String) {
        prefs.edit().putString(KEY_CUSTOM_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    fun getSavedQuizzes(): List<SavedQuiz> {
        val json = prefs.getString(KEY_SAVED_QUIZZES, "[]") ?: "[]"
        val list = mutableListOf<SavedQuiz>()
        var needsRepairSave = false
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                var rawCategory = obj.optString("category", "General").trim().ifBlank { "General" }
                var rawData = obj.optString("data", "").trim()
                var qCount = obj.optInt("questionCount", 0)

                // Self-heal if category and data were previously swapped
                if ((rawCategory.contains("{") || rawCategory.length > 50) && !rawData.contains("{")) {
                    val temp = rawData
                    rawData = rawCategory
                    rawCategory = temp.lineSequence().firstOrNull()?.trim()?.take(30)?.ifBlank { "General" } ?: "General"
                    qCount = QuizParser.parse(rawData).questions.size
                    needsRepairSave = true
                } else if (rawCategory.contains("{") || rawCategory.contains("\n") || rawCategory.length > 35) {
                    rawCategory = "General"
                    needsRepairSave = true
                }

                if (qCount == 0 && rawData.contains("{")) {
                    qCount = QuizParser.parse(rawData).questions.size
                }

                list.add(
                    SavedQuiz(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Untitled Quiz").lineSequence().firstOrNull()?.trim()?.ifBlank { "Untitled Quiz" } ?: "Untitled Quiz",
                        category = rawCategory,
                        data = rawData,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        questionCount = qCount
                    )
                )
            }
        } catch (_: Exception) {}
        if (needsRepairSave && list.isNotEmpty()) {
            saveAllQuizzes(list)
        }
        return list
    }

    fun saveQuiz(name: String, data: String, category: String = "General"): SavedQuiz {
        val parseResult = QuizParser.parse(data)
        val qCount = parseResult.questions.size
        val cleanName = name.lineSequence().firstOrNull()?.trim()?.take(60)?.ifBlank {
            "Quiz " + android.text.format.DateFormat.format("dd/MM/yyyy", java.util.Date())
        } ?: ("Quiz " + android.text.format.DateFormat.format("dd/MM/yyyy", java.util.Date()))
        val rawCat = category.lineSequence().firstOrNull()?.trim() ?: "General"
        val cleanCategory = if (rawCat.isBlank() || rawCat.contains("{") || rawCat.length > 35) {
            "General"
        } else {
            rawCat
        }

        val currentList = getSavedQuizzes().toMutableList()
        val existingIdx = currentList.indexOfFirst {
            it.name.equals(cleanName, ignoreCase = true) && it.category.equals(cleanCategory, ignoreCase = true)
        }

        val savedQuiz = if (existingIdx >= 0) {
            val updated = currentList[existingIdx].copy(
                name = cleanName,
                category = cleanCategory,
                data = data,
                timestamp = System.currentTimeMillis(),
                questionCount = qCount
            )
            currentList.removeAt(existingIdx)
            currentList.add(0, updated)
            updated
        } else {
            val created = SavedQuiz(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                category = cleanCategory,
                data = data,
                timestamp = System.currentTimeMillis(),
                questionCount = qCount
            )
            currentList.add(0, created)
            created
        }

        saveAllQuizzes(currentList)
        return savedQuiz
    }

    fun deleteQuiz(quizId: String) {
        val currentList = getSavedQuizzes().filter { it.id != quizId }
        saveAllQuizzes(currentList)
    }

    fun clearAllQuizzes() {
        saveAllQuizzes(emptyList())
        saveStarredQuestions(emptyList())
    }

    private fun saveAllQuizzes(list: List<SavedQuiz>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("category", item.category.ifBlank { "General" })
                put("data", item.data)
                put("timestamp", item.timestamp)
                put("questionCount", item.questionCount)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_QUIZZES, array.toString()).apply()
    }

    fun getStarredQuestions(): List<QuizQuestion> {
        val json = prefs.getString(KEY_STARRED_QUESTIONS, "[]") ?: "[]"
        val list = mutableListOf<QuizQuestion>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val qText = obj.optString("q", "").trim()
                val optionsArray = obj.optJSONArray("o")
                val options = mutableListOf<String>()
                if (optionsArray != null) {
                    for (j in 0 until optionsArray.length()) {
                        options.add(optionsArray.optString(j, ""))
                    }
                }
                val answerIdx = obj.optInt("a", 0).coerceIn(0, (options.size - 1).coerceAtLeast(0))
                if (qText.isNotEmpty() && options.size >= 2) {
                    list.add(QuizQuestion(q = qText, o = options, a = answerIdx))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveStarredQuestions(questions: List<QuizQuestion>) {
        val array = JSONArray()
        for (item in questions) {
            val opts = JSONArray()
            item.o.forEach { opts.put(it) }
            val obj = JSONObject().apply {
                put("q", item.q)
                put("o", opts)
                put("a", item.a.coerceIn(0, (item.o.size - 1).coerceAtLeast(0)))
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_STARRED_QUESTIONS, array.toString()).apply()
    }

    fun formatQuestionsAsQuizText(questions: List<QuizQuestion>): String {
        if (questions.isEmpty()) return ""
        return questions.joinToString("\n") { q ->
            val escapedQ = q.q.replace("\"", "'")
            val escapedOpts = q.o.joinToString(", ") { opt -> "\"${opt.replace("\"", "'")}\"" }
            val safeA = q.a.coerceIn(0, (q.o.size - 1).coerceAtLeast(0))
            "{ q: \"$escapedQ\", o: [$escapedOpts], a: $safeA },"
        }
    }

    /**
     * Exports one or more quizzes (and optionally the Starred Questions Bank) into a clean,
     * human-readable, easy-to-edit text format:
     *
     * === QUIZ: Quiz Name ===
     * CATEGORY: Category Name
     * { q: "Question 1", o: ["A", "B", "C", "D"], a: 0 },
     * { q: "Question 2", o: ["A", "B", "C", "D"], a: 1 },
     */
    fun exportQuizzesToEditableText(
        quizzes: List<SavedQuiz>,
        starredQuestions: List<QuizQuestion> = emptyList()
    ): String {
        if (quizzes.isEmpty() && starredQuestions.isEmpty()) return ""
        val sb = StringBuilder()
        quizzes.forEachIndexed { index, quiz ->
            if (index > 0) sb.append("\n\n")
            sb.append("=== QUIZ: ${quiz.name.trim()} ===\n")
            sb.append("CATEGORY: ${quiz.category.trim().ifBlank { "General" }}\n")
            sb.append(quiz.data.trim())
        }
        if (starredQuestions.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append("\n\n")
            sb.append("=== QUIZ: Starred Questions Bank ===\n")
            sb.append("CATEGORY: __STARRED_BANK__\n")
            sb.append(formatQuestionsAsQuizText(starredQuestions))
        }
        return sb.toString().trim()
    }

    /**
     * Imports quizzes from the editable text format (=== QUIZ: Name === / CATEGORY: Cat / { q: ... })
     * OR from a JSON array backup OR from a plain block of { q: ... } questions.
     * Also detects and restores Starred Questions Bank blocks.
     * Returns the number of quizzes/banks imported.
     */
    fun importQuizzesFromText(rawText: String, fallbackName: String = "Imported Quiz"): Int {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return 0

        val currentList = getSavedQuizzes().toMutableList()
        val currentStarred = getStarredQuestions().toMutableList()
        var importedCount = 0
        var quizImported = false
        var starredImported = false

        // 1. Check if the text uses === QUIZ: ... === or # QUIZ: ... headers
        val headerRegex = Regex("""(?m)^(?:===\s*QUIZ\s*:|#\s*QUIZ\s*:|QUIZ\s*:)\s*(.+?)\s*(?:===)?\s*$""", RegexOption.IGNORE_CASE)
        val matches = headerRegex.findAll(trimmed).toList()

        if (matches.isNotEmpty()) {
            for (i in matches.indices) {
                val match = matches[i]
                val quizName = match.groupValues[1].trim().ifBlank { "Imported Quiz ${i + 1}" }
                val blockStart = match.range.last + 1
                val blockEnd = if (i + 1 < matches.size) matches[i + 1].range.first else trimmed.length
                val blockContent = trimmed.substring(blockStart, blockEnd).trim()

                var category = "General"
                val contentLines = blockContent.lines().toMutableList()
                val categoryLineIdx = contentLines.indexOfFirst {
                    it.trim().startsWith("CATEGORY:", ignoreCase = true)
                }
                if (categoryLineIdx >= 0) {
                    category = contentLines[categoryLineIdx]
                        .substringAfter(":", "General")
                        .trim()
                        .ifBlank { "General" }
                    contentLines.removeAt(categoryLineIdx)
                }

                val quizBody = QuizParser.sanitizeQuizText(contentLines.joinToString("\n")).trim()
                val parsed = QuizParser.parse(quizBody)
                if (parsed.questions.isNotEmpty()) {
                    val isStarredBank = category.equals("__STARRED_BANK__", ignoreCase = true) ||
                        quizName.equals("Starred Questions Bank", ignoreCase = true) ||
                        (category.equals("Starred", ignoreCase = true) && quizName.contains("Starred", ignoreCase = true))

                    if (isStarredBank) {
                        for (sq in parsed.questions) {
                            val exists = currentStarred.any { it.q.trim().equals(sq.q.trim(), ignoreCase = true) }
                            if (!exists) {
                                currentStarred.add(sq)
                            }
                        }
                        starredImported = true
                        importedCount++
                    } else {
                        upsertQuizInList(currentList, quizName, category, quizBody, parsed.questions.size)
                        quizImported = true
                        importedCount++
                    }
                }
            }
            if (quizImported) {
                saveAllQuizzes(currentList)
            }
            if (starredImported) {
                saveStarredQuestions(currentStarred)
            }
            if (importedCount > 0) {
                return importedCount
            }
        }

        // 2. Check if it is a JSON Array of saved quizzes [{"name":..., "category":..., "data":...}]
        if (trimmed.startsWith("[")) {
            try {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    if (obj.has("data") && obj.has("name")) {
                        val qName = obj.optString("name", "Imported Quiz").trim()
                        val qCat = obj.optString("category", "General").trim().ifBlank { "General" }
                        val qData = QuizParser.sanitizeQuizText(obj.optString("data", "")).trim()
                        val parsed = QuizParser.parse(qData)
                        if (parsed.questions.isNotEmpty()) {
                            upsertQuizInList(currentList, qName, qCat, qData, parsed.questions.size)
                            importedCount++
                        }
                    }
                }
                if (importedCount > 0) {
                    saveAllQuizzes(currentList)
                    return importedCount
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: Plain quiz questions block without === QUIZ: === header
        var singleCategory = "General"
        var singleName = fallbackName.ifBlank { "Imported Quiz" }
        val lines = trimmed.lines().toMutableList()
        val catIdx = lines.indexOfFirst { it.trim().startsWith("CATEGORY:", ignoreCase = true) }
        if (catIdx >= 0) {
            singleCategory = lines[catIdx].substringAfter(":", "General").trim().ifBlank { "General" }
            lines.removeAt(catIdx)
        }
        val body = QuizParser.sanitizeQuizText(lines.joinToString("\n")).trim()
        val parsed = QuizParser.parse(body)
        if (parsed.questions.isNotEmpty()) {
            upsertQuizInList(currentList, singleName, singleCategory, body, parsed.questions.size)
            saveAllQuizzes(currentList)
            return 1
        }

        return 0
    }

    private fun upsertQuizInList(
        currentList: MutableList<SavedQuiz>,
        name: String,
        category: String,
        data: String,
        questionCount: Int
    ) {
        val cleanName = name.trim().ifBlank { "Untitled Quiz" }
        val cleanCat = category.trim().ifBlank { "General" }
        val existingIdx = currentList.indexOfFirst {
            it.name.equals(cleanName, ignoreCase = true) && it.category.equals(cleanCat, ignoreCase = true)
        }
        if (existingIdx >= 0) {
            currentList[existingIdx] = currentList[existingIdx].copy(
                name = cleanName,
                category = cleanCat,
                data = data,
                timestamp = System.currentTimeMillis(),
                questionCount = questionCount
            )
        } else {
            currentList.add(
                0,
                SavedQuiz(
                    id = UUID.randomUUID().toString(),
                    name = cleanName,
                    category = cleanCat,
                    data = data,
                    timestamp = System.currentTimeMillis(),
                    questionCount = questionCount
                )
            )
        }
    }
}
