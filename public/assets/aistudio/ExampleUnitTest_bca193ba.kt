package com.example

import com.example.data.QuizParser
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun brTagConvertedToNewlineOnPaste_isRestoredToLiteralBrTag() {
        val corruptedPaste = """
            { q: "What tag is used for break in line?", o: ["
            ", "<hr>", "<p>", "<div>"], a: 0 },
        """.trimIndent()

        val sanitized = QuizParser.sanitizeQuizText(corruptedPaste)
        assertTrue(sanitized.contains("\"<br>\""))

        val parsed = QuizParser.parse(corruptedPaste)
        assertEquals(1, parsed.questions.size)
        assertEquals(listOf("<br>", "<hr>", "<p>", "<div>"), parsed.questions[0].o)
        assertEquals(0, parsed.questions[0].a)
    }

    @Test
    fun optionsWithSquareBracketsAndInnerQuotes_parseWithoutBlankOptions() {
        val input = """
            { q: "How do you declare an "empty" array in JS?", o: ["let arr = []", "let arr = {}", "arr[0]", "None, of above"], a: 0 },
        """.trimIndent()

        val parsed = QuizParser.parse(input)
        assertEquals(1, parsed.questions.size)
        assertEquals(listOf("let arr = []", "let arr = {}", "arr[0]", "None, of above"), parsed.questions[0].o)
        assertEquals(0, parsed.questions[0].a)
    }
}
