package com.example.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class QuizQuestion(
    val q: String,
    val o: List<String>,
    val a: Int,
    val isTimedOut: Boolean = false,
    val userAnswerIndex: Int? = null
)

enum class QuizMode {
    PRACTICE,
    EXAM
}

data class MarkingScheme(
    val positiveMarks: Double = 1.0,
    val negativeMarks: Double = 0.25
) {
    val hasNegativeMarking: Boolean
        get() = negativeMarks > 0.0001

    val shortLabel: String
        get() = if (hasNegativeMarking) {
            "+${formatNumber(positiveMarks)} / -${formatNumber(negativeMarks)}"
        } else {
            "+${formatNumber(positiveMarks)} / 0"
        }

    fun calculatePositiveScore(correctCount: Int): Double = correctCount * positiveMarks

    fun calculateNegativePenalty(wrongCount: Int): Double = wrongCount * negativeMarks

    fun calculateNetScore(correctCount: Int, wrongCount: Int): Double {
        return (correctCount * positiveMarks) - (wrongCount * negativeMarks)
    }

    fun calculateMaxMarks(totalQuestions: Int): Double {
        return totalQuestions * positiveMarks
    }

    fun formatScore(value: Double): String = formatNumber(value)

    companion object {
        fun formatNumber(value: Double): String {
            val roundedInt = value.roundToInt()
            if (abs(value - roundedInt) < 0.001) {
                return roundedInt.toString()
            }
            val formatted = String.format(Locale.US, "%.2f", value)
            return formatted.trimEnd('0').trimEnd('.')
        }

        fun formatScore(value: Double): String = formatNumber(value)

        val PRESETS: List<Pair<String, MarkingScheme>> = listOf(
            "SSC / Banking (+1 / -0.25)" to MarkingScheme(1.0, 0.25),
            "Railway / RRB (+1 / -0.33)" to MarkingScheme(1.0, 0.33),
            "SSC Tier-1 (+2 / -0.50)" to MarkingScheme(2.0, 0.50),
            "JEE / NEET (+4 / -1)" to MarkingScheme(4.0, 1.0),
            "No Negative (+1 / 0)" to MarkingScheme(1.0, 0.0)
        )
    }
}

data class SavedQuiz(
    val id: String,
    val name: String,
    val category: String = "General",
    val data: String,
    val timestamp: Long,
    val questionCount: Int
)

enum class ScreenState {
    HOME,
    DATABASE,
    QUIZ,
    RESULT
}

data class QuizGrade(
    val grade: String,
    val message: String
)

object QuizGrading {
    fun getGrade(accuracy: Int): QuizGrade {
        return when {
            accuracy >= 90 -> QuizGrade("A+", "Excellent! You are very smart 🎉")
            accuracy >= 80 -> QuizGrade("A", "Very good! Just work a little harder 👏")
            accuracy >= 70 -> QuizGrade("B", "Good performance! You can do even better 👍")
            accuracy >= 60 -> QuizGrade("C", "Not bad! Study a little more 📚")
            accuracy >= 40 -> QuizGrade("D", "Pay a little more attention 🔁")
            else -> QuizGrade("F", "Stay motivated! Try again 💪")
        }
    }
}

