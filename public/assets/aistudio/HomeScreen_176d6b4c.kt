package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.QuizMode
import com.example.model.SavedQuiz
import com.example.ui.theme.Amber500
import com.example.ui.theme.Blue600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Green600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Red600
import com.example.ui.theme.appBackgroundBrush
import com.example.viewmodel.QuizUiState
import com.example.viewmodel.QuizViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: QuizUiState,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = uiState.isDarkMode ?: isSystemInDarkTheme()
    var showTimerDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showCopyPromptDialog by remember { mutableStateOf(false) }
    var tempTimeSeconds by remember { mutableStateOf(uiState.questionTimeSeconds) }
    var customTimeInput by remember { mutableStateOf(uiState.questionTimeSeconds.toString()) }

    if (showAiDialog) {
        AiGeneratorDialog(
            uiState = uiState,
            viewModel = viewModel,
            isDark = isDark,
            onDismiss = {
                viewModel.clearAiError()
                showAiDialog = false
            }
        )
    }

    if (showCopyPromptDialog) {
        CopyPromptDialog(
            viewModel = viewModel,
            isDark = isDark,
            onDismiss = { showCopyPromptDialog = false }
        )
    }

    // File picker launcher for loading .txt or .json files into editor (or auto-importing if multi-quiz bundle)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().use { r -> r.readText() }
                    if (content.contains("=== QUIZ:", ignoreCase = true)) {
                        viewModel.importQuizzesFromText(content)
                    } else {
                        viewModel.onInputTextChanged(content)
                    }
                }
            } catch (e: Exception) {
                viewModel.showSnackbar("Error reading file: ${e.localizedMessage}")
            }
        }
    }

    if (showTimerDialog) {
        val timePresets = listOf(15, 30, 45, 60, 90, 120)
        val sliderMax = 180

        Dialog(onDismissRequest = { showTimerDialog = false }) {
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
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Question Timing",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Set time per question (Default: 30s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val canDecrease = tempTimeSeconds > 5
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
                                        val next = (tempTimeSeconds - 5).coerceAtLeast(5)
                                        tempTimeSeconds = next
                                        customTimeInput = next.toString()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease Time",
                                        tint = if (canDecrease) Blue600 else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${tempTimeSeconds}s",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF60A5FA) else Blue600
                                )
                                Text(
                                    text = "Seconds per Question",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val canIncrease = tempTimeSeconds < 600
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
                                        val next = (tempTimeSeconds + 5).coerceAtMost(600)
                                        tempTimeSeconds = next
                                        customTimeInput = next.toString()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase Time",
                                        tint = if (canIncrease) Blue600 else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Modern Gradient Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = tempTimeSeconds.coerceIn(5, sliderMax).toFloat(),
                            onValueChange = {
                                val v = it.roundToInt().coerceIn(5, sliderMax)
                                tempTimeSeconds = v
                                customTimeInput = v.toString()
                            },
                            valueRange = 5f..sliderMax.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("question_time_slider"),
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
                                val fraction = ((sliderState.value - sliderState.valueRange.start) /
                                    (sliderState.valueRange.endInclusive - sliderState.valueRange.start)).coerceIn(0f, 1f)
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
                                text = "5s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "180s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Preset Chips
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
                        timePresets.forEach { preset ->
                            val isSelected = tempTimeSeconds == preset
                            val chipBg = when {
                                isSelected -> Blue600
                                isDark -> Color(0xFF0F172A)
                                else -> Color(0xFFF1F5F9)
                            }
                            val chipBorder = when {
                                isSelected -> Blue600
                                isDark -> Color(0xFF334155)
                                else -> Color(0xFFCBD5E1)
                            }
                            val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = chipBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                                shadowElevation = if (isSelected) 4.dp else 0.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        tempTimeSeconds = preset
                                        customTimeInput = preset.toString()
                                    }
                                    .testTag("preset_time_$preset")
                            ) {
                                Text(
                                    text = if (preset == 30) "30s (Default)" else "${preset}s",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = chipText,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Custom Timing Input Box
                    OutlinedTextField(
                        value = customTimeInput,
                        onValueChange = { raw ->
                            val digitsOnly = raw.filter { it.isDigit() }.take(3)
                            customTimeInput = digitsOnly
                            val parsed = digitsOnly.toIntOrNull()
                            if (parsed != null && parsed > 0) {
                                tempTimeSeconds = parsed.coerceIn(5, 600)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_time_input"),
                        label = { Text("Custom Timing (Seconds)") },
                        placeholder = { Text("e.g. 45, 75, 150") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue600,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Cancel & Apply Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showTimerDialog = false },
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
                                val finalSeconds = customTimeInput.toIntOrNull()?.coerceIn(5, 600) ?: tempTimeSeconds.coerceIn(5, 600)
                                viewModel.setQuestionTimeSeconds(finalSeconds)
                                showTimerDialog = false
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("apply_time_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Blue600,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundBrush(isDark))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Banner - Only show "Raman Quiz"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E40AF), Indigo600, Color(0xFF6D28D9))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Raman Quiz",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            // Textarea for Quiz Input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Folder icon, Dark Mode toggle icon, Question Timing icon, and Question counter
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { filePickerLauncher.launch(arrayOf("text/*", "application/json")) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("open_file_button")
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = "Open File",
                                    tint = Indigo600,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            // 1st Icon after Folder: Toggle Dark Mode On/Off
                            IconButton(
                                onClick = { viewModel.toggleDarkMode(isDark) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("toggle_dark_mode_button")
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                                    tint = if (isDark) Amber500 else Indigo600,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            // 2nd Icon after Folder (before No. of Questions): Question Timing Setting
                            IconButton(
                                onClick = {
                                    tempTimeSeconds = uiState.questionTimeSeconds
                                    customTimeInput = uiState.questionTimeSeconds.toString()
                                    showTimerDialog = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("question_timing_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Question Timing (${uiState.questionTimeSeconds}s)",
                                    tint = Blue600,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            val hasQuestions = uiState.totalParsedQuestions.isNotEmpty()
                            val badgeBg = when {
                                hasQuestions && isDark -> Color(0xFF064E3B)
                                hasQuestions && !isDark -> Color(0xFFDCFCE7)
                                isDark -> Color(0xFF0F172A)
                                else -> Color(0xFFF1F5F9)
                            }
                            val badgeText = when {
                                hasQuestions && isDark -> Color(0xFF4ADE80)
                                hasQuestions && !isDark -> Green600
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = "${uiState.totalParsedQuestions.size} Questions",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeText
                                )
                            }
                        }

                        // Right: Paste Data button
                        Button(
                            onClick = { viewModel.pasteFromClipboard() },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo600,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            modifier = Modifier.testTag("paste_clipboard_button")
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Paste", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.inputQuizText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp)
                            .testTag("quiz_input_field"),
                        placeholder = {
                            Text("{ q: \"Question\", o: [\"A\", \"B\", \"C\", \"D\"], a: 0 },")
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue600,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quiz Name & Category Row (Auto-filled on AI Generation)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.inputQuizName,
                            onValueChange = { viewModel.onInputNameChanged(it) },
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("quiz_name_field"),
                            placeholder = { Text("Quiz Name") },
                            label = { Text("Quiz Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = uiState.inputQuizCategory,
                            onValueChange = { viewModel.onInputCategoryChanged(it) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quiz_category_field"),
                            placeholder = { Text("Category") },
                            label = { Text("Category") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Indigo600,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        item {
            // Mode Selector & Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Practice Mode vs Exam Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isPractice = uiState.quizMode == QuizMode.PRACTICE
                    val isExam = uiState.quizMode == QuizMode.EXAM

                    // Practice Mode Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isPractice && isDark -> Color(0xFF1E3A8A)
                            isPractice && !isDark -> Color(0xFFDBEAFE)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isPractice) 2.dp else 1.dp,
                            color = if (isPractice) Blue600 else MaterialTheme.colorScheme.outline
                        ),
                        shadowElevation = if (isPractice) 4.dp else 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.setQuizMode(QuizMode.PRACTICE) }
                            .testTag("mode_practice_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isPractice) Blue600 else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = if (isPractice) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Practice Mode",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isPractice && !isDark) Color(0xFF1E3A8A) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Instant Answer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Exam Mode Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isExam && isDark -> Color(0xFF312E81)
                            isExam && !isDark -> Color(0xFFE0E7FF)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isExam) 2.dp else 1.dp,
                            color = if (isExam) Indigo600 else MaterialTheme.colorScheme.outline
                        ),
                        shadowElevation = if (isExam) 4.dp else 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.setQuizMode(QuizMode.EXAM) }
                            .testTag("mode_exam_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isExam) Indigo600 else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = if (isExam) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Exam Mode",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isExam && !isDark) Color(0xFF312E81) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Real CBT Exam",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Main Start Quiz Button
                Button(
                    onClick = { viewModel.startQuizFromHome() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_quiz_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.quizMode == QuizMode.EXAM) Indigo600 else Blue600,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.quizMode == QuizMode.EXAM) "Start Exam Mode" else "Start Practice Quiz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // AI Quiz Generator Button (Full Width)
                Button(
                    onClick = { showAiDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_ai_generator_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6D28D9),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create Quiz with AI",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Copy Prompt & Save Quiz Row (Matched Unified Royal Indigo Background)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showCopyPromptDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("copy_prompt_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isPromptCopied) Green600 else Indigo600,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isPromptCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isPromptCopied) "Copied" else "Copy Prompt",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.saveCurrentQuiz() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_quiz_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isQuizSaved) Green600 else Indigo600,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isQuizSaved) Icons.Default.Check else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isQuizSaved) "Saved" else "Save Quiz",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quiz Database Button (Opens dedicated Category-Wise Quiz Database Page)
                Button(
                    onClick = { viewModel.openQuizDatabase() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_quiz_database_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quiz Database",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            // Footer Info
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "🏆 Raman Quiz Challenge • Made by Sudarshan Kumar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
