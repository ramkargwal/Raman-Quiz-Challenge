package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.QuizMode
import com.example.ui.theme.Amber500
import com.example.ui.theme.Blue600
import com.example.ui.theme.Green600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.OptionCorrectBg
import com.example.ui.theme.OptionCorrectBorder
import com.example.ui.theme.OptionWrongBg
import com.example.ui.theme.OptionWrongBorder
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.appBackgroundBrush
import com.example.ui.util.MathText
import com.example.viewmodel.QuizUiState
import com.example.viewmodel.QuizViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizScreen(
    uiState: QuizUiState,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.isDarkMode ?: isSystemInDarkTheme()
    val isExamMode = uiState.quizMode == QuizMode.EXAM
    var showQtyDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var tempQty by remember { mutableStateOf(uiState.numQuestions) }

    BackHandler {
        showExitConfirmDialog = true
    }

    if (showExitConfirmDialog) {
        Dialog(onDismissRequest = { showExitConfirmDialog = false }) {
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
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Red600.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Red600,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isExamMode) "Exit Exam?" else "Exit Quiz?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isExamMode) {
                            "Are you sure you want to exit the exam? Your current exam progress will be lost."
                        } else {
                            "Are you sure you want to exit the quiz? Your current progress will be lost."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showExitConfirmDialog = false
                                viewModel.goHome()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("confirm_exit_quiz_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Red600,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Yes, Exit",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { showExitConfirmDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("cancel_exit_quiz_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green600,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "No",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPaletteDialog && isExamMode) {
        ExamPaletteDialog(
            uiState = uiState,
            viewModel = viewModel,
            isDark = isDark,
            onDismiss = { showPaletteDialog = false },
            onRequestSubmit = { showSubmitConfirmDialog = true }
        )
    }

    if (showSubmitConfirmDialog && isExamMode) {
        ExamSubmitConfirmDialog(
            uiState = uiState,
            onDismiss = { showSubmitConfirmDialog = false },
            onConfirmSubmit = {
                showSubmitConfirmDialog = false
                viewModel.submitExam()
            }
        )
    }

    val currentQ = uiState.activeQuiz.getOrNull(uiState.currentIndex)
    val totalCount = uiState.activeQuiz.size
    val currentNumber = uiState.currentIndex + 1
    val remainingCount = (totalCount - uiState.currentIndex).coerceAtLeast(1)

    // Animated score pop
    var prevScore by remember { mutableStateOf(uiState.score) }
    var scorePop by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.score) {
        if (uiState.score > prevScore) {
            scorePop = true
            kotlinx.coroutines.delay(350)
            scorePop = false
        }
        prevScore = uiState.score
    }

    val scoreScale by animateFloatAsState(
        targetValue = if (scorePop) 1.25f else 1.0f,
        animationSpec = spring(),
        label = "scoreScale"
    )

    if (showQtyDialog) {
        val maxQ = uiState.maxQuestions.coerceAtLeast(1)
        val defaultPresets = listOf(5, 10, 15, 20, 30)

        Dialog(onDismissRequest = { showQtyDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon + Title
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Blue600, Indigo600)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select No. of Questions",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Hero Counter Card with - / + Stepper
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Minus Stepper Button
                            val canDecrease = tempQty > 1
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    canDecrease && isDark -> Color(0xFF1E293B)
                                    canDecrease && !isDark -> Color.White
                                    isDark -> Color(0xFF1E293B).copy(alpha = 0.4f)
                                    else -> Color(0xFFE2E8F0)
                                },
                                shadowElevation = if (canDecrease) 3.dp else 0.dp,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = canDecrease) {
                                        tempQty = (tempQty - 1).coerceAtLeast(1)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease Questions",
                                        tint = if (canDecrease) Blue600 else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Center Counter Display
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$tempQty",
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF60A5FA) else Blue600
                                )
                                Text(
                                    text = "out of $maxQ Questions",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Plus Stepper Button
                            val canIncrease = tempQty < maxQ
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    canIncrease && isDark -> Color(0xFF1E293B)
                                    canIncrease && !isDark -> Color.White
                                    isDark -> Color(0xFF1E293B).copy(alpha = 0.4f)
                                    else -> Color(0xFFE2E8F0)
                                },
                                shadowElevation = if (canIncrease) 3.dp else 0.dp,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = canIncrease) {
                                        tempQty = (tempQty + 1).coerceAtMost(maxQ)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase Questions",
                                        tint = if (canIncrease) Blue600 else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Modern Custom Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = tempQty.toFloat(),
                            onValueChange = { tempQty = it.roundToInt().coerceIn(1, maxQ) },
                            valueRange = 1f..maxQ.coerceAtLeast(2).toFloat(),
                            enabled = maxQ > 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("question_qty_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = Indigo600,
                                activeTrackColor = Blue600,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline
                            ),
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .shadow(6.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF0F172A) else Color.White)
                                        .border(6.dp, Blue600, CircleShape)
                                )
                            },
                            track = { sliderState ->
                                val fraction = if (sliderState.valueRange.endInclusive > sliderState.valueRange.start) {
                                    ((sliderState.value - sliderState.valueRange.start) /
                                        (sliderState.valueRange.endInclusive - sliderState.valueRange.start)).coerceIn(0f, 1f)
                                } else {
                                    1f
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Blue600, Indigo600)
                                                )
                                            )
                                    )
                                }
                            }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Min: 1",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Max: $maxQ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Preset Chips (5, 10, 15, 20, 30,  All)
                    Text(
                        text = "Quick Select",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        defaultPresets.forEach { preset ->
                            val isAvailable = preset <= maxQ
                            val isSelected = tempQty == preset && isAvailable
                            val chipBg = when {
                                isSelected -> Blue600
                                isAvailable && isDark -> Color(0xFF0F172A)
                                isAvailable && !isDark -> Color(0xFFF1F5F9)
                                isDark -> Color(0xFF0F172A).copy(alpha = 0.45f)
                                else -> Color(0xFFF8FAFC)
                            }
                            val chipBorder = when {
                                isSelected -> Blue600
                                isAvailable && isDark -> Color(0xFF334155)
                                isAvailable && !isDark -> Color(0xFFCBD5E1)
                                isDark -> Color(0xFF1E293B)
                                else -> Color(0xFFE2E8F0)
                            }
                            val chipText = when {
                                isSelected -> Color.White
                                isAvailable -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = chipBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                                shadowElevation = if (isSelected) 4.dp else 0.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        tempQty = preset.coerceAtMost(maxQ)
                                    }
                                    .testTag("preset_qty_$preset")
                            ) {
                                Text(
                                    text = "$preset",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = chipText,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                )
                            }
                        }

                        // "All" Preset Chip
                        val isAllSelected = tempQty == maxQ
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isAllSelected -> Indigo600
                                isDark -> Color(0xFF1E1B4B)
                                else -> Color(0xFFEEF2FF)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    isAllSelected -> Indigo600
                                    isDark -> Color(0xFF3730A3)
                                    else -> Color(0xFFC7D2FE)
                                }
                            ),
                            shadowElevation = if (isAllSelected) 4.dp else 0.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { tempQty = maxQ }
                                .testTag("preset_qty_all")
                        ) {
                            Text(
                                text = "All ($maxQ)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = when {
                                    isAllSelected -> Color.White
                                    isDark -> Color(0xFFA5B4FC)
                                    else -> Indigo600
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showQtyDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Red600,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.setNumQuestions(tempQty)
                                showQtyDialog = false
                                viewModel.startQuiz()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("apply_qty_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Blue600,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Apply",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundBrush(isDark))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Quiz Container Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 850.dp)
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Blue600, Indigo600)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top Row: Title Centered
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isExamMode) "🏆 Raman Quiz • Exam Mode" else "🏆 Raman Quiz Challenge",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }

                        // Bottom Controls Row: Home Icon, Restart Icon, Qty, and Score/Palette Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left side: Home button + Restart button (icon only)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Home button
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF0F172A) else Color.White,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable { showExitConfirmDialog = true }
                                        .testTag("quiz_home_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Home,
                                            contentDescription = "Home",
                                            tint = if (isDark) Color.White else Slate700,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Restart icon button (icon only)
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF0F172A) else Color.White,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable { viewModel.startQuiz() }
                                        .testTag("quiz_restart_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Restart",
                                            tint = if (isDark) Color(0xFF60A5FA) else Blue600,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Middle Question Count Selector Pill
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.22f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable {
                                        tempQty = uiState.numQuestions
                                        showQtyDialog = true
                                    }
                                    .testTag("quiz_qty_selector_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Select Number of Questions",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Q: ${uiState.numQuestions}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Right Pill: Question Palette in Exam Mode OR Score in Practice Mode
                            if (isExamMode) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isDark) Color(0xFF0F172A) else Color.White,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { showPaletteDialog = true }
                                        .testTag("exam_palette_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = "Question Palette",
                                            tint = if (isDark) Color(0xFF60A5FA) else Blue600,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "${uiState.examAnswers.size}/$totalCount",
                                            color = if (isDark) Color(0xFF60A5FA) else Blue600,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isDark) Color(0xFF0F172A) else Color.White,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .scale(scoreScale)
                                        .testTag("quiz_score_box")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "Score: ",
                                            color = if (isDark) Color(0xFFCBD5E1) else Slate600,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${uiState.score}",
                                            color = if (isDark) Color(0xFF60A5FA) else Blue600,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Progress Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Q. $currentNumber / $totalCount",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$remainingCount Left",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val progress = if (totalCount > 0) (currentNumber - 1).toFloat() / totalCount else 0f
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        label = "progress"
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = Blue600,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                }

                // Timer Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isExamMode) {
                        // In Exam Mode: no timer pause allowed
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Exam Timer",
                                tint = Indigo600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.toggleTimer() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isTimerPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (uiState.isTimerPaused) "Resume Timer" else "Pause Timer",
                                tint = if (uiState.isTimerPaused) Amber500 else Blue600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    val maxTime = if (isExamMode) {
                        uiState.examTotalTimeSeconds.coerceAtLeast(1).toFloat()
                    } else {
                        uiState.questionTimeSeconds.coerceAtLeast(1).toFloat()
                    }
                    val timerFraction = (uiState.timeLeft / maxTime).coerceIn(0f, 1f)
                    val animatedTimer by animateFloatAsState(
                        targetValue = timerFraction,
                        animationSpec = tween(durationMillis = 950),
                        label = "timerBar"
                    )

                    val timerColor by animateColorAsState(
                        targetValue = when {
                            uiState.timeLeft <= 5 -> Red600
                            uiState.timeLeft <= 10 -> Amber500
                            else -> Green600
                        },
                        label = "timerColor"
                    )

                    LinearProgressIndicator(
                        progress = { animatedTimer },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = timerColor,
                        trackColor = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = if (isExamMode) formatExamTime(uiState.timeLeft) else "${uiState.timeLeft}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = timerColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = if (isExamMode) 48.dp else 32.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Question & Options Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Question Header with TTS speak button centered vertically
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MathText(
                                text = currentQ?.q ?: "",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, lineHeight = 28.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            )

                            IconButton(
                                onClick = { viewModel.speakCurrentQuestion() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Blue600, Indigo600)
                                        )
                                    )
                            ) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "Speak",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 4 Option items
                        val labels = listOf("1", "2", "3", "4")
                        currentQ?.o?.forEachIndexed { index, optionText ->
                            val isCorrectAnswer = index == currentQ.a
                            val isSelectedByPlayer = if (isExamMode) {
                                uiState.examAnswers[uiState.currentIndex] == index
                            } else {
                                uiState.selectedOptionIndex == index
                            }

                            val isTargetCorrect = !isExamMode && uiState.isLocked && isCorrectAnswer
                            val isTargetWrong = !isExamMode && uiState.isLocked && isSelectedByPlayer && !isCorrectAnswer
                            val isExamSelected = isExamMode && isSelectedByPlayer

                            val optionBg = when {
                                isExamSelected && isDark -> Color(0xFF1E3A8A)
                                isExamSelected -> Color(0xFFDBEAFE)
                                isTargetCorrect && isDark -> Color(0xFF064E3B)
                                isTargetCorrect -> OptionCorrectBg
                                isTargetWrong && isDark -> Color(0xFF7F1D1D)
                                isTargetWrong -> OptionWrongBg
                                isDark -> Color(0xFF0F172A)
                                else -> Color(0xFFFEF9C3) // Soft gold background
                            }

                            val optionBorder = when {
                                isExamSelected -> Blue600
                                isTargetCorrect -> OptionCorrectBorder
                                isTargetWrong -> OptionWrongBorder
                                isDark -> Color(0xFFF59E0B).copy(alpha = 0.55f)
                                else -> Color(0xFFFCD34D) // Soft gold border
                            }

                            val optionTextColor = when {
                                isExamSelected && isDark -> Color(0xFFDBEAFE)
                                isExamSelected -> Color(0xFF1E3A8A)
                                isTargetCorrect && isDark -> Color(0xFFD1FAE5)
                                isTargetCorrect -> Color(0xFF065F46)
                                isTargetWrong && isDark -> Color(0xFFFEE2E2)
                                isTargetWrong -> Color(0xFF991B1B)
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(optionBg)
                                    .border(
                                        width = if (isExamSelected || isTargetCorrect || isTargetWrong) 2.dp else 1.5.dp,
                                        color = optionBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable(enabled = isExamMode || !uiState.isLocked) {
                                        viewModel.checkAnswer(index)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 14.dp)
                                    .testTag("quiz_option_$index")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Number badge
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isExamSelected -> Blue600
                                                    isDark -> Color(0xFF1E293B)
                                                    else -> Color.White
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = labels.getOrElse(index) { "${index + 1}" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = when {
                                                isExamSelected -> Color.White
                                                isTargetCorrect && isDark -> Color(0xFF4ADE80)
                                                isTargetCorrect -> Green600
                                                isTargetWrong && isDark -> Color(0xFFF87171)
                                                isTargetWrong -> Red600
                                                isDark -> Color(0xFFFBBF24)
                                                else -> Color(0xFFB45309) // Deep amber/gold
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    MathText(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isExamSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = optionTextColor,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isExamSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = if (isDark) Color(0xFF60A5FA) else Blue600,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else if (isTargetCorrect) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Correct",
                                            tint = if (isDark) Color(0xFF4ADE80) else Green600,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else if (isTargetWrong) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Wrong",
                                            tint = if (isDark) Color(0xFFF87171) else Red600,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Feedback Banner (Practice Mode only)
                    if (!isExamMode) {
                        AnimatedVisibility(
                            visible = uiState.feedbackMessage.isNotEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut()
                        ) {
                            val isSuccess = uiState.isFeedbackSuccess == true
                            val bannerBg = when {
                                isSuccess && isDark -> Color(0xFF064E3B)
                                isSuccess -> Color(0xFFDCFCE7)
                                isDark -> Color(0xFF7F1D1D)
                                else -> Color(0xFFFEE2E2)
                            }
                            val bannerText = when {
                                isSuccess && isDark -> Color(0xFFD1FAE5)
                                isSuccess -> Color(0xFF166534)
                                isDark -> Color(0xFFFEE2E2)
                                else -> Color(0xFF991B1B)
                            }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = bannerBg
                            ) {
                                Text(
                                    text = uiState.feedbackMessage,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = bannerText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Bar for Exam Mode (Previous / Star / Next / Clear / Submit Exam)
                if (isExamMode) {
                    ExamBottomBar(
                        uiState = uiState,
                        viewModel = viewModel,
                        isDark = isDark,
                        onRequestSubmit = { showSubmitConfirmDialog = true }
                    )
                } else {
                    val isStarred = remember(currentQ, uiState.starredQuestions, uiState.bookmarkedIndices) {
                        uiState.bookmarkedIndices.contains(uiState.currentIndex) ||
                            viewModel.isQuestionStarred(currentQ)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isStarred && isDark -> Color(0xFF451A03)
                                isStarred && !isDark -> Color(0xFFFEF3C7)
                                isDark -> Color(0xFF1E293B)
                                else -> Color(0xFFF1F5F9)
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.5.dp,
                                    color = if (isStarred) Amber500 else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("quiz_star_question_button")
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleBookmark() },
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(
                                    imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (isStarred) "Unstar Question" else "Star Question",
                                    tint = if (isStarred) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
