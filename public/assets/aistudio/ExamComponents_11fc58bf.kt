package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.Amber500
import com.example.ui.theme.Blue600
import com.example.ui.theme.Green600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Red600
import com.example.viewmodel.QuizUiState
import com.example.viewmodel.QuizViewModel

fun formatExamTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

@Composable
fun ExamBottomBar(
    uiState: QuizUiState,
    viewModel: QuizViewModel,
    isDark: Boolean,
    onRequestSubmit: () -> Unit
) {
    val totalCount = uiState.activeQuiz.size
    val canGoPrev = uiState.currentIndex > 0
    val canGoNext = uiState.currentIndex < totalCount - 1
    val currentQ = uiState.activeQuiz.getOrNull(uiState.currentIndex)
    val isBookmarked = uiState.bookmarkedIndices.contains(uiState.currentIndex) ||
        viewModel.isQuestionStarred(currentQ)
    val hasSelectedAnswer = uiState.examAnswers.containsKey(uiState.currentIndex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Utility Row: Clear Response + Submit Exam
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear Option Pill (only when an option is selected)
                    if (hasSelectedAnswer) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Red600,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.clearExamOption() }
                                .testTag("exam_clear_option_button")
                        ) {
                            Text(
                                text = "Clear",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // Submit Exam Button
                Button(
                    onClick = onRequestSubmit,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green600,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("exam_submit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Submit Exam",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Bottom Navigation Row: Previous, Star Question, & Next Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.prevExamQuestion() },
                    enabled = canGoPrev,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("exam_prev_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo600,
                        contentColor = Color.White,
                        disabledContainerColor = Indigo600.copy(alpha = 0.45f),
                        disabledContentColor = Color.White.copy(alpha = 0.75f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Question",
                        modifier = Modifier.size(18.dp),
                        tint = if (canGoPrev) Color.White else Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Previous",
                        fontWeight = FontWeight.Bold,
                        color = if (canGoPrev) Color.White else Color.White.copy(alpha = 0.75f)
                    )
                }

                // Star Question Icon Button between Previous and Next
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isBookmarked && isDark -> Color(0xFF451A03)
                        isBookmarked && !isDark -> Color(0xFFFEF3C7)
                        isDark -> Color(0xFF1E293B)
                        else -> Color(0xFFF1F5F9)
                    },
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isBookmarked) Amber500 else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("quiz_star_question_button")
                ) {
                    IconButton(
                        onClick = { viewModel.toggleBookmark() },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isBookmarked) "Unstar Question" else "Star Question",
                            tint = if (isBookmarked) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (canGoNext) {
                            viewModel.nextExamQuestion()
                        } else {
                            onRequestSubmit()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("exam_next_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canGoNext) Blue600 else Indigo600,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (canGoNext) "Next" else "Finish & Submit",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (canGoNext) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.CheckCircle,
                        contentDescription = "Next Question",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamPaletteDialog(
    uiState: QuizUiState,
    viewModel: QuizViewModel,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRequestSubmit: () -> Unit
) {
    val totalCount = uiState.activeQuiz.size
    val answeredCount = uiState.examAnswers.size
    val bookmarkedCount = uiState.bookmarkedIndices.size
    val unansweredCount = (totalCount - answeredCount).coerceAtLeast(0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = Blue600,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Question Palette",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PaletteLegendItem("Answered ($answeredCount)", Green600)
                    PaletteLegendItem("Bookmarked ($bookmarkedCount)", Amber500)
                    PaletteLegendItem("Left ($unansweredCount)", Color(0xFF64748B))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Grid of Question Numbers
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (idx in 0 until totalCount) {
                            val isCurrent = idx == uiState.currentIndex
                            val isAnswered = uiState.examAnswers.containsKey(idx)
                            val isBookmarked = uiState.bookmarkedIndices.contains(idx)

                            val cellBg = when {
                                isBookmarked -> Amber500
                                isAnswered -> Green600
                                isDark -> Color(0xFF1E293B)
                                else -> Color(0xFFF1F5F9)
                            }
                            val cellText = when {
                                isBookmarked || isAnswered -> Color.White
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = cellBg,
                                border = BorderStroke(
                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                    color = if (isCurrent) Blue600 else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.goToQuestion(idx)
                                        onDismiss()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = cellText
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red600,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onRequestSubmit()
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green600,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Submit Exam", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ExamSubmitConfirmDialog(
    uiState: QuizUiState,
    onDismiss: () -> Unit,
    onConfirmSubmit: () -> Unit
) {
    val totalCount = uiState.activeQuiz.size
    val answeredCount = uiState.examAnswers.size
    val unansweredCount = (totalCount - answeredCount).coerceAtLeast(0)
    val bookmarkedCount = uiState.bookmarkedIndices.size

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Submit Exam?",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Check your exam summary before final submission:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPill("Total", "$totalCount", Blue600, Modifier.weight(1f))
                    StatPill("Done", "$answeredCount", Green600, Modifier.weight(1f))
                    StatPill("Skipped", "$unansweredCount", Amber500, Modifier.weight(1f))
                    StatPill("Starred", "$bookmarkedCount", Indigo600, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red600,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Back", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirmSubmit,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("confirm_submit_exam_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green600,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Submit Now", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
