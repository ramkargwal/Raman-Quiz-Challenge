package com.example.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.model.MarkingScheme
import com.example.model.QuizQuestion
import com.example.model.SavedQuiz
import com.example.ui.util.LatexNode
import com.example.ui.util.MathMarkdownFormatter
import com.example.ui.util.OverlineKind
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuestionPaperSection(
    val title: String,
    val category: String,
    val questions: List<QuizQuestion>
)

object PdfQuestionPaperGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points (72 dpi)
    private const val MARGIN_H = 36f
    private const val MARGIN_TOP = 34f
    private const val MARGIN_BOTTOM = 42f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2)

    private val OPTION_LABELS = listOf("A", "B", "C", "D", "E", "F")

    fun buildSectionsFromSavedQuizzes(quizzes: List<SavedQuiz>): List<QuestionPaperSection> {
        return quizzes.mapNotNull { saved ->
            val parsed = QuizParser.parse(saved.data).questions
            if (parsed.isEmpty()) null
            else QuestionPaperSection(
                title = saved.name.ifBlank { "Quiz Paper" },
                category = saved.category.lineSequence().firstOrNull()?.trim()?.ifBlank { "General" } ?: "General",
                questions = parsed
            )
        }
    }

    fun writePdfToStream(
        outputStream: OutputStream,
        paperTitle: String,
        subtitleCategory: String,
        sections: List<QuestionPaperSection>,
        timePerQuestionSeconds: Int = 30,
        includeAnswerKey: Boolean = true,
        markingScheme: MarkingScheme = MarkingScheme()
    ) {
        val document = PdfDocument()
        try {
            val totalQuestions = sections.sumOf { it.questions.size }
            val totalMinutes = ((totalQuestions * timePerQuestionSeconds) / 60).coerceAtLeast(1)
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val maxMarksStr = markingScheme.formatScore(markingScheme.calculateMaxMarks(totalQuestions))
            val markingSchemeStr = markingScheme.shortLabel

            // Paints
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.rgb(30, 41, 59) // Slate 800
                strokeWidth = 1.2f
            }
            val thinLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.rgb(203, 213, 225) // Slate 300
                strokeWidth = 0.8f
            }
            val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.rgb(241, 245, 249) // Slate 100
            }
            val omrColHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.rgb(224, 231, 255) // Indigo 100
            }
            val omrStripeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.rgb(248, 250, 252) // Slate 50
            }
            val sectionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.rgb(224, 231, 255) // Indigo 100
            }
            val answerKeyBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.rgb(236, 253, 245) // Emerald 50
            }
            val answerKeyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.rgb(16, 185, 129) // Emerald 500
                strokeWidth = 1f
            }
            val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.rgb(71, 85, 105)
                strokeWidth = 1.1f
            }
            val omrCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.rgb(30, 41, 59)
                strokeWidth = 1.1f
            }
            val filledCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.rgb(30, 41, 59)
            }

            val brandTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(67, 56, 202) // Indigo 700
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val titleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 15.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val metaBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9.2f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val sectionTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 27, 75)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val questionTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val optionTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val optionLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(37, 99, 235)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val omrBubbleLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val omrQNumPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val footerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val answerKeyTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(6, 78, 59)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            var pageNumber = 1
            var currentPage = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            var canvas = currentPage.canvas
            var cursorY = MARGIN_TOP

            fun drawPageFooter(c: Canvas, pageNum: Int, sectionName: String) {
                val footerY = PAGE_HEIGHT - 18f
                c.drawLine(MARGIN_H, footerY - 11f, PAGE_WIDTH - MARGIN_H, footerY - 11f, thinLinePaint)
                c.drawText(
                    "Raman Quiz Challenge • $sectionName",
                    MARGIN_H,
                    footerY,
                    footerTextPaint
                )
                val pageLabel = "Page $pageNum"
                val labelWidth = footerTextPaint.measureText(pageLabel)
                c.drawText(pageLabel, PAGE_WIDTH - MARGIN_H - labelWidth, footerY, footerTextPaint)
            }

            var currentFooterLabel = "OMR Answer Sheet"

            fun startNewPage(newFooterLabel: String = currentFooterLabel) {
                drawPageFooter(canvas, pageNumber, currentFooterLabel)
                document.finishPage(currentPage)
                pageNumber++
                currentFooterLabel = newFooterLabel
                currentPage = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = currentPage.canvas
                cursorY = MARGIN_TOP
            }

            fun ensureSpace(requiredHeight: Float) {
                if (cursorY + requiredHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    startNewPage(currentFooterLabel)
                }
            }

            fun drawTopPaperBanner(bannerSubtitle: String, showCandidateLines: Boolean) {
                val titleLayout = buildStaticLayout(paperTitle, titleTextPaint, (CONTENT_WIDTH - 24f).toInt())
                val extraCandidateHeight = if (showCandidateLines) 42f else 0f
                val headerBoxHeight = 66f + titleLayout.height + extraCandidateHeight
                val headerRect = RectF(MARGIN_H, cursorY, PAGE_WIDTH - MARGIN_H, cursorY + headerBoxHeight)
                canvas.drawRoundRect(headerRect, 8f, 8f, headerBgPaint)
                canvas.drawRoundRect(headerRect, 8f, 8f, borderPaint)

                var innerY = cursorY + 14f
                val brandWidth = brandTextPaint.measureText(bannerSubtitle)
                canvas.drawText(bannerSubtitle, (PAGE_WIDTH - brandWidth) / 2f, innerY, brandTextPaint)
                innerY += 7f

                // Centered Paper Title
                canvas.save()
                canvas.translate(MARGIN_H + 12f, innerY)
                for (i in 0 until titleLayout.lineCount) {
                    val lineStart = titleLayout.getLineStart(i)
                    val lineEnd = titleLayout.getLineEnd(i)
                    val lineText = paperTitle.substring(lineStart, lineEnd).trim()
                    val lineW = titleTextPaint.measureText(lineText)
                    val lineX = ((CONTENT_WIDTH - 24f) - lineW) / 2f
                    canvas.drawText(
                        lineText,
                        lineX.coerceAtLeast(0f),
                        titleLayout.getLineBaseline(i).toFloat(),
                        titleTextPaint
                    )
                }
                canvas.restore()
                innerY += titleLayout.height + 9f

                // Divider inside header
                canvas.drawLine(MARGIN_H + 10f, innerY, PAGE_WIDTH - MARGIN_H - 10f, innerY, thinLinePaint)
                innerY += 13f

                // Metadata row: Category | Date | Total Questions | Time | Marking Scheme | Max Marks
                val metaLeft = "Category: $subtitleCategory  •  Date: $dateStr"
                val metaRight = "Qs: $totalQuestions  •  Time: $totalMinutes Min  •  Marking: $markingSchemeStr  •  Max Marks: $maxMarksStr"
                canvas.drawText(metaLeft, MARGIN_H + 12f, innerY, metaBoldPaint)
                val rightW = metaBoldPaint.measureText(metaRight)
                canvas.drawText(metaRight, PAGE_WIDTH - MARGIN_H - 12f - rightW, innerY, metaBoldPaint)

                if (showCandidateLines) {
                    innerY += 15f
                    canvas.drawLine(MARGIN_H + 10f, innerY - 9f, PAGE_WIDTH - MARGIN_H - 10f, innerY - 9f, thinLinePaint)
                    val candidateLine1 = "Student Name: ___________________________   Roll No: ______________   Sign: ______________"
                    canvas.drawText(candidateLine1, MARGIN_H + 12f, innerY + 3f, metaTextPaint)
                    innerY += 18f
                    val posLabel = "+${MarkingScheme.formatNumber(markingScheme.positiveMarks)}"
                    val negLabel = if (markingScheme.hasNegativeMarking) "-${MarkingScheme.formatNumber(markingScheme.negativeMarks)}" else "0"
                    val candidateLine2 = "Attempted: _____   Correct ($posLabel): _____   Wrong ($negLabel): _____   Net Score: _____ / $maxMarksStr"
                    canvas.drawText(candidateLine2, MARGIN_H + 12f, innerY + 3f, metaBoldPaint)
                }

                cursorY += headerBoxHeight + 12f
            }

            // =========================================================================
            // PART 1 (FIRST PAGE): OMR ANSWER SHEET
            // =========================================================================
            currentFooterLabel = "OMR Answer Sheet"
            drawTopPaperBanner(
                bannerSubtitle = "RAMAN QUIZ CHALLENGE • OMR ANSWER SHEET",
                showCandidateLines = true
            )

            // OMR Instructions Strip
            val instrRect = RectF(MARGIN_H, cursorY, PAGE_WIDTH - MARGIN_H, cursorY + 24f)
            canvas.drawRoundRect(instrRect, 5f, 5f, sectionBgPaint)
            canvas.drawText(
                "Instructions: Darken only ONE circle (A, B, C, or D) completely with a Blue/Black pen.   Correct Method: ",
                MARGIN_H + 10f,
                cursorY + 15f,
                metaBoldPaint
            )
            // Draw sample filled circle for Correct Method
            canvas.drawCircle(PAGE_WIDTH - MARGIN_H - 22f, cursorY + 12f, 5.5f, filledCirclePaint)
            cursorY += 32f

            // Multi-Column OMR Grid (4 columns across A4 page, up to 25 rows per column = 100 questions per page)
            val colsPerPage = 4
            val rowsPerCol = 25
            val questionsPerPage = colsPerPage * rowsPerCol
            val colGap = 10f
            val omrColWidth = (CONTENT_WIDTH - (colGap * (colsPerPage - 1))) / colsPerPage
            val rowHeight = 21f
            val colHeaderHeight = 20f

            val totalOmrPages = ((totalQuestions + questionsPerPage - 1) / questionsPerPage).coerceAtLeast(1)
            for (omrPageIdx in 0 until totalOmrPages) {
                if (omrPageIdx > 0) {
                    startNewPage("OMR Answer Sheet")
                    drawTopPaperBanner(
                        bannerSubtitle = "RAMAN QUIZ CHALLENGE • OMR ANSWER SHEET (CONT.)",
                        showCandidateLines = false
                    )
                }

                val startQForPage = omrPageIdx * questionsPerPage + 1
                val endQForPage = minOf(totalQuestions, (omrPageIdx + 1) * questionsPerPage)
                val gridTopY = cursorY

                for (colIdx in 0 until colsPerPage) {
                    val colStartQ = startQForPage + (colIdx * rowsPerCol)
                    if (colStartQ > endQForPage) break
                    val colEndQ = minOf(endQForPage, colStartQ + rowsPerCol - 1)
                    val colRowCount = (colEndQ - colStartQ + 1).coerceAtLeast(1)

                    val colLeft = MARGIN_H + colIdx * (omrColWidth + colGap)
                    val colRight = colLeft + omrColWidth
                    val colBottom = gridTopY + colHeaderHeight + (colRowCount * rowHeight) + 4f

                    // Column Header Box ("Q.No    A   B   C   D")
                    val colHeaderRect = RectF(colLeft, gridTopY, colRight, gridTopY + colHeaderHeight)
                    canvas.drawRoundRect(colHeaderRect, 4f, 4f, omrColHeaderBgPaint)
                    canvas.drawText("Q.", colLeft + 7f, gridTopY + 13.5f, omrQNumPaint)

                    val bubbleStartX = colLeft + 36f
                    val bubbleSpacing = (omrColWidth - 42f) / 3f
                    for (bIdx in 0 until 4) {
                        val bx = bubbleStartX + bIdx * bubbleSpacing
                        val letter = OPTION_LABELS[bIdx]
                        val lw = omrQNumPaint.measureText(letter)
                        canvas.drawText(letter, bx - lw / 2f, gridTopY + 13.5f, omrQNumPaint)
                    }

                    // Rows for each question in this column
                    for (rIdx in 0 until colRowCount) {
                        val qNum = colStartQ + rIdx
                        val rowTop = gridTopY + colHeaderHeight + (rIdx * rowHeight)
                        val rowCenterY = rowTop + (rowHeight / 2f)

                        if (rIdx % 2 == 1) {
                            canvas.drawRect(
                                colLeft + 1f,
                                rowTop,
                                colRight - 1f,
                                rowTop + rowHeight,
                                omrStripeBgPaint
                            )
                        }

                        // Question Number
                        val qNumText = String.format(Locale.US, "%02d.", qNum)
                        canvas.drawText(qNumText, colLeft + 6f, rowCenterY + 3.2f, omrQNumPaint)

                        // 4 OMR Bubbles (A, B, C, D)
                        for (bIdx in 0 until 4) {
                            val bx = bubbleStartX + bIdx * bubbleSpacing
                            canvas.drawCircle(bx, rowCenterY, 6.2f, omrCirclePaint)
                            val letter = OPTION_LABELS[bIdx]
                            val lw = omrBubbleLabelPaint.measureText(letter)
                            canvas.drawText(letter, bx - lw / 2f, rowCenterY + 2.8f, omrBubbleLabelPaint)
                        }
                    }

                    // Outer Border for Column
                    val colOuterRect = RectF(colLeft, gridTopY, colRight, colBottom)
                    canvas.drawRoundRect(colOuterRect, 4f, 4f, borderPaint)
                }
            }

            // =========================================================================
            // PART 2 (NEW PAGE): QUESTION PAPER (प्रश्न पत्र)
            // =========================================================================
            startNewPage("Question Paper (प्रश्न पत्र)")
            drawTopPaperBanner(
                bannerSubtitle = "RAMAN QUIZ CHALLENGE • QUESTION PAPER (प्रश्न पत्र)",
                showCandidateLines = false
            )

            data class AnswerKeyEntry(
                val globalQuestionNum: Int,
                val sectionTitle: String,
                val optionLetter: String,
                val rawAnswerText: String
            )

            val answerKeyEntries = mutableListOf<AnswerKeyEntry>()
            var globalQNum = 1

            sections.forEachIndexed { sectionIndex, section ->
                if (sections.size > 1) {
                    ensureSpace(34f)
                    val secRect = RectF(MARGIN_H, cursorY, PAGE_WIDTH - MARGIN_H, cursorY + 24f)
                    canvas.drawRoundRect(secRect, 5f, 5f, sectionBgPaint)
                    val secHeader = "Section ${sectionIndex + 1}: ${section.title} (${section.category} • ${section.questions.size} Qs)"
                    canvas.drawText(secHeader, MARGIN_H + 10f, cursorY + 16f, sectionTitlePaint)
                    cursorY += 32f
                }

                section.questions.forEach { q ->
                    val qPrefix = "Q$globalQNum. "
                    val fullQuestionRaw = "$qPrefix${q.q.trim()}"
                    val qLayout = buildMathLayout(fullQuestionRaw, questionTextPaint, CONTENT_WIDTH)

                    val rawOptions = q.o.map { it.trim() }
                    val colWidth = (CONTENT_WIDTH - 20f) / 2f
                    val optionTextMaxWidthTwoCol = colWidth - 28f
                    val optionTextMaxWidthOneCol = CONTENT_WIDTH - 28f

                    // Decide if options can cleanly fit in 2-column grid (each option <= 2 lines in half-width)
                    val twoColLayouts = rawOptions.map { opt ->
                        buildMathLayout(opt, optionTextPaint, optionTextMaxWidthTwoCol)
                    }
                    val useTwoColumns = twoColLayouts.all { it.lineCount <= 2 } && rawOptions.size <= 4

                    val optionsHeight = if (useTwoColumns) {
                        val row0Height = maxOf(
                            twoColLayouts.getOrNull(0)?.height ?: 0f,
                            twoColLayouts.getOrNull(1)?.height ?: 0f,
                            15f
                        ) + 8f
                        val row1Height = if (rawOptions.size > 2) {
                            maxOf(
                                twoColLayouts.getOrNull(2)?.height ?: 0f,
                                twoColLayouts.getOrNull(3)?.height ?: 0f,
                                15f
                            ) + 8f
                        } else 0f
                        row0Height + row1Height
                    } else {
                        val oneColLayouts = rawOptions.map { opt ->
                            buildMathLayout(opt, optionTextPaint, optionTextMaxWidthOneCol)
                        }
                        oneColLayouts.sumOf { (maxOf(it.height, 15f) + 6f).toDouble() }.toFloat()
                    }

                    val totalQuestionBlockHeight = qLayout.height + 6f + optionsHeight + 12f
                    ensureSpace(totalQuestionBlockHeight)

                    // Draw Question Text with full LaTeX AST rendering
                    canvas.save()
                    canvas.translate(MARGIN_H, cursorY)
                    qLayout.draw(canvas)
                    canvas.restore()
                    cursorY += qLayout.height + 6f

                    // Draw Options with Circle + (A)/(B)/(C)/(D) vertically centered with the equation
                    if (useTwoColumns) {
                        val rows = rawOptions.chunked(2)
                        rows.forEachIndexed { rIdx, rowOpts ->
                            var rowMaxH = 15f
                            rowOpts.forEachIndexed { cIdx, _ ->
                                val optIndex = rIdx * 2 + cIdx
                                val optLayout = twoColLayouts[optIndex]
                                if (optLayout.height > rowMaxH) {
                                    rowMaxH = optLayout.height
                                }
                            }
                            rowOpts.forEachIndexed { cIdx, _ ->
                                val optIndex = rIdx * 2 + cIdx
                                val label = OPTION_LABELS.getOrElse(optIndex) { "${optIndex + 1}" }
                                val startX = MARGIN_H + (cIdx * (colWidth + 20f))
                                val optLayout = twoColLayouts[optIndex]

                                val optOffsetY = if (optLayout.height < 15f) (15f - optLayout.height) / 2f else 0f
                                val circleCenterX = startX + 8f
                                val circleCenterY = cursorY + optOffsetY + optLayout.firstLineCenterY.coerceAtLeast(7f)
                                canvas.drawCircle(circleCenterX, circleCenterY, 6.5f, circlePaint)
                                val lw = optionLabelPaint.measureText(label)
                                canvas.drawText(label, circleCenterX - lw / 2f, circleCenterY + 3.2f, optionLabelPaint)

                                canvas.save()
                                canvas.translate(startX + 20f, cursorY + optOffsetY)
                                optLayout.draw(canvas)
                                canvas.restore()
                            }
                            cursorY += rowMaxH + 8f
                        }
                    } else {
                        rawOptions.forEachIndexed { optIndex, optText ->
                            val label = OPTION_LABELS.getOrElse(optIndex) { "${optIndex + 1}" }
                            val optLayout = buildMathLayout(optText, optionTextPaint, optionTextMaxWidthOneCol)
                            val rowH = maxOf(optLayout.height, 15f)
                            ensureSpace(rowH + 8f)

                            val optOffsetY = if (optLayout.height < 15f) (15f - optLayout.height) / 2f else 0f
                            val circleCenterX = MARGIN_H + 8f
                            val circleCenterY = cursorY + optOffsetY + optLayout.firstLineCenterY.coerceAtLeast(7f)
                            canvas.drawCircle(circleCenterX, circleCenterY, 6.5f, circlePaint)
                            val lw = optionLabelPaint.measureText(label)
                            canvas.drawText(label, circleCenterX - lw / 2f, circleCenterY + 3.2f, optionLabelPaint)

                            canvas.save()
                            canvas.translate(MARGIN_H + 20f, cursorY + optOffsetY)
                            optLayout.draw(canvas)
                            canvas.restore()

                            cursorY += rowH + 6f
                        }
                    }

                    // Separator line between questions
                    cursorY += 3f
                    canvas.drawLine(MARGIN_H, cursorY, PAGE_WIDTH - MARGIN_H, cursorY, thinLinePaint)
                    cursorY += 8f

                    // Record Answer Key Entry
                    val safeAnsIdx = q.a.coerceIn(0, (rawOptions.size - 1).coerceAtLeast(0))
                    val ansLetter = OPTION_LABELS.getOrElse(safeAnsIdx) { "A" }
                    val ansText = rawOptions.getOrElse(safeAnsIdx) { "" }.trim()
                    answerKeyEntries.add(
                        AnswerKeyEntry(
                            globalQuestionNum = globalQNum,
                            sectionTitle = section.title,
                            optionLetter = ansLetter,
                            rawAnswerText = ansText
                        )
                    )
                    globalQNum++
                }
            }

            // =========================================================================
            // PART 3 (SEPARATE NEW PAGE): ANSWER KEY (उत्तर माला)
            // =========================================================================
            if (includeAnswerKey && answerKeyEntries.isNotEmpty()) {
                // Always start the Answer Key on a brand new page so it can be printed separately!
                startNewPage("Answer Key (उत्तर माला)")

                // Answer Key Banner Header
                val keyBannerRect = RectF(MARGIN_H, cursorY, PAGE_WIDTH - MARGIN_H, cursorY + 32f)
                canvas.drawRoundRect(keyBannerRect, 6f, 6f, answerKeyBgPaint)
                canvas.drawRoundRect(keyBannerRect, 6f, 6f, answerKeyBorderPaint)
                canvas.drawText(
                    "ANSWER KEY (उत्तर माला) — $paperTitle (${answerKeyEntries.size} Questions)",
                    MARGIN_H + 12f,
                    cursorY + 20f,
                    answerKeyTextPaint
                )
                cursorY += 42f

                // Render Answer Key in 2 clean columns with full LaTeX Math formatting
                val keyColWidth = (CONTENT_WIDTH - 16f) / 2f
                val rows = answerKeyEntries.chunked(2)
                rows.forEach { pair ->
                    val layouts = pair.map { entry ->
                        val text = "Q${entry.globalQuestionNum}. (${entry.optionLetter}) ${entry.rawAnswerText}"
                        buildMathLayout(text, optionTextPaint, keyColWidth)
                    }
                    val rowHeight = (layouts.maxOfOrNull { it.height } ?: 14f) + 8f
                    ensureSpace(rowHeight)

                    layouts.forEachIndexed { idx, layout ->
                        val x = MARGIN_H + idx * (keyColWidth + 16f)
                        canvas.save()
                        canvas.translate(x, cursorY)
                        layout.draw(canvas)
                        canvas.restore()
                    }
                    cursorY += rowHeight
                }
            }

            // Finish final page
            drawPageFooter(canvas, pageNumber, currentFooterLabel)
            document.finishPage(currentPage)

            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    fun generatePdfFileInCache(
        context: Context,
        fileNameBase: String,
        paperTitle: String,
        subtitleCategory: String,
        sections: List<QuestionPaperSection>,
        timePerQuestionSeconds: Int = 30,
        includeAnswerKey: Boolean = true,
        markingScheme: MarkingScheme = MarkingScheme()
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeFileName = fileNameBase
            .replace(Regex("""[^a-zA-Z0-9_\-\u0900-\u097F ]"""), "")
            .trim()
            .replace(Regex("""\s+"""), "_")
            .ifBlank { "raman_question_paper" }
        val pdfFile = File(exportDir, "${safeFileName}_Question_Paper.pdf")
        FileOutputStream(pdfFile).use { out ->
            writePdfToStream(
                outputStream = out,
                paperTitle = paperTitle,
                subtitleCategory = subtitleCategory,
                sections = sections,
                timePerQuestionSeconds = timePerQuestionSeconds,
                includeAnswerKey = includeAnswerKey,
                markingScheme = markingScheme
            )
        }
        return pdfFile
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("No destination file descriptor")
                    return
                }
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        val safeWidth = width.coerceAtLeast(40)
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(false)
            .build()
    }

    // =============================================================================================
    // NATIVE PDF CANVAS LATEX MATH AST LAYOUT & RENDERER
    // Mirrors the Jetpack Compose `MathText` renderer so PDF exports draw real stacked fractions,
    // integrals with upper/lower limits, square roots with top vinculum bars, powers/subscripts,
    // matrices, and overlines with exact vertical middle alignment.
    // =============================================================================================

    private class PdfMathBox(
        val width: Float,
        val height: Float,
        val isWhitespaceOnly: Boolean = false,
        val isForcedLineBreak: Boolean = false,
        val draw: (canvas: Canvas, x: Float, y: Float) -> Unit
    )

    private class PdfMathLine(
        val boxes: List<PdfMathBox>,
        val width: Float,
        val height: Float
    )

    private class PdfMathLayout(
        val lines: List<PdfMathLine>,
        val width: Float,
        val height: Float,
        val lineCount: Int,
        val firstLineCenterY: Float
    ) {
        fun draw(canvas: Canvas) {
            var currentY = 0f
            lines.forEachIndexed { index, line ->
                var currentX = 0f
                val rowCenterY = currentY + (line.height / 2f)
                for (box in line.boxes) {
                    val boxTopY = rowCenterY - (box.height / 2f)
                    box.draw(canvas, currentX, boxTopY)
                    currentX += box.width
                }
                currentY += line.height + if (index < lines.lastIndex) 4f else 0f
            }
        }
    }

    private fun buildMathLayout(
        rawText: String,
        basePaint: TextPaint,
        maxWidth: Float
    ): PdfMathLayout {
        val safeMaxWidth = maxWidth.coerceAtLeast(40f)
        val nodes = MathMarkdownFormatter.parseLatexNodes(rawText)
        val inlineBoxes = mutableListOf<PdfMathBox>()

        for (node in nodes) {
            when (node) {
                is LatexNode.TextRun -> {
                    val runPaint = createScaledPaint(
                        base = basePaint,
                        scale = 1f,
                        bold = node.bold,
                        italic = node.italic,
                        monospace = node.monospace
                    )
                    val paragraphs = node.text.split('\n')
                    paragraphs.forEachIndexed { pIdx, paragraph ->
                        if (pIdx > 0) {
                            inlineBoxes.add(
                                PdfMathBox(
                                    width = 0f,
                                    height = basePaint.textSize,
                                    isForcedLineBreak = true,
                                    draw = { _, _, _ -> }
                                )
                            )
                        }
                        if (paragraph.isNotEmpty()) {
                            val tokens = MathMarkdownFormatter.splitPreservingSpaces(paragraph)
                            for (token in tokens) {
                                val measuredW = runPaint.measureText(token)
                                if (measuredW > safeMaxWidth && token.trim().length > 1) {
                                    val fallbackLayout = buildStaticLayout(token.trim(), runPaint, safeMaxWidth.toInt())
                                    inlineBoxes.add(
                                        PdfMathBox(
                                            width = safeMaxWidth,
                                            height = fallbackLayout.height.toFloat(),
                                            draw = { c, x, y ->
                                                c.save()
                                                c.translate(x, y)
                                                fallbackLayout.draw(c)
                                                c.restore()
                                            }
                                        )
                                    )
                                } else {
                                    inlineBoxes.add(
                                        measureSingleTextBox(
                                            text = token,
                                            paint = runPaint,
                                            isWhitespaceOnly = token.all { it == ' ' }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    inlineBoxes.add(measureSingleLatexNode(node, basePaint))
                }
            }
        }

        val lines = mutableListOf<PdfMathLine>()
        var currentBoxes = mutableListOf<PdfMathBox>()
        var currentW = 0f
        var currentH = basePaint.textSize * 1.25f

        fun flushLine() {
            if (currentBoxes.isNotEmpty()) {
                lines.add(PdfMathLine(currentBoxes.toList(), currentW, currentH))
                currentBoxes = mutableListOf()
                currentW = 0f
                currentH = basePaint.textSize * 1.25f
            }
        }

        for (box in inlineBoxes) {
            if (box.isForcedLineBreak) {
                flushLine()
                continue
            }
            if (currentBoxes.isNotEmpty() && currentW + box.width > safeMaxWidth) {
                flushLine()
                if (box.isWhitespaceOnly) {
                    continue
                }
            }
            currentBoxes.add(box)
            currentW += box.width
            if (box.height > currentH) {
                currentH = box.height
            }
        }
        flushLine()

        if (lines.isEmpty()) {
            val defaultH = basePaint.textSize * 1.25f
            lines.add(PdfMathLine(emptyList(), 0f, defaultH))
        }

        val totalWidth = lines.maxOfOrNull { it.width } ?: 0f
        val totalHeight = lines.sumOf { it.height.toDouble() }.toFloat() + ((lines.size - 1).coerceAtLeast(0) * 4f)
        val firstLineCenterY = lines.first().height / 2f

        return PdfMathLayout(
            lines = lines,
            width = totalWidth,
            height = totalHeight,
            lineCount = lines.size,
            firstLineCenterY = firstLineCenterY
        )
    }

    private fun createScaledPaint(
        base: TextPaint,
        scale: Float = 1f,
        bold: Boolean = false,
        italic: Boolean = false,
        monospace: Boolean = false
    ): TextPaint {
        return TextPaint(base).apply {
            textSize = (base.textSize * scale).coerceAtLeast(6.5f)
            val baseBold = base.typeface?.isBold == true || base.isFakeBoldText
            val wantBold = baseBold || bold
            val wantItalic = (base.typeface?.isItalic == true) || italic
            val family = if (monospace) Typeface.MONOSPACE else Typeface.DEFAULT
            val style = when {
                wantBold && wantItalic -> Typeface.BOLD_ITALIC
                wantBold -> Typeface.BOLD
                wantItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            typeface = Typeface.create(family, style)
        }
    }

    private fun measureSingleTextBox(
        text: String,
        paint: TextPaint,
        isWhitespaceOnly: Boolean = false
    ): PdfMathBox {
        val fm = paint.fontMetrics
        val w = paint.measureText(text).coerceAtLeast(0f)
        val h = (fm.descent - fm.ascent).coerceAtLeast(paint.textSize)
        val baselineOffset = -fm.ascent
        return PdfMathBox(
            width = w,
            height = h,
            isWhitespaceOnly = isWhitespaceOnly,
            draw = { canvas, x, y ->
                if (text.isNotEmpty()) {
                    canvas.drawText(text, x, y + baselineOffset, paint)
                }
            }
        )
    }

    private fun measureFormulaGroup(
        nodes: List<LatexNode>,
        paint: TextPaint
    ): PdfMathBox {
        if (nodes.isEmpty()) {
            return PdfMathBox(0f, paint.textSize) { _, _, _ -> }
        }
        val childBoxes = nodes.map { measureSingleLatexNode(it, paint) }
        val totalW = childBoxes.sumOf { it.width.toDouble() }.toFloat()
        val maxH = childBoxes.maxOfOrNull { it.height } ?: paint.textSize
        return PdfMathBox(
            width = totalW,
            height = maxH,
            draw = { canvas, x, y ->
                var curX = x
                val midY = y + (maxH / 2f)
                for (child in childBoxes) {
                    child.draw(canvas, curX, midY - (child.height / 2f))
                    curX += child.width
                }
            }
        )
    }

    private fun measureSingleLatexNode(
        node: LatexNode,
        paint: TextPaint
    ): PdfMathBox {
        return when (node) {
            is LatexNode.TextRun -> {
                val runPaint = createScaledPaint(
                    base = paint,
                    scale = 1f,
                    bold = node.bold,
                    italic = node.italic,
                    monospace = node.monospace
                )
                measureSingleTextBox(node.text, runPaint)
            }

            is LatexNode.Fraction -> {
                val childPaint = createScaledPaint(paint, 0.84f)
                val numBox = measureFormulaGroup(node.numerator, childPaint)
                val denBox = measureFormulaGroup(node.denominator, childPaint)
                val padH = 2.5f
                val barGapV = 1.6f
                // Keep fraction bar at exact vertical midpoint so surrounding -, +, =, ln 2 align in the middle
                val halfH = maxOf(numBox.height, denBox.height) + barGapV
                val totalH = halfH * 2f
                val barW = maxOf(numBox.width, denBox.width) + 3.5f
                val totalW = barW + (padH * 2f)
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = paint.color
                    strokeWidth = (paint.textSize * 0.085f).coerceIn(0.75f, 1.25f)
                }
                PdfMathBox(
                    width = totalW,
                    height = totalH,
                    draw = { canvas, x, y ->
                        val barY = y + halfH
                        val numX = x + (totalW - numBox.width) / 2f
                        val numY = barY - barGapV - numBox.height
                        numBox.draw(canvas, numX, numY)

                        canvas.drawLine(x + padH, barY, x + totalW - padH, barY, barPaint)

                        val denX = x + (totalW - denBox.width) / 2f
                        val denY = barY + barGapV
                        denBox.draw(canvas, denX, denY)
                    }
                )
            }

            is LatexNode.Binomial -> {
                val childPaint = createScaledPaint(paint, 0.82f)
                val topBox = measureFormulaGroup(node.top, childPaint)
                val botBox = measureFormulaGroup(node.bottom, childPaint)
                val parenPaint = createScaledPaint(paint, 1.32f)
                val leftParen = measureSingleTextBox("(", parenPaint)
                val rightParen = measureSingleTextBox(")", parenPaint)
                val innerW = maxOf(topBox.width, botBox.width) + 3f
                val totalW = leftParen.width + innerW + rightParen.width + 2f
                val totalH = maxOf(topBox.height + botBox.height + 2f, leftParen.height)
                PdfMathBox(
                    width = totalW,
                    height = totalH,
                    draw = { canvas, x, y ->
                        val midY = y + (totalH / 2f)
                        leftParen.draw(canvas, x + 1f, midY - (leftParen.height / 2f))
                        val centerColX = x + 1f + leftParen.width + (innerW / 2f)
                        topBox.draw(canvas, centerColX - (topBox.width / 2f), midY - 1f - topBox.height)
                        botBox.draw(canvas, centerColX - (botBox.width / 2f), midY + 1f)
                        rightParen.draw(canvas, x + 1f + leftParen.width + innerW, midY - (rightParen.height / 2f))
                    }
                )
            }

            is LatexNode.Sqrt -> {
                val radBox = measureFormulaGroup(node.radicand, paint)
                val degPaint = createScaledPaint(paint, 0.62f)
                val degBox = node.degree?.takeIf { it.isNotEmpty() }?.let { measureFormulaGroup(it, degPaint) }
                val topBarPad = 1.6f
                val totalH = radBox.height + topBarPad + 1.2f
                val radicalScale = (radBox.height / paint.textSize).coerceIn(1.02f, 1.55f)
                val radicalPaint = createScaledPaint(paint, radicalScale, bold = true)
                val radicalBox = measureSingleTextBox("√", radicalPaint)
                val degAdvance = if (degBox != null) (degBox.width - 1.5f).coerceAtLeast(2.5f) else 0f
                val totalW = 1f + degAdvance + radicalBox.width + radBox.width + 2.5f
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = paint.color
                    strokeWidth = (paint.textSize * 0.08f).coerceIn(0.7f, 1.2f)
                }
                PdfMathBox(
                    width = totalW,
                    height = totalH,
                    draw = { canvas, x, y ->
                        var curX = x + 1f
                        if (degBox != null) {
                            degBox.draw(canvas, curX, y + 1f)
                            curX += degAdvance
                        }
                        val midY = y + (totalH / 2f)
                        radicalBox.draw(canvas, curX, midY - (radicalBox.height / 2f) + 0.8f)
                        val radStartX = curX + radicalBox.width
                        val barY = y + 1.2f
                        canvas.drawLine(radStartX - 0.8f, barY, radStartX + radBox.width + 1.2f, barY, barPaint)
                        radBox.draw(canvas, radStartX, y + topBarPad + 1f)
                    }
                )
            }

            is LatexNode.Overline -> {
                val contentBox = measureFormulaGroup(node.content, paint)
                val topHeight = when (node.kind) {
                    OverlineKind.BAR -> 2.5f
                    OverlineKind.VECTOR, OverlineKind.HAT -> paint.textSize * 0.55f
                }
                val totalW = contentBox.width + 2f
                val totalH = contentBox.height + topHeight
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = paint.color
                    strokeWidth = (paint.textSize * 0.08f).coerceIn(0.7f, 1.15f)
                }
                val ornamentPaint = createScaledPaint(paint, 0.68f, bold = true)
                PdfMathBox(
                    width = totalW,
                    height = totalH,
                    draw = { canvas, x, y ->
                        when (node.kind) {
                            OverlineKind.BAR -> {
                                canvas.drawLine(x + 1f, y + 1.4f, x + totalW - 1f, y + 1.4f, barPaint)
                            }
                            OverlineKind.VECTOR -> {
                                val arrowW = ornamentPaint.measureText("→")
                                canvas.drawText(
                                    "→",
                                    x + (totalW - arrowW) / 2f,
                                    y - ornamentPaint.fontMetrics.ascent * 0.78f,
                                    ornamentPaint
                                )
                            }
                            OverlineKind.HAT -> {
                                val hatW = ornamentPaint.measureText("^")
                                canvas.drawText(
                                    "^",
                                    x + (totalW - hatW) / 2f,
                                    y - ornamentPaint.fontMetrics.ascent * 0.82f,
                                    ornamentPaint
                                )
                            }
                        }
                        contentBox.draw(canvas, x + 1f, y + topHeight)
                    }
                )
            }

            is LatexNode.Delimited -> {
                val innerBox = measureFormulaGroup(node.inner, paint)
                val delimScale = (innerBox.height / (paint.textSize * 1.2f)).coerceIn(1.0f, 1.45f)
                val delimPaint = createScaledPaint(paint, delimScale)
                val leftBox = if (node.left.isNotEmpty() && node.left != ".") {
                    measureSingleTextBox(node.left, delimPaint)
                } else null
                val rightBox = if (node.right.isNotEmpty() && node.right != ".") {
                    measureSingleTextBox(node.right, delimPaint)
                } else null
                val totalW = (leftBox?.width ?: 0f) + innerBox.width + (rightBox?.width ?: 0f) + 1f
                val totalH = maxOf(innerBox.height, leftBox?.height ?: 0f, rightBox?.height ?: 0f)
                PdfMathBox(
                    width = totalW,
                    height = totalH,
                    draw = { canvas, x, y ->
                        var curX = x + 0.5f
                        val midY = y + (totalH / 2f)
                        if (leftBox != null) {
                            leftBox.draw(canvas, curX, midY - (leftBox.height / 2f))
                            curX += leftBox.width
                        }
                        innerBox.draw(canvas, curX, midY - (innerBox.height / 2f))
                        curX += innerBox.width
                        if (rightBox != null) {
                            rightBox.draw(canvas, curX, midY - (rightBox.height / 2f))
                        }
                    }
                )
            }

            is LatexNode.Script -> {
                val scriptPaint = createScaledPaint(paint, 0.72f)
                val baseBox = node.base?.let { measureSingleLatexNode(it, paint) }
                    ?: PdfMathBox(0f, paint.textSize) { _, _, _ -> }
                val supBox = node.superscript?.takeIf { it.isNotEmpty() }?.let { measureFormulaGroup(it, scriptPaint) }
                val subBox = node.subscript?.takeIf { it.isNotEmpty() }?.let { measureFormulaGroup(it, scriptPaint) }
                val scriptW = maxOf(supBox?.width ?: 0f, subBox?.width ?: 0f)
                val totalW = baseBox.width + scriptW + 0.8f
                val halfBase = baseBox.height / 2f
                val topReach = if (supBox != null) maxOf(halfBase, (halfBase * 0.28f) + supBox.height) else halfBase
                val botReach = if (subBox != null) maxOf(halfBase, (halfBase * 0.28f) + subBox.height) else halfBase
                val halfH = maxOf(topReach, botReach)
                val totalH = halfH * 2f
                PdfMathBox(
                    width = totalW,
                    height = totalH,
                    draw = { canvas, x, y ->
                        val midY = y + halfH
                        baseBox.draw(canvas, x, midY - halfBase)
                        val scriptX = x + baseBox.width + 0.4f
                        supBox?.let { it.draw(canvas, scriptX, midY - topReach) }
                        subBox?.let { it.draw(canvas, scriptX, midY + botReach - it.height) }
                    }
                )
            }

            is LatexNode.BigOp -> {
                val limitPaint = createScaledPaint(paint, 0.70f)
                val opScale = when (node.symbol) {
                    "∫", "∮" -> 1.55f
                    "∑", "∏" -> 1.32f
                    else -> 1.0f
                }
                val opPaint = createScaledPaint(paint, opScale)
                val opBox = measureSingleTextBox(node.symbol, opPaint)
                val upperBox = node.upper?.takeIf { it.isNotEmpty() }?.let { measureFormulaGroup(it, limitPaint) }
                val lowerBox = node.lower?.takeIf { it.isNotEmpty() }?.let { measureFormulaGroup(it, limitPaint) }

                if (node.limitsUnderOver) {
                    val halfOp = opBox.height / 2f
                    val topReach = halfOp + (if (upperBox != null) upperBox.height + 1f else 0f)
                    val botReach = halfOp + (if (lowerBox != null) lowerBox.height + 1f else 0f)
                    val halfH = maxOf(topReach, botReach)
                    val totalH = halfH * 2f
                    val contentW = maxOf(opBox.width, upperBox?.width ?: 0f, lowerBox?.width ?: 0f)
                    val totalW = contentW + 4f
                    PdfMathBox(
                        width = totalW,
                        height = totalH,
                        draw = { canvas, x, y ->
                            val midY = y + halfH
                            opBox.draw(canvas, x + (totalW - opBox.width) / 2f, midY - halfOp)
                            upperBox?.let {
                                it.draw(canvas, x + (totalW - it.width) / 2f, midY - halfOp - 1f - it.height)
                            }
                            lowerBox?.let {
                                it.draw(canvas, x + (totalW - it.width) / 2f, midY + halfOp + 1f)
                            }
                        }
                    )
                } else {
                    val limitsW = maxOf(upperBox?.width ?: 0f, lowerBox?.width ?: 0f)
                    val gapAfterOp = if (limitsW > 0f) 1.2f else 0f
                    val endPad = 2.8f
                    val totalW = 1f + opBox.width + gapAfterOp + limitsW + endPad
                    val halfOp = opBox.height / 2f
                    val topReach = maxOf(halfOp, (halfOp * 0.35f) + (upperBox?.height ?: 0f))
                    val botReach = maxOf(halfOp, (halfOp * 0.35f) + (lowerBox?.height ?: 0f))
                    val halfH = maxOf(topReach, botReach)
                    val totalH = halfH * 2f
                    PdfMathBox(
                        width = totalW,
                        height = totalH,
                        draw = { canvas, x, y ->
                            val midY = y + halfH
                            opBox.draw(canvas, x + 1f, midY - halfOp)
                            val limitX = x + 1f + opBox.width + gapAfterOp
                            upperBox?.let {
                                it.draw(canvas, limitX, midY - topReach)
                            }
                            lowerBox?.let {
                                it.draw(canvas, limitX, midY + botReach - it.height)
                            }
                        }
                    )
                }
            }

            is LatexNode.MatrixNode -> {
                val cellPaint = createScaledPaint(paint, 0.85f)
                val measuredRows = node.rows.map { row ->
                    row.map { cell -> measureFormulaGroup(cell, cellPaint) }
                }
                val numCols = measuredRows.maxOfOrNull { it.size } ?: 1
                val colWidths = FloatArray(numCols) { cIdx ->
                    measuredRows.maxOfOrNull { row -> row.getOrNull(cIdx)?.width ?: 0f } ?: 0f
                }
                val rowHeights = measuredRows.map { row ->
                    row.maxOfOrNull { it.height } ?: cellPaint.textSize
                }
                val colGap = 6f
                val rowGap = 2.5f
                val gridW = colWidths.sum() + ((numCols - 1).coerceAtLeast(0) * colGap)
                val gridH = rowHeights.sum() + ((measuredRows.size - 1).coerceAtLeast(0) * rowGap)
                val bracketScale = (gridH / (paint.textSize * 1.15f)).coerceIn(1.0f, 2.2f)
                val bracketPaint = createScaledPaint(paint, bracketScale)
                val leftB = if (node.leftDelim.isNotEmpty()) measureSingleTextBox(node.leftDelim, bracketPaint) else null
                val rightB = if (node.rightDelim.isNotEmpty()) measureSingleTextBox(node.rightDelim, bracketPaint) else null
                val totalW = (leftB?.width ?: 0f) + gridW + (rightB?.width ?: 0f) + 6f
                val totalH = maxOf(gridH + 2f, leftB?.height ?: 0f, rightB?.height ?: 0f)
                PdfMathBox(
                    width = totalW,
                    height = totalH,
                    draw = { canvas, x, y ->
                        val midY = y + (totalH / 2f)
                        var curX = x + 1.5f
                        if (leftB != null) {
                            leftB.draw(canvas, curX, midY - (leftB.height / 2f))
                            curX += leftB.width + 1.5f
                        }
                        var rowY = midY - (gridH / 2f)
                        measuredRows.forEachIndexed { rIdx, row ->
                            val rH = rowHeights[rIdx]
                            var cellX = curX
                            row.forEachIndexed { cIdx, cellBox ->
                                val cW = colWidths[cIdx]
                                cellBox.draw(
                                    canvas,
                                    cellX + (cW - cellBox.width) / 2f,
                                    rowY + (rH - cellBox.height) / 2f
                                )
                                cellX += cW + colGap
                            }
                            rowY += rH + rowGap
                        }
                        curX += gridW + 1.5f
                        if (rightB != null) {
                            rightB.draw(canvas, curX, midY - (rightB.height / 2f))
                        }
                    }
                )
            }
        }
    }
}

