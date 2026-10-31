package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.PdfQuestionPaperGenerator
import com.example.model.SavedQuiz
import com.example.ui.theme.Amber500
import com.example.ui.theme.Blue600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Green600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Red600
import com.example.ui.theme.Rose500
import com.example.ui.theme.appBackgroundBrush
import com.example.ui.util.MathText
import com.example.viewmodel.QuizUiState
import com.example.viewmodel.QuizViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PdfExportTarget(
    val quizzes: List<SavedQuiz>,
    val title: String,
    val category: String
)

@Composable
fun QuizDatabaseScreen(
    uiState: QuizUiState,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.goHome()
    }

    val context = LocalContext.current
    val isDark = uiState.isDarkMode ?: isSystemInDarkTheme()

    var quizPendingDelete by remember { mutableStateOf<SavedQuiz?>(null) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    var showStarredBankDialog by remember { mutableStateOf(false) }
    var pdfExportTarget by remember { mutableStateOf<PdfExportTarget?>(null) }
    var includeAnswerKeyInPdf by remember { mutableStateOf(true) }

    if (showStarredBankDialog) {
        Dialog(onDismissRequest = { showStarredBankDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .heightIn(max = 620.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Amber500.copy(alpha = 0.18f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Amber500,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Starred Questions Bank",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.starredQuestions.size} Important Questions",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (uiState.starredQuestions.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearAllStarredQuestions() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear All Starred",
                                    tint = Red600
                                )
                            }
                        }
                    }

                    if (uiState.starredQuestions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No starred questions yet. Tap the ⭐ Star icon during any quiz or on the Result page to save important questions here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(uiState.starredQuestions) { idx, q ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            Amber500.copy(alpha = 0.45f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            MathText(
                                                text = "Q${idx + 1}. ${q.q}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 8.dp)
                                            )
                                            IconButton(
                                                onClick = { viewModel.removeStarredQuestion(q.q) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove Starred Question",
                                                    tint = Red600,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val correctAns = q.o.getOrElse(q.a) { "" }
                                        MathText(
                                            text = "✔ Ans: $correctAns",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF4ADE80) else Green600
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    showStarredBankDialog = false
                                    viewModel.startStarredQuestionsQuiz()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Blue600,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Start Quiz", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val starredSaved = viewModel.getStarredSavedQuiz()
                                    showStarredBankDialog = false
                                    pdfExportTarget = PdfExportTarget(
                                        quizzes = listOf(starredSaved),
                                        title = "Starred Questions Bank",
                                        category = "Starred"
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Indigo600,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Export PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = { showStarredBankDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red600,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Deleting a Single Quiz List
    quizPendingDelete?.let { targetQuiz ->
        Dialog(onDismissRequest = { quizPendingDelete = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Red600.copy(alpha = 0.14f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Red600,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Text(
                        text = "Delete Quiz?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Are you sure you want to delete \"${targetQuiz.name}\" (${targetQuiz.questionCount} questions)?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { quizPendingDelete = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("cancel_delete_quiz_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green600,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.deleteSavedQuiz(targetQuiz.id)
                                quizPendingDelete = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("confirm_delete_quiz_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Red600,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Clearing All Quiz Data
    if (showClearAllConfirmDialog) {
        Dialog(onDismissRequest = { showClearAllConfirmDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Red600.copy(alpha = 0.14f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Red600,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Text(
                        text = "Clear All Quiz Data?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Are you sure you want to delete all ${uiState.savedQuizzes.size} saved quizzes and ${uiState.starredQuestions.size} starred questions from Quiz Database? This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showClearAllConfirmDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("cancel_clear_all_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green600,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.clearAllSavedQuizzes()
                                showClearAllConfirmDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("confirm_clear_all_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Red600,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Delete All", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 1-Click Import .txt File Launcher
    val importQuizFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().use { r -> r.readText() }
                    viewModel.importQuizzesFromText(content, "Imported Quiz")
                }
            } catch (_: Exception) {
            }
        }
    }

    // 1-Click Export .txt File Launcher
    val saveAllToFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            try {
                val exportContent = viewModel.getAllQuizzesExportText()
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(exportContent.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {
            }
        }
    }

    // 1-Click Save PDF Question Paper Launcher
    val savePdfToFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val target = pdfExportTarget
        if (uri != null && target != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    viewModel.writeQuizzesPdfToStream(
                        outputStream = out,
                        quizzes = target.quizzes,
                        paperTitle = target.title,
                        subtitleCategory = target.category,
                        includeAnswerKey = includeAnswerKeyInPdf
                    )
                }
            } catch (_: Exception) {
            }
            pdfExportTarget = null
        }
    }

    // Printable Question Paper / PDF Export Dialog
    pdfExportTarget?.let { target ->
        val totalQs = target.quizzes.sumOf { it.questionCount }
        Dialog(onDismissRequest = { pdfExportTarget = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Rose500.copy(alpha = 0.14f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Rose500,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Text(
                        text = "PDF Question Paper",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "${target.title}\n(${target.category} • $totalQs Questions)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Answer Key Toggle Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Include Answer Key",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = includeAnswerKeyInPdf,
                                onCheckedChange = { includeAnswerKeyInPdf = it },
                                modifier = Modifier.testTag("toggle_pdf_answer_key")
                            )
                        }
                    }

                    // Primary Button: Share PDF
                    Button(
                        onClick = {
                            viewModel.shareQuizzesAsPdf(
                                quizzes = target.quizzes,
                                paperTitle = target.title,
                                subtitleCategory = target.category,
                                includeAnswerKey = includeAnswerKeyInPdf
                            )
                            pdfExportTarget = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("share_pdf_paper_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Rose500,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share PDF", fontWeight = FontWeight.Bold)
                    }

                    // Secondary Row: Save PDF + Print Paper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val safeName = target.title
                                    .replace(Regex("""[^a-zA-Z0-9_\-\u0900-\u097F ]"""), "")
                                    .trim()
                                    .replace(Regex("""\s+"""), "_")
                                    .ifBlank { "raman_question_paper" }
                                savePdfToFileLauncher.launch("${safeName}.pdf")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("save_pdf_file_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Blue600,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save PDF", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val pdfFile = viewModel.createPdfFileForPrinting(
                                    quizzes = target.quizzes,
                                    paperTitle = target.title,
                                    subtitleCategory = target.category,
                                    includeAnswerKey = includeAnswerKeyInPdf
                                )
                                if (pdfFile != null) {
                                    PdfQuestionPaperGenerator.printPdfFile(
                                        context = context,
                                        pdfFile = pdfFile,
                                        jobName = target.title
                                    )
                                }
                                pdfExportTarget = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("print_pdf_paper_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo600,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { pdfExportTarget = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF475569),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    val categoryCounts = remember(uiState.savedQuizzes) {
        val counts = linkedMapOf<String, Int>()
        uiState.savedQuizzes.forEach { q ->
            val cat = q.category.lineSequence().firstOrNull()?.trim()?.take(25)?.ifBlank { "General" } ?: "General"
            counts[cat] = (counts[cat] ?: 0) + 1
        }
        counts
    }

    val filteredQuizzes = remember(uiState.savedQuizzes, uiState.selectedCategoryFilter, uiState.savedQuizSearchQuery) {
        uiState.savedQuizzes.filter { saved ->
            val cleanCat = saved.category.lineSequence().firstOrNull()?.trim()?.take(25)?.ifBlank { "General" } ?: "General"
            val matchesCategory = uiState.selectedCategoryFilter == "All" ||
                cleanCat.equals(uiState.selectedCategoryFilter, ignoreCase = true)
            val query = uiState.savedQuizSearchQuery.trim()
            val matchesQuery = query.isEmpty() ||
                saved.name.contains(query, ignoreCase = true) ||
                cleanCat.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val groupedByCategory = remember(filteredQuizzes) {
        filteredQuizzes.groupBy { saved ->
            saved.category.lineSequence().firstOrNull()?.trim()?.take(25)?.ifBlank { "General" } ?: "General"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundBrush(isDark))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Banner with Back Button + Title + Delete All Icon + Import/Export Buttons
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Row: Back Button, Title & Delete All Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Blue600,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.goHome() }
                                    .testTag("database_back_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Home",
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Quiz Database",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.savedQuizzes.size} Quizzes • ${categoryCounts.size} Categories",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Delete All Quizzes & Starred Questions Icon Button (with Confirmation)
                        if (uiState.savedQuizzes.isNotEmpty() || uiState.starredQuestions.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = Red600.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { showClearAllConfirmDialog = true }
                                    .testTag("delete_all_quizzes_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear All Quiz Data",
                                        tint = Red600,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Import / Export / Share Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                importQuizFileLauncher.launch(arrayOf("text/plain", "text/*", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("import_quizzes_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald600,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Import .txt",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Import",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (uiState.savedQuizzes.isNotEmpty() || uiState.starredQuestions.isNotEmpty()) {
                                    saveAllToFileLauncher.launch("raman_quizzes.txt")
                                }
                            },
                            enabled = uiState.savedQuizzes.isNotEmpty() || uiState.starredQuestions.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("export_txt_quizzes_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Blue600,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Export .txt",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Export",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.shareAllQuizzes(onlySelectedCategory = false)
                            },
                            enabled = uiState.savedQuizzes.isNotEmpty() || uiState.starredQuestions.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("share_all_quizzes_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo600,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share .txt",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Share",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = uiState.savedQuizSearchQuery,
                        onValueChange = { viewModel.setSavedQuizSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("saved_quiz_search_field"),
                        placeholder = { Text("Search quiz list and cat.") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.savedQuizSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSavedQuizSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue600,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Filter Chips Row
                    if (uiState.savedQuizzes.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isAllSelected = uiState.selectedCategoryFilter == "All"
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isAllSelected) Blue600 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { viewModel.setSelectedCategoryFilter("All") }
                                    .testTag("category_filter_All")
                            ) {
                                Text(
                                    text = "All (${uiState.savedQuizzes.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            categoryCounts.forEach { (category, count) ->
                                val isSelected = uiState.selectedCategoryFilter.equals(category, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Blue600 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { viewModel.setSelectedCategoryFilter(category) }
                                        .testTag("category_filter_$category")
                                ) {
                                    Text(
                                        text = "$category ($count)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dedicated Starred / Bookmarked Questions Bank Card
        item(key = "starred_questions_bank_card") {
            val starredCount = uiState.starredQuestions.size
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .border(
                        width = 1.5.dp,
                        color = Amber500.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { showStarredBankDialog = true }
                    .testTag("starred_questions_bank_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFBEB)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Amber500.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Amber500,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Starred Questions Bank",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (starredCount > 0) {
                                    "$starredCount प्रश्न सेव हैं • Tap to View, Play or Export PDF"
                                } else {
                                    "क्विज़ के दौरान ⭐ दबाकर महत्वपूर्ण प्रश्न यहाँ सेव करें"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (starredCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.startStarredQuestionsQuiz() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("start_starred_quiz_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Starred Quiz",
                                    tint = Blue600
                                )
                            }
                            IconButton(
                                onClick = { showStarredBankDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("view_starred_questions_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "View Starred Questions",
                                    tint = Amber500
                                )
                            }
                            IconButton(
                                onClick = {
                                    val starredSaved = viewModel.getStarredSavedQuiz()
                                    pdfExportTarget = PdfExportTarget(
                                        quizzes = listOf(starredSaved),
                                        title = "Starred Questions Bank",
                                        category = "Starred"
                                    )
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("export_pdf_starred_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export Starred PDF",
                                    tint = Indigo600
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category-Wise Quiz List Content
        if (uiState.savedQuizzes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 700.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Saved Quizzes Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Save a quiz from the Home page or tap Import above to load a .txt quiz file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (filteredQuizzes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 700.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No quizzes match your search or category filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            groupedByCategory.forEach { (categoryName, quizzesInCat) ->
                item(key = "header_$categoryName") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFE0E7FF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 700.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF818CF8) else Indigo600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF1E1B4B)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Indigo600
                            ) {
                                Text(
                                    text = "${quizzesInCat.size} Quiz${if (quizzesInCat.size > 1) "zes" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                items(quizzesInCat, key = { it.id }) { saved ->
                    SavedQuizItem(
                        saved = saved,
                        onSelect = { viewModel.selectSavedQuiz(saved, startImmediately = false) },
                        onStart = { viewModel.selectSavedQuiz(saved, startImmediately = true) },
                        onSharePdf = {
                            val cleanCat = saved.category.lineSequence().firstOrNull()?.trim()?.ifBlank { "General" } ?: "General"
                            pdfExportTarget = PdfExportTarget(
                                quizzes = listOf(saved),
                                title = saved.name.ifBlank { "Question Paper" },
                                category = cleanCat
                            )
                        },
                        onDelete = { quizPendingDelete = saved },
                        modifier = Modifier.widthIn(max = 700.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SavedQuizItem(
    saved: SavedQuiz,
    onSelect: () -> Unit,
    onStart: () -> Unit,
    onSharePdf: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(saved.timestamp))
    val categoryLabel = saved.category.lineSequence().firstOrNull()?.trim()?.take(25)?.ifBlank { "General" } ?: "General"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = saved.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Indigo600.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Indigo600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateStr • ${saved.questionCount} प्रश्न",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onStart,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Quiz",
                        tint = Blue600
                    )
                }
                IconButton(
                    onClick = onSharePdf,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("export_pdf_item_${saved.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share PDF Question Paper",
                        tint = Indigo600
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Saved Quiz",
                        tint = Red600
                    )
                }
            }
        }
    }
}
