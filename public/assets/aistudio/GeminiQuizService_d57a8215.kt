package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class GeneratedQuizPayload(
    val title: String,
    val category: String,
    val quizData: String
)

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiQuizGenerator {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Ordered chain of supported Gemini models to automatically recover from HTTP 503 (Model Overloaded) or 429
    private val MODEL_FALLBACK_CHAIN = listOf(
        "gemini-flash-latest",
        "gemini-3.1-flash-lite-preview",
        "gemini-3.5-flash",
        "gemini-3.1-pro-preview"
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun resolveApiKey(customKey: String): String {
        val trimmedCustom = customKey.trim()
        if (trimmedCustom.isNotEmpty()) return trimmedCustom
        val buildConfigKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildConfigKey.isNotEmpty() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return buildConfigKey
        }
        return ""
    }

    suspend fun generateQuizData(
        topicOrNotes: String,
        questionCount: Int,
        difficulty: String,
        languageStyle: String,
        customApiKey: String
    ): Result<GeneratedQuizPayload> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API Key is not set. Please tap 'Built-in API Key (Tap to Change)' to enter a key or configure GEMINI_API_KEY in AI Studio Secrets.")
            )
        }

        val langInstruction = when (languageStyle) {
            "English" -> "Write all questions and options in clear English."
            "Hindi", "Pure Hindi" -> "Write all questions and options in Hindi (Devanagari script)."
            else -> "Write questions primarily in Hindi (Devanagari script), while keeping technical terms, scientific names, code/HTML tags (like <br>, <div>), acronyms, and formulas in English."
        }

        val d = "$"
        val fullPrompt = """
            You are an expert Quiz Master and Competitive Exam Designer.
            Generate EXACTLY $questionCount unique multiple-choice questions based on the topic or study notes below.
            
            Requirements:
            1. Quantity: $questionCount questions.
            2. Difficulty: $difficulty.
            3. Language: $langInstruction
            4. Provide a short, suitable "title" for this quiz (2 to 5 words, max 35 characters).
            5. Provide a short 1-2 word "category" for this quiz (e.g., "Computer", "Science", "GK", "History", "Polity", "Geography", "Math", "Hindi", "English", or "General").
            6. Each question MUST have strictly 4 non-empty options in the array "o".
            7. "a" MUST be the 0-indexed integer (0, 1, 2, or 3) of the correct option.
            8. Pure LaTeX Math & Formula Formatting (${d}...${d}):
               - Write ALL math expressions, equations, fractions, exponents, subscripts, roots, limits, and chemical formulas in standard LaTeX wrapped in `${d}...${d}`.
               - For fractions, always use `\frac{a}{b}` (e.g., `${d}\frac{1}{\sin\theta}${d}`, `${d}\frac{x + 1}{x - 1}${d}`).
               - For powers/exponents, always use `^{...}` (e.g., `${d}z^n + z^{-n}${d}`, `${d}x^{n+1}${d}`, `${d}10^{-6}${d}`, `${d}\sin^2\theta${d}`).
               - For subscripts, always use `_{...}` (e.g., `${d}a_n${d}`, `${d}a_{n-1}${d}`, `${d}\text{H}_2\text{O}${d}`).
               - For roots, always use `\sqrt{...}` or `\sqrt[n]{...}` (e.g., `${d}\sqrt{3}${d}`, `${d}\sqrt{x^2 + y^2}${d}`).
               - For Greek letters & symbols, use standard LaTeX commands (`\theta`, `\pi`, `\alpha`, `\beta`, `\gamma`, `\Delta`, `\Omega`, `^\circ`, `\pm`, `\times`, `\div`, `\cdot`, `\leq`, `\geq`, `\neq`, `\infty`, `\rightarrow`, `\rightleftharpoons`).
               - Keep HTML/code tags (such as <br>, <p>, <div>) as literal text on a single line.
            9. Return ONLY a valid JSON object in this exact structure:
            {
              "title": "Suitable Quiz Name",
              "category": "Category",
              "questions": [
                { "q": "Question text", "o": ["Option 1", "Option 2", "Option 3", "Option 4"], "a": 0 }
              ]
            }
            
            Topic / Study Notes:
            $topicOrNotes
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = fullPrompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.6f
            )
        )

        var lastException: Exception? = null

        for ((index, modelName) in MODEL_FALLBACK_CHAIN.withIndex()) {
            try {
                val response = service.generateContent(
                    model = modelName,
                    apiKey = apiKey,
                    request = request
                )
                val rawText = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()

                if (!rawText.isNullOrBlank()) {
                    val payload = parseQuizPayload(rawText, topicOrNotes)
                    if (payload != null && payload.quizData.isNotBlank()) {
                        return@withContext Result.success(payload)
                    }
                }
            } catch (e: HttpException) {
                lastException = e
                val code = e.code()
                val errBody = try { e.response()?.errorBody()?.string().orEmpty() } catch (_: Exception) { "" }
                if (code == 400 && errBody.contains("API_KEY_INVALID", ignoreCase = true)) {
                    return@withContext Result.failure(
                        IllegalStateException("Invalid Gemini API Key. Please check your API key.")
                    )
                }
                if (code == 403) {
                    return@withContext Result.failure(
                        IllegalStateException("API Key permission denied (HTTP 403). Please verify your Gemini API Key.")
                    )
                }
                if (index < MODEL_FALLBACK_CHAIN.lastIndex) {
                    delay(600L * (index + 1))
                    continue
                }
            } catch (e: Exception) {
                lastException = e
                if (index < MODEL_FALLBACK_CHAIN.lastIndex) {
                    delay(500L)
                    continue
                }
            }
        }

        val friendlyMsg = when (val ex = lastException) {
            is HttpException -> {
                if (ex.code() == 503) {
                    "AI server is currently busy (HTTP 503). Please tap Generate again in a few seconds."
                } else {
                    "Server error (HTTP ${ex.code()}). Please try again."
                }
            }
            else -> lastException?.localizedMessage ?: "Failed to generate quiz. Please check your internet connection."
        }
        Result.failure(IllegalStateException(friendlyMsg))
    }

    private fun escapeLatexBackslashesInJson(raw: String): String {
        val sb = StringBuilder(raw.length + 32)
        var inQuotes = false
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '"') {
                inQuotes = !inQuotes
                sb.append(c)
                i++
                continue
            }
            if (inQuotes && c == '\\' && i + 1 < raw.length) {
                val next = raw[i + 1]
                val afterNext = if (i + 2 < raw.length) raw[i + 2] else ' '
                when {
                    next == '"' || next == '/' -> {
                        sb.append(c).append(next)
                        i += 2
                    }
                    next == '\\' -> {
                        sb.append("\\\\")
                        i += 2
                    }
                    next == 'u' && i + 5 < raw.length && raw.substring(i + 2, i + 6).all { it.isDigit() || it.lowercaseChar() in 'a'..'f' } -> {
                        sb.append(raw.substring(i, i + 6))
                        i += 6
                    }
                    next.isLetter() && (afterNext.isLetter() || next !in listOf('n', 'r', 't')) -> {
                        // Escape single backslash on LaTeX commands like \frac, \theta, \tan, \beta, \rho, \neq, \sin, \cos, \sqrt
                        sb.append("\\\\").append(next)
                        i += 2
                    }
                    next in listOf('(', ')', '[', ']', '{', '}', ',', ';', ':', '!', '|', '^', '_') -> {
                        sb.append("\\\\").append(next)
                        i += 2
                    }
                    else -> {
                        sb.append(c).append(next)
                        i += 2
                    }
                }
                continue
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun parseQuizPayload(rawJson: String, topicOrNotes: String): GeneratedQuizPayload? {
        val cleanedJson = rawJson
            .removePrefix("```json")
            .removePrefix("```text")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        var aiTitle: String? = null
        var aiCategory: String? = null
        var questionsArray: JsonArray? = null

        try {
            val safeJson = escapeLatexBackslashesInJson(cleanedJson)
            val root = json.parseToJsonElement(safeJson)
            if (root is JsonObject) {
                aiTitle = root["title"]?.jsonPrimitive?.content?.trim()
                aiCategory = root["category"]?.jsonPrimitive?.content?.trim()
                questionsArray = (root["questions"] ?: root["items"] ?: root["quiz"])?.jsonArray
            } else if (root is JsonArray) {
                questionsArray = root
            }
        } catch (_: Exception) {}

        val formattedLines = if (questionsArray != null) {
            formatJsonArrayToQuizLines(questionsArray)
        } else {
            fallbackParse(cleanedJson)
        }

        if (formattedLines.isBlank()) return null

        val finalTitle = deriveSmartTitle(topicOrNotes, aiTitle)
        val finalCategory = deriveSmartCategory(topicOrNotes, aiCategory)

        return GeneratedQuizPayload(
            title = finalTitle,
            category = finalCategory,
            quizData = formattedLines
        )
    }

    private fun deriveSmartTitle(topicOrNotes: String, aiTitle: String?): String {
        val candidate = aiTitle
            ?.replace("\r\n", " ")
            ?.replace("\n", " ")
            ?.replace("\"", "")
            ?.trim()
            .orEmpty()
        if (candidate.isNotEmpty() && !candidate.equals("Suitable Quiz Name", ignoreCase = true)) {
            return candidate.take(45)
        }

        val firstLine = topicOrNotes
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
            .replace(Regex("[#*_`~>]+"), "")
            .trim()

        if (firstLine.isEmpty()) return "AI Generated Quiz"
        val words = firstLine.split(Regex("\\s+")).take(5).joinToString(" ")
        return words.take(40).ifBlank { "AI Generated Quiz" }
    }

    private fun deriveSmartCategory(topicOrNotes: String, aiCategory: String?): String {
        val cleanCat = aiCategory
            ?.replace("\r\n", " ")
            ?.replace("\n", " ")
            ?.replace("\"", "")
            ?.trim()
            .orEmpty()
        if (cleanCat.isNotEmpty() && !cleanCat.equals("Category", ignoreCase = true) && cleanCat.length <= 24) {
            return cleanCat
        }
        val lower = topicOrNotes.lowercase()
        return when {
            lower.contains("html") || lower.contains("css") || lower.contains("computer") ||
                lower.contains("python") || lower.contains("java") || lower.contains("code") -> "Computer"
            lower.contains("science") || lower.contains("physics") || lower.contains("chemistry") ||
                lower.contains("biology") || lower.contains("vitamin") -> "Science"
            lower.contains("history") || lower.contains("polity") || lower.contains("constitution") ||
                lower.contains("geography") || lower.contains("india") || lower.contains("gk") -> "GK"
            lower.contains("math") || lower.contains("algebra") || lower.contains("geometry") -> "Math"
            else -> "General"
        }
    }

    private fun formatJsonArrayToQuizLines(array: JsonArray): String {
        val sb = StringBuilder()
        for (elem in array) {
            try {
                val obj = elem.jsonObject
                val qText = obj["q"]?.jsonPrimitive?.content
                    ?.replace("\r\n", " ")
                    ?.replace("\n", " ")
                    ?.replace("\"", "'")
                    ?.trim() ?: continue
                val optsArray = obj["o"]?.jsonArray ?: continue
                val opts = optsArray.mapNotNull {
                    it.jsonPrimitive.content
                        .replace("\r\n", " ")
                        .replace("\n", " ")
                        .replace("\"", "'")
                        .trim()
                        .takeIf { s -> s.isNotEmpty() }
                }
                if (qText.isEmpty() || opts.size < 2) continue
                val paddedOpts = if (opts.size >= 4) opts.take(4) else opts + List(4 - opts.size) { "Option ${opts.size + it + 1}" }
                val ans = (obj["a"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0).coerceIn(0, 3)
                val optsFormatted = paddedOpts.joinToString(", ") { "\"$it\"" }
                sb.append("{ q: \"$qText\", o: [$optsFormatted], a: $ans },\n")
            } catch (_: Exception) {}
        }
        return sb.toString().trim()
    }

    private fun fallbackParse(rawText: String): String {
        val parsed = QuizParser.parse(rawText)
        return if (parsed.questions.isNotEmpty()) {
            parsed.questions.joinToString("\n") { q ->
                val cleanQ = q.q.replace("\"", "'").replace("\n", " ")
                val optsFormatted = q.o.joinToString(", ") { "\"${it.replace("\"", "'").replace("\n", " ")}\"" }
                "{ q: \"$cleanQ\", o: [$optsFormatted], a: ${q.a} },"
            }
        } else {
            ""
        }
    }
}
