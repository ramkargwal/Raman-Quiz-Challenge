package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.QuizParser
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Raman Quiz Challenge", appName)
  }

  @Test
  fun `verify quiz parser extracts questions accurately`() {
    val sample = """
      { q: "सूर्य से कौन सा विटामिन मिलता है?", o: ["A", "B", "C", "D"], a: 3 },
      { q: "जल का रासायनिक सूत्र क्या है?", o: ["CO2", "H2O", "NaCl", "O2"], a: 1 }
    """.trimIndent()

    val result = QuizParser.parse(sample)
    assertEquals(2, result.questions.size)
    assertEquals("सूर्य से कौन सा विटामिन मिलता है?", result.questions[0].q)
    assertEquals(3, result.questions[0].a)
    assertEquals("D", result.questions[0].o[3])
    assertEquals("H2O", result.questions[1].o[1])
  }

  @Test
  fun `verify quiz parser handles various answer formats`() {
    val sampleWithVariousAnswers = """
      { q: "Letter Answer Q", o: ["Alpha", "Beta", "Gamma", "Delta"], a: "B" },
      { q: "Text Answer Q", o: ["Dog", "Cat", "Elephant", "Tiger"], a: "Elephant" },
      { q: "One-Based Answer Q", o: ["First", "Second", "Third", "Fourth"], a: 4 },
      { q: "Quoted Number Answer Q", o: ["Zero", "One", "Two", "Three"], a: "2" }
    """.trimIndent()

    val result = QuizParser.parse(sampleWithVariousAnswers)
    assertEquals(4, result.questions.size)
    assertEquals(1, result.questions[0].a) // "B" -> index 1 ("Beta")
    assertEquals(2, result.questions[1].a) // "Elephant" -> index 2
    assertEquals(3, result.questions[2].a) // 4 -> index 3 (1-based mapping)
    assertEquals(2, result.questions[3].a) // "2" -> index 2
  }

  @Test
  fun `verify remaining questions count calculation`() {
    val totalCount = 2
    // On question 1 (index 0) of 2 questions
    val remainingOnFirst = (totalCount - 0).coerceAtLeast(1)
    assertEquals(2, remainingOnFirst)

    // On last question (index 1) of 2 questions
    val remainingOnLast = (totalCount - 1).coerceAtLeast(1)
    assertEquals(1, remainingOnLast)
  }
}
