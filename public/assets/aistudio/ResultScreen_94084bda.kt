package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QuizGrading
import com.example.model.QuizQuestion
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.Blue600
import com.example.ui.theme.Blue700
import com.example.ui.theme.Green600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Red600
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate700
import com.example.ui.theme.appBackgroundBrush
import com.example.ui.util.MathText
import com.example.viewmodel.QuizUiState
import com.example.viewmodel.QuizViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    uiState: QuizUiState,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.goHome()
    }

    val isDark = uiState.isDarkMode ?: isSystemInDarkTheme()
    val total = uiState.activeQuiz.size
    val wrongQuestionsOnly = uiState.wrongQuestions.filter { !it.isTimedOut }
    val wrongCount = wrongQuestionsOnly.size
    val correctCount = uiState.score
    val scheme = uiState.markingScheme

    val positiveEarned = scheme.calculatePositiveScore(correctCount)
    val negativePenalty = scheme.calculateNegativePenalty(wrongCount)
    val netScore = scheme.calculateNetScore(correctCount, wrongCount)
    val maxMarks = scheme.calculateMaxMarks(total)

    val accuracy = if (total > 0) ((correctCount.toFloat() / total) * 100).toInt() else 0
    val gradeInfo = QuizGrading.getGrade(accuracy)

    val gradientColors = when {
        accuracy >= 80 -> listOf(Green600, Color(0xFF15803D))
        accuracy >= 60 -> listOf(Blue600, Blue700)
        accuracy >= 40 -> listOf(Amber500, Amber600)
        else -> listOf(Red600, Color(0xFFB91C1C))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundBrush(isDark))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏆 Quiz Result",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Marking Scheme Pill on Result Header
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Indigo600.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("result_marking_scheme_badge")
                    ) {
                        Text(
                            text = "Marking Scheme: ${scheme.shortLabel}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFA5B4FC) else Indigo600,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Score Ring Circle (Shows Net Score / Max Marks)
                    Box(
                        modifier = Modifier
                            .size(146.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = scheme.formatScore(netScore),
                                fontSize = if (scheme.formatScore(netScore).length >= 5) 32.sp else 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "/ ${scheme.formatScore(maxMarks)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Net Marks",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Grade Badge
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = gradientColors[0]
                    ) {
                        Text(
                            text = "Grade ${gradeInfo.grade}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = gradeInfo.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Positive & Negative Marking Breakdown Strip
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+Marks",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+${scheme.formatScore(positiveEarned)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Green600
                                )
                            }
                            Text(
                                text = "−",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Negative",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "-${scheme.formatScore(negativePenalty)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Red600
                                )
                            }
                            Text(
                                text = "=",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Net Score",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${scheme.formatScore(netScore)} / ${scheme.formatScore(maxMarks)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF60A5FA) else Blue600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("Accuracy", "$accuracy%", gradientColors[0], Modifier.weight(1f))
                        StatPill("Correct", "$correctCount", Green600, Modifier.weight(1f))
                        StatPill("Wrong", "$wrongCount", Red600, Modifier.weight(1f))
                        StatPill("Skipped", "${uiState.skipped}", Amber500, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Actions
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uiState.wrongQuestions.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.toggleWrongReview() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Amber500,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.testTag("toggle_review_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.showWrongReview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (uiState.showWrongReview) "Hide" else "Wrong")
                            }

                            Button(
                                onClick = { viewModel.retryWrongAnswers() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Rose500,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.testTag("retry_wrong_button")
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Wrong")
                            }
                        }

                        Button(
                            onClick = { viewModel.startQuiz() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Blue600,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.testTag("try_again_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Try Again")
                        }

                        Button(
                            onClick = { viewModel.shareActiveQuizAsPdf(includeAnswerKey = true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo600,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.testTag("result_pdf_paper_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF Paper")
                        }

                        Button(
                            onClick = { viewModel.goHome() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Slate700,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.testTag("home_button")
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Home")
                        }
                    }
                }
            }
        }

        // Expandable Review Section
        if (uiState.showWrongReview && uiState.wrongQuestions.isNotEmpty()) {
            item {
                Text(
                    text = "Review of Wrong Answers:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 700.dp)
                )
            }

            items(uiState.wrongQuestions) { q ->
                val isStarred = remember(q, uiState.starredQuestions) {
                    viewModel.isQuestionStarred(q)
                }
                ReviewQuestionItem(
                    question = q,
                    isStarred = isStarred,
                    onToggleStar = { viewModel.toggleStarQuestion(q) },
                    isDark = isDark,
                    modifier = Modifier.widthIn(max = 700.dp)
                )
            }
        }
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ReviewQuestionItem(
    question: QuizQuestion,
    isStarred: Boolean = false,
    onToggleStar: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, Red600.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF3F1D1D) else Color(0xFFFEF2F2)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                MathText(
                    text = "Q: ${question.q}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isStarred) Amber500.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                ) {
                    Icon(
                        imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isStarred) "Unstar Question" else "Star Question",
                        tint = if (isStarred) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            val userIdx = question.userAnswerIndex
            if (userIdx != null) {
                val userChoice = question.o.getOrElse(userIdx) { "" }
                MathText(
                    text = "✖ Your Answer: $userChoice",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFF87171) else Red600
                )
                Spacer(modifier = Modifier.height(4.dp))
            } else if (question.isTimedOut) {
                Text(
                    text = "⏰ Status: Skipped / Not Answered",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Amber500
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            val correctAnswer = question.o.getOrElse(question.a) { "" }
            MathText(
                text = "✔ Correct Answer: $correctAnswer",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF4ADE80) else Green600
            )
        }
    }
}
