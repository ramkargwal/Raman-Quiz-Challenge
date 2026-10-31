package com.example.data

import com.example.model.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject

data class ParseResult(
    val questions: List<QuizQuestion>,
    val skippedCount: Int,
    val totalFound: Int
)

object QuizParser {

    /**
     * Sanitizes pasted or edited quiz text so that HTML tags like `<br>` that were turned into
     * literal line breaks by browser/chat rich-text copying are restored back to `"<br>"`
     * without breaking lines in the input box.
     */
    fun sanitizeQuizText(rawText: String, rawHtml: String? = null): String {
        if (rawText.isEmpty() && rawHtml.isNullOrEmpty()) return ""

        var baseText = rawText

        // If clipboard also provided HTML and plain text lost tags (like <br>, <hr>, <img>, <input>),
        // recover literal tags that were inside quotes in the HTML payload.
        if (!rawHtml.isNullOrBlank() && rawHtml.contains("{") && rawHtml.contains(":")) {
            val recoveredFromHtml = recoverLiteralTagsFromClipboardHtml(rawHtml)
            if (recoveredFromHtml != null && recoveredFromHtml.contains("{")) {
                // Use recovered HTML text if plain text had newline-broken options or empty quotes ""
                val plainHasBrokenQuotes = Regex(""""\s*\r?\n\s*"""").containsMatchIn(baseText) ||
                    Regex("""\[\s*""|,\s*""\s*[,\]\}]""").containsMatchIn(baseText)
                if (plainHasBrokenQuotes || baseText.isBlank()) {
                    baseText = recoveredFromHtml
                }
            }
        }

        // 1. If an option or string literal is just a quoted newline `"\n"` (caused by <br> turning
        //    into a line break when copying from a chat/browser), restore it directly to `"<br>"`.
        var result = baseText.replace(
            Regex("""(["'])\s*\r?\n\s*\1"""),
            "$1<br>$1"
        )

        // 2. Inside options arrays `o: [ ... ]`, if a quoted option has a literal newline inside it
        //    (e.g. `"Line 1\nLine 2"` where <br> broke the line inside the option), restore to `<br>`.
        val optionsArrayPattern = Regex("""(?i)(\b(?:o|options|opts|choices|answers)"?\s*:\s*\[)([\s\S]*?)(\]\s*,\s*"?(?:a|ans|answer|correct|correctAnswer|correct_answer|right)\b)""")
        result = optionsArrayPattern.replace(result) { match ->
            val prefix = match.groupValues[1]
            val content = match.groupValues[2]
            val suffix = match.groupValues[3]
            val fixedContent = repairNewlinesInsideQuotedItems(content)
            "$prefix$fixedContent$suffix"
        }

        return result
    }

    private fun repairNewlinesInsideQuotedItems(arrayContent: String): String {
        val sb = StringBuilder()
        var inQuote = false
        var quoteChar = '"'
        var i = 0
        while (i < arrayContent.length) {
            val c = arrayContent[i]
            if (!inQuote) {
                if (c == '"' || c == '\'') {
                    inQuote = true
                    quoteChar = c
                }
                sb.append(c)
                i++
            } else {
                if (c == '\\' && i + 1 < arrayContent.length) {
                    sb.append(c).append(arrayContent[i + 1])
                    i += 2
                    continue
                }
                if (c == quoteChar) {
                    inQuote = false
                    sb.append(c)
                    i++
                    continue
                }
                if (c == '\r' || c == '\n') {
                    // Skip consecutive \r\n and replace with literal <br>
                    if (c == '\r' && i + 1 < arrayContent.length && arrayContent[i + 1] == '\n') {
                        i += 2
                    } else {
                        i++
                    }
                    // Trim surrounding spaces around the accidental break and insert <br>
                    while (sb.isNotEmpty() && sb.last() == ' ') {
                        sb.deleteCharAt(sb.length - 1)
                    }
                    sb.append("<br>")
                    while (i < arrayContent.length && arrayContent[i] == ' ') {
                        i++
                    }
                    continue
                }
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    /**
     * Extracts quiz text from clipboard HTML when a browser/chat rendered tags like `<br>`, `<hr>`,
     * `<img>`, `<input>` inside option quotes `"..."` instead of keeping them as plain text.
     */
    private fun recoverLiteralTagsFromClipboardHtml(html: String): String? {
        return try {
            // Preserve any HTML tag that sits directly between quotes: e.g. "<br>", "<hr>", "<img>"
            var s = html.replace(
                Regex("""(["'])\s*<([a-zA-Z0-9]+(?:\s[^>]*)?/?>)\s*\1"""),
                "$1&lt;$2&gt;$1".replace("&gt;&gt;", "&gt;")
            )
            // Also fix any self-closing/opening tag directly inside quotes that got double `>`
            s = s.replace("&gt;&gt;", "&gt;")

            // Convert block-level line breaks outside quotes to newlines
            s = s.replace(Regex("""(?i)</(p|div|li|tr|pre|h[1-6])>"""), "\n")
            // Strip remaining HTML wrapper tags (like <span style="...">, <code>, <pre>, <b>)
            s = s.replace(Regex("""<[^>]+>"""), "")
            // Decode HTML entities back to literal characters (<, >, &, ", ')
            s = decodeHtmlEntities(s)
            sanitizeQuizText(s, null).trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }

    fun parse(rawText: String): ParseResult {
        val sanitized = sanitizeQuizText(rawText)
        val trimmed = sanitized.trim()
        if (trimmed.isEmpty()) {
            return ParseResult(emptyList(), 0, 0)
        }

        // Clean markdown code blocks if user pasted from AI chat
        var cleaned = trimmed
        cleaned = cleaned.replace(Regex("""^```[a-zA-Z0-9_-]*\s*"""), "")
        cleaned = cleaned.replace(Regex("""\s*```$"""), "")
        cleaned = cleaned.trim().trimEnd(',')

        // Strategy 1: Try parsing the whole input as a JSON array first
        try {
            val jsonFormatted = convertRelaxedJsToJsonArray(cleaned)
            val jsonArray = JSONArray(jsonFormatted)
            val parsedList = mutableListOf<QuizQuestion>()
            var skipped = 0

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i)
                if (item != null) {
                    val question = parseQuestionFromJsonObject(item)
                    if (question != null) {
                        parsedList.add(question)
                    } else {
                        skipped++
                    }
                }
            }

            if (parsedList.isNotEmpty() && skipped == 0) {
                return ParseResult(parsedList, 0, parsedList.size)
            }
        } catch (_: Exception) {}

        // Strategy 2: Extract individual { ... } blocks and parse each block resiliently
        val blocks = extractObjectBlocks(cleaned)
        val validQuestions = mutableListOf<QuizQuestion>()
        var skippedCount = 0

        for (block in blocks) {
            val question = parseSingleBlockResilient(block)
            if (question != null) {
                validQuestions.add(question)
            } else {
                skippedCount++
            }
        }

        return ParseResult(validQuestions, skippedCount, blocks.size)
    }

    private fun parseSingleBlockResilient(blockText: String): QuizQuestion? {
        // First try converting this individual block to a JSONObject
        try {
            val jsonFormatted = convertRelaxedJsToJsonArray(blockText)
            val arr = JSONArray(jsonFormatted)
            val obj = arr.optJSONObject(0)
            if (obj != null) {
                val parsed = parseQuestionFromJsonObject(obj)
                if (parsed != null) return parsed
            }
        } catch (_: Exception) {}

        // Fallback to direct field extraction with smart quote/bracket handling
        return parseBlockDirectly(blockText)
    }

    private fun parseQuestionFromJsonObject(item: JSONObject): QuizQuestion? {
        // Resolve Question text
        val q = item.optString("q")
            .ifEmpty { item.optString("question") }
            .ifEmpty { item.optString("ques") }
            .ifEmpty { item.optString("title") }
            .trim()

        if (q.isEmpty()) return null

        // Resolve Options
        val oArray = item.optJSONArray("o")
            ?: item.optJSONArray("options")
            ?: item.optJSONArray("opts")
            ?: item.optJSONArray("choices")
            ?: item.optJSONArray("answers")

        val options = mutableListOf<String>()
        if (oArray != null && oArray.length() >= 2) {
            for (i in 0 until oArray.length()) {
                val rawOpt = oArray.optString(i, "")
                val cleanedOpt = if (rawOpt.isNotEmpty() && rawOpt.trim().isEmpty() && rawOpt.contains("\n")) {
                    "<br>"
                } else {
                    rawOpt.trim()
                }
                if (cleanedOpt.isNotEmpty()) {
                    options.add(cleanedOpt)
                }
            }
        } else {
            // Check if options were provided as an object { a: "...", b: "...", c: "...", d: "..." }
            val oObj = item.optJSONObject("o") ?: item.optJSONObject("options")
            if (oObj != null) {
                val keys = oObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = oObj.optString(k, "").trim()
                    if (v.isNotEmpty()) options.add(v)
                }
            }
        }

        if (options.size < 2) return null

        // Resolve Answer
        val rawAnswer = when {
            item.has("a") -> item.get("a")
            item.has("ans") -> item.get("ans")
            item.has("answer") -> item.get("answer")
            item.has("correct") -> item.get("correct")
            item.has("correctAnswer") -> item.get("correctAnswer")
            item.has("correct_answer") -> item.get("correct_answer")
            item.has("right") -> item.get("right")
            else -> null
        }

        val answerIndex = resolveAnswerIndex(rawAnswer, options)
        return QuizQuestion(q, options, answerIndex)
    }

    private fun parseBlockDirectly(blockText: String): QuizQuestion? {
        val q = extractField(blockText, listOf("q", "question", "ques", "title")) ?: return null
        if (q.isEmpty()) return null

        val options = extractOptionsArray(blockText) ?: return null
        if (options.size < 2) return null

        val rawAnswer = extractAnswerRaw(blockText)
        val answerIndex = resolveAnswerIndex(rawAnswer, options)
        return QuizQuestion(q, options, answerIndex)
    }

    fun resolveAnswerIndex(rawAnswer: Any?, options: List<String>): Int {
        if (options.isEmpty()) return 0
        if (rawAnswer == null) return 0

        when (rawAnswer) {
            is Number -> {
                val num = rawAnswer.toInt()
                // If 0-indexed and within range
                if (num in 0 until options.size) return num
                // If 1-indexed (e.g. 4 for 4 options)
                if (num in 1..options.size) return num - 1
            }
            is String -> {
                val s = rawAnswer.trim()

                // Check direct integer string
                val num = s.toIntOrNull()
                if (num != null) {
                    if (num in 0 until options.size) return num
                    if (num in 1..options.size) return num - 1
                }

                // Check if string matches text of one of the options first (in case an option is "A" or "1")
                val exactMatch = options.indexOfFirst { it.trim().equals(s, ignoreCase = true) }
                if (exactMatch >= 0) return exactMatch

                // Check letter index e.g. "A", "B", "C", "D"
                if (s.length == 1) {
                    val ch = s.uppercase()[0]
                    if (ch in 'A'..'Z') {
                        val idx = ch - 'A'
                        if (idx in 0 until options.size) return idx
                    }
                }

                // Check "Option 1", "Option A", "Opt 2", etc.
                val optionLetterRegex = Regex("""^(?:option|opt|ans|choice)\s*([a-zA-Z]|\d+)$""", RegexOption.IGNORE_CASE)
                val m = optionLetterRegex.find(s)
                if (m != null) {
                    val capture = m.groupValues[1]
                    val asNum = capture.toIntOrNull()
                    if (asNum != null) {
                        if (asNum in 1..options.size) return asNum - 1
                        if (asNum in 0 until options.size) return asNum
                    }
                    val asChar = capture.uppercase()[0]
                    if (asChar in 'A'..'Z') {
                        val idx = asChar - 'A'
                        if (idx in 0 until options.size) return idx
                    }
                }

                // Check partial match
                val partialMatch = options.indexOfFirst { opt ->
                    val optClean = opt.trim()
                    optClean.isNotEmpty() && s.isNotEmpty() &&
                        (optClean.contains(s, ignoreCase = true) || s.contains(optClean, ignoreCase = true))
                }
                if (partialMatch >= 0) return partialMatch
            }
        }

        return 0
    }

    private fun extractField(text: String, keys: List<String>): String? {
        for (key in keys) {
            // Require key to be preceded by { or , or whitespace/start, not part of another word
            val keyPattern = Regex("""(?:^|[{\s,])"?$key"?\s*:\s*""")
            val keyMatch = keyPattern.find(text) ?: continue
            val valueStart = keyMatch.range.last + 1
            if (valueStart >= text.length) continue

            val quoteChar = text[valueStart]
            if (quoteChar == '"' || quoteChar == '\'') {
                // Scan until the closing quote that is followed by `,` and the next key (`o`, `options`, etc.)
                val nextFieldPattern = Regex("""^\s*,\s*"?(?:o|options|opts|choices|answers|a|ans|answer)\b""")
                val sb = StringBuilder()
                var i = valueStart + 1
                while (i < text.length) {
                    val c = text[i]
                    if (c == '\\' && i + 1 < text.length) {
                        sb.append(c).append(text[i + 1])
                        i += 2
                        continue
                    }
                    if (c == quoteChar) {
                        val remainder = text.substring(i + 1)
                        if (nextFieldPattern.containsMatchIn(remainder) || remainder.trimStart().startsWith("}")) {
                            break
                        }
                    }
                    sb.append(c)
                    i++
                }
                val extracted = unescapeString(sb.toString()).trim()
                if (extracted.isNotEmpty()) return extracted
            }
        }
        return null
    }

    private fun extractOptionsArray(text: String): List<String>? {
        val headerRegex = Regex("""(?:^|[{\s,])"?(?:o|options|opts|choices|answers)"?\s*:\s*\[""")
        val match = headerRegex.find(text) ?: return null
        val startBracketIndex = match.range.last // index of '['

        // Balanced bracket scan that respects quotes so options like "arr[0]" don't truncate the array
        var depth = 0
        var inQuote = false
        var quoteChar = '"'
        var endBracketIndex = -1

        var i = startBracketIndex
        while (i < text.length) {
            val c = text[i]
            if (inQuote) {
                if (c == '\\' && i + 1 < text.length) {
                    i += 2
                    continue
                }
                if (c == quoteChar) {
                    // Check if this quote is actually closing the option (followed by `,` or `]`)
                    val after = text.substring(i + 1).trimStart()
                    if (after.isEmpty() || after[0] == ',' || after[0] == ']') {
                        inQuote = false
                    }
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = true
                    quoteChar = c
                } else if (c == '[') {
                    depth++
                } else if (c == ']') {
                    depth--
                    if (depth == 0) {
                        endBracketIndex = i
                        break
                    }
                }
            }
            i++
        }

        if (endBracketIndex == -1 || endBracketIndex <= startBracketIndex) return null
        val arrayContent = text.substring(startBracketIndex + 1, endBracketIndex)

        // Split arrayContent into individual option elements respecting quotes
        val items = mutableListOf<String>()
        var idx = 0
        val len = arrayContent.length

        while (idx < len) {
            while (idx < len && (arrayContent[idx].isWhitespace() || arrayContent[idx] == ',')) {
                idx++
            }
            if (idx >= len) break

            val firstChar = arrayContent[idx]
            if (firstChar == '"' || firstChar == '\'') {
                val qChar = firstChar
                idx++
                val sb = StringBuilder()
                while (idx < len) {
                    val c = arrayContent[idx]
                    if (c == '\\' && idx + 1 < len) {
                        sb.append(c).append(arrayContent[idx + 1])
                        idx += 2
                        continue
                    }
                    if (c == qChar) {
                        val rest = arrayContent.substring(idx + 1).trimStart()
                        if (rest.isEmpty() || rest[0] == ',') {
                            idx++
                            break
                        }
                    }
                    sb.append(c)
                    idx++
                }
                val rawVal = unescapeString(sb.toString())
                val cleanedVal = if (rawVal.isNotEmpty() && rawVal.trim().isEmpty() && rawVal.contains("\n")) {
                    "<br>"
                } else {
                    rawVal.trim()
                }
                if (cleanedVal.isNotEmpty()) {
                    items.add(cleanedVal)
                }
            } else {
                // Unquoted option value until next comma
                val start = idx
                while (idx < len && arrayContent[idx] != ',') {
                    idx++
                }
                val rawVal = arrayContent.substring(start, idx).trim()
                if (rawVal.isNotEmpty()) {
                    items.add(unescapeString(rawVal))
                }
            }
        }

        return if (items.isNotEmpty()) items else null
    }

    private fun extractAnswerRaw(text: String): String? {
        val keys = listOf("a", "ans", "answer", "correct", "correctAnswer", "correct_answer", "right")
        // Search after the options array if possible so we never match `a:` inside the question or options
        val optionsHeader = Regex("""(?:^|[{\s,])"?(?:o|options|opts|choices|answers)"?\s*:\s*\[""").find(text)
        val searchRegion = if (optionsHeader != null) {
            text.substring(optionsHeader.range.first)
        } else {
            text
        }

        for (key in keys) {
            val regex = Regex("""(?:^|[,{\s\]])"?$key"?\s*:\s*(?:"([^"\\]*(?:\\.[^"\\]*)*)"|'([^'\\]*(?:\\.[^"\\]*)*)'|([a-zA-Z0-9_\-\.]+))""")
            val matches = regex.findAll(searchRegion).toList()
            if (matches.isNotEmpty()) {
                val lastMatch = matches.last()
                val v = lastMatch.groupValues[1]
                    .ifEmpty { lastMatch.groupValues[2] }
                    .ifEmpty { lastMatch.groupValues[3] }
                    .trim()
                if (v.isNotEmpty()) return unescapeString(v)
            }
        }
        return null
    }

    private fun convertRelaxedJsToJsonArray(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length - 1).trim()
        }
        if (s.endsWith(",")) s = s.substring(0, s.length - 1)

        val sb = StringBuilder("[")
        var i = 0
        val len = s.length

        while (i < len) {
            val ch = s[i]
            if (ch == '"') {
                sb.append(ch)
                i++
                while (i < len) {
                    val c = s[i]
                    i++
                    if (c == '\\' && i < len) {
                        val nextC = s[i]
                        val afterNext = if (i + 1 < len) s[i + 1] else ' '
                        val isLatexCmd = (nextC.isLetter() && (afterNext.isLetter() || nextC !in listOf('n', 'r', 't', 'b', 'f', 'u'))) ||
                            nextC in listOf('(', ')', '[', ']', '{', '}', ',', ';', '^', '_')
                        if (isLatexCmd) {
                            sb.append("\\\\").append(nextC)
                        } else {
                            sb.append(c).append(nextC)
                        }
                        i++
                        continue
                    }
                    if (c == '\r') continue
                    if (c == '\n') {
                        sb.append("\\n")
                        continue
                    }
                    if (c == '"') {
                        // Check if this quote is the real end of the JSON string (followed by `,`, `]`, `}`, or `:`)
                        var lookahead = i
                        while (lookahead < len && (s[lookahead] == ' ' || s[lookahead] == '\t' || s[lookahead] == '\r' || s[lookahead] == '\n')) {
                            lookahead++
                        }
                        val nextNonSpace = if (lookahead < len) s[lookahead] else '}'
                        if (nextNonSpace == ',' || nextNonSpace == ']' || nextNonSpace == '}' || nextNonSpace == ':') {
                            sb.append('"')
                            break
                        } else {
                            // Unescaped inner quote inside string! Escape it instead of breaking JSON
                            sb.append("\\\"")
                            continue
                        }
                    }
                    sb.append(c)
                }
                continue
            }
            if (ch == '\'') {
                sb.append('"')
                i++
                while (i < len) {
                    val c = s[i]
                    i++
                    if (c == '\\' && i < len) {
                        val nextC = s[i]
                        if (nextC == '\'') {
                            sb.append('\'')
                            i++
                            continue
                        }
                        val afterNext = if (i + 1 < len) s[i + 1] else ' '
                        val isLatexCmd = (nextC.isLetter() && (afterNext.isLetter() || nextC !in listOf('n', 'r', 't', 'b', 'f', 'u'))) ||
                            nextC in listOf('(', ')', '[', ']', '{', '}', ',', ';', '^', '_')
                        if (isLatexCmd) {
                            sb.append("\\\\").append(nextC)
                        } else {
                            sb.append(c).append(nextC)
                        }
                        i++
                        continue
                    }
                    if (c == '\r') continue
                    if (c == '\n') {
                        sb.append("\\n")
                        continue
                    }
                    if (c == '\'') break
                    if (c == '"') {
                        sb.append("\\\"")
                        continue
                    }
                    sb.append(c)
                }
                sb.append('"')
                continue
            }
            if (ch.isLetter() || ch == '_') {
                var ident = ""
                var k = i
                while (k < len && (s[k].isLetterOrDigit() || s[k] == '_')) {
                    ident += s[k]
                    k++
                }
                var m = k
                while (m < len && (s[m] == ' ' || s[m] == '\t' || s[m] == '\n' || s[m] == '\r')) {
                    m++
                }
                if (m < len && s[m] == ':') {
                    sb.append('"').append(ident).append('"')
                    i = k
                    continue
                }
                sb.append(ident)
                i = k
                continue
            }
            sb.append(ch)
            i++
        }
        sb.append("]")

        // Strip trailing commas before closing braces
        return sb.toString().replace(Regex(""",\s*([}\]])"""), "$1")
    }

    private fun extractObjectBlocks(text: String): List<String> {
        val list = mutableListOf<String>()
        var depth = 0
        var startIndex = -1
        var inQuote = false
        var quoteChar = ' '

        for (i in text.indices) {
            val c = text[i]
            if (inQuote) {
                if (c == quoteChar && (i == 0 || text[i - 1] != '\\')) {
                    inQuote = false
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = true
                    quoteChar = c
                } else if (c == '{') {
                    if (depth == 0) startIndex = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && startIndex != -1) {
                        list.add(text.substring(startIndex, i + 1))
                        startIndex = -1
                    }
                }
            }
        }
        return list
    }

    private fun unescapeString(str: String): String {
        return str
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace(Regex("""\\n(?![a-zA-Z])"""), "\n")
            .replace(Regex("""\\t(?![a-zA-Z])"""), "\t")
            .replace("\\\\", "\\")
    }
}
