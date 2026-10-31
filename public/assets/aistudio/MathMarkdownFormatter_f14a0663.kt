package com.example.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * AST Node representation for our Native Jetpack Compose LaTeX Math Renderer.
 */
sealed interface LatexNode {
    data class TextRun(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val monospace: Boolean = false
    ) : LatexNode

    data class Fraction(
        val numerator: List<LatexNode>,
        val denominator: List<LatexNode>
    ) : LatexNode

    data class Binomial(
        val top: List<LatexNode>,
        val bottom: List<LatexNode>
    ) : LatexNode

    data class Sqrt(
        val degree: List<LatexNode>?,
        val radicand: List<LatexNode>
    ) : LatexNode

    data class Script(
        val base: LatexNode?,
        val superscript: List<LatexNode>?,
        val subscript: List<LatexNode>?
    ) : LatexNode

    data class Overline(
        val content: List<LatexNode>,
        val kind: OverlineKind = OverlineKind.BAR
    ) : LatexNode

    data class Delimited(
        val left: String,
        val right: String,
        val inner: List<LatexNode>
    ) : LatexNode

    data class BigOp(
        val symbol: String,
        val lower: List<LatexNode>?,
        val upper: List<LatexNode>?,
        val limitsUnderOver: Boolean
    ) : LatexNode

    data class MatrixNode(
        val rows: List<List<List<LatexNode>>>,
        val leftDelim: String,
        val rightDelim: String
    ) : LatexNode
}

enum class OverlineKind {
    BAR, VECTOR, HAT
}

/**
 * Native Jetpack Compose LaTeX & Math Renderer (`MathText`).
 * Supports:
 * - Standard LaTeX inline `$...$` and display `$$...$$`, `\(...\)`, `\[...\]`
 * - Stacked fractions `\frac{a}{b}`, `\dfrac{a}{b}`, `\tfrac{a}{b}`, `{a \over b}`, and math slash fractions
 * - Square roots `\sqrt{x}` and nth roots `\sqrt[n]{x}` with a real top vinculum bar
 * - Superscripts `x^{2n}`, `z^{-n}`, `z^(-n)` and subscripts `a_{n-1}`, `H_2O` (including combined `x_i^2`)
 * - Big operators with limits: `\lim_{x \to 0}`, `\sum_{i=1}^{n}`, `\prod`, `\int_{a}^{b}`
 * - Binomials `\binom{n}{r}`, overlines `\overline{AB}`, `\bar{z}`, vectors `\vec{a}`, hats `\hat{i}`
 * - Matrices & determinants `\begin{pmatrix}...\end{pmatrix}`, `\begin{vmatrix}...\end{vmatrix}`
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null
) {
    val nodes = remember(text) {
        MathMarkdownFormatter.parseLatexNodes(text)
    }

    val resolvedColor = when {
        color != Color.Unspecified -> color
        style.color != Color.Unspecified -> style.color
        else -> MaterialTheme.colorScheme.onSurface
    }
    val baseFontSize = if (style.fontSize != TextUnit.Unspecified) style.fontSize else 16.sp
    val effectiveWeight = fontWeight ?: style.fontWeight ?: FontWeight.Medium

    val hasComplexNodes = remember(nodes) {
        MathMarkdownFormatter.containsStructuralLatexNode(nodes)
    }

    if (!hasComplexNodes) {
        val annotated = remember(text) {
            MathMarkdownFormatter.formatMathAndMarkdown(text)
        }
        Text(
            text = annotated,
            modifier = modifier,
            style = style,
            fontWeight = fontWeight,
            color = resolvedColor,
            textAlign = textAlign
        )
        return
    }

    val horizontalArrangement = when (textAlign) {
        TextAlign.Center -> Arrangement.Center
        TextAlign.End, TextAlign.Right -> Arrangement.End
        else -> Arrangement.Start
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        RenderLatexNodeListInline(
            nodes = nodes,
            fontSize = baseFontSize,
            fontWeight = effectiveWeight,
            color = resolvedColor,
            style = style,
            splitWordsForWrapping = true
        )
    }
}

@Composable
private fun RowScope.RenderLatexNodeListInline(
    nodes: List<LatexNode>,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    style: TextStyle,
    splitWordsForWrapping: Boolean
) {
    nodes.forEach { node ->
        when (node) {
            is LatexNode.TextRun -> {
                val runWeight = if (node.bold) FontWeight.ExtraBold else fontWeight
                val runStyle = if (node.italic) FontStyle.Italic else FontStyle.Normal
                val runFamily = if (node.monospace) FontFamily.Monospace else style.fontFamily

                if (splitWordsForWrapping && node.text.contains(' ')) {
                    val tokens = remember(node.text) {
                        MathMarkdownFormatter.splitPreservingSpaces(node.text)
                    }
                    tokens.forEach { token ->
                        Text(
                            text = token,
                            modifier = Modifier.align(Alignment.CenterVertically),
                            style = style.copy(
                                fontSize = fontSize,
                                lineHeight = (fontSize.value * 1.25f).sp,
                                fontWeight = runWeight,
                                fontStyle = runStyle,
                                fontFamily = runFamily
                            ),
                            color = color
                        )
                    }
                } else if (node.text.isNotEmpty()) {
                    Text(
                        text = node.text,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = style.copy(
                            fontSize = fontSize,
                            lineHeight = (fontSize.value * 1.25f).sp,
                            fontWeight = runWeight,
                            fontStyle = runStyle,
                            fontFamily = runFamily
                        ),
                        color = color
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    RenderSingleLatexNode(
                        node = node,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color,
                        style = style
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderLatexFormulaGroup(
    nodes: List<LatexNode>,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    style: TextStyle
) {
    if (nodes.isEmpty()) return
    if (!MathMarkdownFormatter.containsStructuralLatexNode(nodes)) {
        val annotated = remember(nodes) {
            MathMarkdownFormatter.nodesToAnnotatedString(nodes)
        }
        Text(
            text = annotated,
            style = style.copy(
                fontSize = fontSize,
                lineHeight = (fontSize.value * 1.15f).sp
            ),
            fontWeight = fontWeight,
            color = color,
            softWrap = false
        )
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        nodes.forEach { node ->
            RenderSingleLatexNode(
                node = node,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                style = style
            )
        }
    }
}

@Composable
private fun RenderSingleLatexNode(
    node: LatexNode,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    style: TextStyle
) {
    when (node) {
        is LatexNode.TextRun -> {
            if (node.text.isNotEmpty()) {
                Text(
                    text = node.text,
                    style = style.copy(
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 1.15f).sp,
                        fontWeight = if (node.bold) FontWeight.ExtraBold else fontWeight,
                        fontStyle = if (node.italic) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = if (node.monospace) FontFamily.Monospace else style.fontFamily
                    ),
                    color = color,
                    softWrap = false
                )
            }
        }

        is LatexNode.Fraction -> {
            val childFontSize = (fontSize.value * 0.84f).coerceAtLeast(10f).sp
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(horizontal = 3.dp, vertical = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RenderLatexFormulaGroup(
                        nodes = node.numerator,
                        fontSize = childFontSize,
                        fontWeight = fontWeight,
                        color = color,
                        style = style
                    )
                }
                Spacer(modifier = Modifier.height(1.5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.4.dp)
                        .background(color)
                )
                Spacer(modifier = Modifier.height(1.5.dp))
                Box(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RenderLatexFormulaGroup(
                        nodes = node.denominator,
                        fontSize = childFontSize,
                        fontWeight = fontWeight,
                        color = color,
                        style = style
                    )
                }
            }
        }

        is LatexNode.Binomial -> {
            val childFontSize = (fontSize.value * 0.82f).coerceAtLeast(10f).sp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text(
                    text = "(",
                    style = style.copy(fontSize = (fontSize.value * 1.35f).sp),
                    fontWeight = FontWeight.Normal,
                    color = color
                )
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RenderLatexFormulaGroup(
                        nodes = node.top,
                        fontSize = childFontSize,
                        fontWeight = fontWeight,
                        color = color,
                        style = style
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    RenderLatexFormulaGroup(
                        nodes = node.bottom,
                        fontSize = childFontSize,
                        fontWeight = fontWeight,
                        color = color,
                        style = style
                    )
                }
                Text(
                    text = ")",
                    style = style.copy(fontSize = (fontSize.value * 1.35f).sp),
                    fontWeight = FontWeight.Normal,
                    color = color
                )
            }
        }

        is LatexNode.Sqrt -> {
            val radicandHasFraction = remember(node.radicand) {
                node.radicand.any { it is LatexNode.Fraction }
            }
            val radicalFontSize = if (radicandHasFraction) {
                (fontSize.value * 1.45f).sp
            } else {
                (fontSize.value * 1.08f).sp
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 1.dp)
            ) {
                if (!node.degree.isNullOrEmpty()) {
                    Box(modifier = Modifier.padding(bottom = 6.dp)) {
                        RenderLatexFormulaGroup(
                            nodes = node.degree,
                            fontSize = (fontSize.value * 0.65f).coerceAtLeast(9f).sp,
                            fontWeight = fontWeight,
                            color = color,
                            style = style
                        )
                    }
                }
                Text(
                    text = "√",
                    style = style.copy(
                        fontSize = radicalFontSize,
                        lineHeight = radicalFontSize
                    ),
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Column(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.3.dp)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Box(modifier = Modifier.padding( start = 1.dp, end = 2.dp)) {
                        RenderLatexFormulaGroup(
                            nodes = node.radicand,
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            color = color,
                            style = style
                        )
                    }
                }
            }
        }

        is LatexNode.Overline -> {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(horizontal = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (node.kind) {
                    OverlineKind.BAR -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.3.dp)
                                .background(color)
                        )
                    }
                    OverlineKind.VECTOR -> {
                        Text(
                            text = "→",
                            style = style.copy(
                                fontSize = (fontSize.value * 0.7f).sp,
                                lineHeight = (fontSize.value * 0.7f).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                    OverlineKind.HAT -> {
                        Text(
                            text = "^",
                            style = style.copy(
                                fontSize = (fontSize.value * 0.75f).sp,
                                lineHeight = (fontSize.value * 0.7f).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
                RenderLatexFormulaGroup(
                    nodes = node.content,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = color,
                    style = style
                )
            }
        }

        is LatexNode.Delimited -> {
            val isTall = remember(node.inner) {
                MathMarkdownFormatter.containsStructuralLatexNode(node.inner)
            }
            val delimSize = if (isTall) (fontSize.value * 1.32f).sp else fontSize
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (node.left.isNotEmpty() && node.left != ".") {
                    Text(
                        text = node.left,
                        style = style.copy(fontSize = delimSize),
                        fontWeight = fontWeight,
                        color = color
                    )
                }
                RenderLatexFormulaGroup(
                    nodes = node.inner,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = color,
                    style = style
                )
                if (node.right.isNotEmpty() && node.right != ".") {
                    Text(
                        text = node.right,
                        style = style.copy(fontSize = delimSize),
                        fontWeight = fontWeight,
                        color = color
                    )
                }
            }
        }

        is LatexNode.Script -> {
            val scriptFontSize = (fontSize.value * 0.72f).coerceAtLeast(9.5f).sp
            val isSimpleScript = (node.base == null || node.base is LatexNode.TextRun) &&
                (node.superscript == null || !MathMarkdownFormatter.containsStructuralLatexNode(node.superscript)) &&
                (node.subscript == null || !MathMarkdownFormatter.containsStructuralLatexNode(node.subscript)) &&
                !(node.superscript != null && node.subscript != null)

            if (isSimpleScript) {
                val annotated = remember(node) {
                    MathMarkdownFormatter.nodesToAnnotatedString(listOf(node))
                }
                Text(
                    text = annotated,
                    style = style.copy(
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 1.18f).sp
                    ),
                    fontWeight = fontWeight,
                    color = color,
                    softWrap = false
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (node.base != null) {
                        RenderSingleLatexNode(
                            node = node.base,
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            color = color,
                            style = style
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (!node.superscript.isNullOrEmpty()) {
                            Box(modifier = Modifier.padding(bottom = if (node.subscript != null) 1.dp else 6.dp)) {
                                RenderLatexFormulaGroup(
                                    nodes = node.superscript,
                                    fontSize = scriptFontSize,
                                    fontWeight = fontWeight,
                                    color = color,
                                    style = style
                                )
                            }
                        }
                        if (!node.subscript.isNullOrEmpty()) {
                            Box(modifier = Modifier.padding(top = if (node.superscript != null) 1.dp else 6.dp)) {
                                RenderLatexFormulaGroup(
                                    nodes = node.subscript,
                                    fontSize = scriptFontSize,
                                    fontWeight = fontWeight,
                                    color = color,
                                    style = style
                                )
                            }
                        }
                    }
                }
            }
        }

        is LatexNode.BigOp -> {
            val limitFontSize = (fontSize.value * 0.70f).coerceAtLeast(9.5f).sp
            val opFontSize = when (node.symbol) {
                "∫", "∮" -> (fontSize.value * 1.55f).sp
                "∑", "∏" -> (fontSize.value * 1.32f).sp
                else -> fontSize
            }
            if (node.limitsUnderOver) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    if (!node.upper.isNullOrEmpty()) {
                        RenderLatexFormulaGroup(
                            nodes = node.upper,
                            fontSize = limitFontSize,
                            fontWeight = fontWeight,
                            color = color,
                            style = style
                        )
                    }
                    Text(
                        text = node.symbol,
                        style = style.copy(
                            fontSize = opFontSize,
                            lineHeight = opFontSize
                        ),
                        fontWeight = fontWeight,
                        color = color
                    )
                    if (!node.lower.isNullOrEmpty()) {
                        RenderLatexFormulaGroup(
                            nodes = node.lower,
                            fontSize = limitFontSize,
                            fontWeight = fontWeight,
                            color = color,
                            style = style
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 1.dp, end = 3.dp)
                ) {
                    Text(
                        text = node.symbol,
                        style = style.copy(
                            fontSize = opFontSize,
                            lineHeight = opFontSize
                        ),
                        fontWeight = fontWeight,
                        color = color
                    )
                    if (!node.upper.isNullOrEmpty() || !node.lower.isNullOrEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.padding(start = 1.dp)
                        ) {
                            if (!node.upper.isNullOrEmpty()) {
                                Box(modifier = Modifier.padding(bottom = if (!node.lower.isNullOrEmpty()) 5.dp else 8.dp)) {
                                    RenderLatexFormulaGroup(
                                        nodes = node.upper,
                                        fontSize = limitFontSize,
                                        fontWeight = fontWeight,
                                        color = color,
                                        style = style
                                    )
                                }
                            }
                            if (!node.lower.isNullOrEmpty()) {
                                Box(modifier = Modifier.padding(top = if (!node.upper.isNullOrEmpty()) 5.dp else 8.dp)) {
                                    RenderLatexFormulaGroup(
                                        nodes = node.lower,
                                        fontSize = limitFontSize,
                                        fontWeight = fontWeight,
                                        color = color,
                                        style = style
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        is LatexNode.MatrixNode -> {
            val cellFontSize = (fontSize.value * 0.85f).coerceAtLeast(10f).sp
            val bracketFontSize = (fontSize.value * (node.rows.size.coerceAtLeast(1) * 0.72f + 0.5f)).sp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                if (node.leftDelim.isNotEmpty()) {
                    Text(
                        text = node.leftDelim,
                        style = style.copy(fontSize = bracketFontSize),
                        fontWeight = FontWeight.Light,
                        color = color
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    node.rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { cell ->
                                Box(contentAlignment = Alignment.Center) {
                                    RenderLatexFormulaGroup(
                                        nodes = cell,
                                        fontSize = cellFontSize,
                                        fontWeight = fontWeight,
                                        color = color,
                                        style = style
                                    )
                                }
                            }
                        }
                    }
                }
                if (node.rightDelim.isNotEmpty()) {
                    Text(
                        text = node.rightDelim,
                        style = style.copy(fontSize = bracketFontSize),
                        fontWeight = FontWeight.Light,
                        color = color
                    )
                }
            }
        }
    }
}

object MathMarkdownFormatter {

    // Ordered longest-first so longer LaTeX commands match before shorter prefixes
    private val LATEX_SYMBOLS: List<Pair<String, String>> = listOf(
        "\\rightleftharpoons" to "⇌",
        "\\leftrightarrow" to "↔",
        "\\Leftrightarrow" to "⇔",
        "\\rightarrow" to "→",
        "\\Rightarrow" to "⇒",
        "\\leftarrow" to "←",
        "\\Leftarrow" to "⇐",
        "\\uparrow" to "↑",
        "\\downarrow" to "↓",
        "\\subseteq" to "⊆",
        "\\supseteq" to "⊇",
        "\\varnothing" to "∅",
        "\\emptyset" to "∅",
        "\\therefore" to "∴",
        "\\because" to "∵",
        "\\triangle" to "△",
        "\\parallel" to "∥",
        "\\varepsilon" to "ε",
        "\\vartheta" to "ϑ",
        "\\varphi" to "φ",
        "\\approx" to "≈",
        "\\propto" to "∝",
        "\\degree" to "°",
        "^{\\circ}" to "°",
        "^(\\circ)" to "°",
        "^\\circ" to "°",
        "\\infty" to "∞",
        "\\partial" to "∂",
        "\\nabla" to "∇",
        "\\forall" to "∀",
        "\\exists" to "∃",
        "\\notin" to "∉",
        "\\subset" to "⊂",
        "\\supset" to "⊃",
        "\\equiv" to "≡",
        "\\simeq" to "≃",
        "\\cong" to "≅",
        "\\times" to "×",
        "\\cdot" to "·",
        "\\bullet" to "•",
        "\\oplus" to "⊕",
        "\\otimes" to "⊗",
        "\\div" to "÷",
        "\\pm" to "±",
        "\\mp" to "∓",
        "\\leq" to "≤",
        "\\geq" to "≥",
        "\\neq" to "≠",
        "\\angle" to "∠",
        "\\perp" to "⊥",
        "\\hbar" to "ℏ",
        "\\alpha" to "α",
        "\\beta" to "β",
        "\\gamma" to "γ",
        "\\Gamma" to "Γ",
        "\\delta" to "δ",
        "\\Delta" to "Δ",
        "\\epsilon" to "ε",
        "\\zeta" to "ζ",
        "\\eta" to "η",
        "\\theta" to "θ",
        "\\Theta" to "Θ",
        "\\iota" to "ι",
        "\\kappa" to "κ",
        "\\lambda" to "λ",
        "\\Lambda" to "Λ",
        "\\mu" to "μ",
        "\\nu" to "ν",
        "\\xi" to "ξ",
        "\\Xi" to "Ξ",
        "\\pi" to "π",
        "\\Pi" to "Π",
        "\\rho" to "ρ",
        "\\sigma" to "σ",
        "\\Sigma" to "Σ",
        "\\tau" to "τ",
        "\\upsilon" to "υ",
        "\\phi" to "φ",
        "\\Phi" to "Φ",
        "\\chi" to "χ",
        "\\psi" to "ψ",
        "\\Psi" to "Ψ",
        "\\omega" to "ω",
        "\\Omega" to "Ω",
        "\\arcsin" to "arcsin",
        "\\arccos" to "arccos",
        "\\arctan" to "arctan",
        "\\cosec" to "cosec",
        "\\sinh" to "sinh",
        "\\cosh" to "cosh",
        "\\tanh" to "tanh",
        "\\sin" to "sin",
        "\\cos" to "cos",
        "\\tan" to "tan",
        "\\cot" to "cot",
        "\\sec" to "sec",
        "\\csc" to "csc",
        "\\log" to "log",
        "\\ln" to "ln",
        "\\det" to "det",
        "\\gcd" to "gcd",
        "\\lcm" to "lcm",
        "\\max" to "max",
        "\\min" to "min",
        "\\cup" to "∪",
        "\\cap" to "∩",
        "\\in" to "∈",
        "\\le" to "≤",
        "\\ge" to "≥",
        "\\ne" to "≠",
        "\\to" to "→",
        "\\sim" to "∼",
        "\\circ" to "°",
        "\\ell" to "ℓ",
        "\\aleph" to "ℵ",
        "\\neg" to "¬",
        "\\lnot" to "¬",
        "\\land" to "∧",
        "\\lor" to "∨",
        "\\{" to "{",
        "\\}" to "}",
        "\\|" to "‖",
        "\\," to " ",
        "\\;" to " ",
        "\\:" to " ",
        "\\!" to "",
        "\\quad" to "  ",
        "\\qquad" to "   "
    )

    private val BLACKBOARD_MAP = mapOf(
        "R" to "ℝ",
        "N" to "ℕ",
        "Z" to "ℤ",
        "Q" to "ℚ",
        "C" to "ℂ"
    )

    private val SUPERSCRIPT_MAP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'a' to 'ᵃ', 'b' to 'ᵇ', 'c' to 'ᶜ',
        'd' to 'ᵈ', 'e' to 'ᵉ', 'f' to 'ᶠ', 'g' to 'ᵍ', 'h' to 'ʰ',
        'j' to 'ʲ', 'k' to 'ᵏ', 'l' to 'ˡ', 'm' to 'ᵐ', 'o' to 'ᵒ',
        'p' to 'ᵖ', 'r' to 'ʳ', 's' to 'ˢ', 't' to 'ᵗ', 'u' to 'ᵘ',
        'v' to 'ᵛ', 'w' to 'ʷ', 'x' to 'ˣ', 'y' to 'ʸ', 'z' to 'ᶻ'
    )

    private val SUBSCRIPT_MAP = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
        'v' to 'ᵥ', 'x' to 'ₓ'
    )

    private val MATH_FUNCTIONS = setOf(
        "sin", "cos", "tan", "cot", "sec", "cosec", "csc",
        "arcsin", "arccos", "arctan", "sinh", "cosh", "tanh",
        "log", "ln", "lim", "exp", "det"
    )

    /**
     * Self-heals LaTeX strings where JSON escape sequences accidentally turned
     * `\frac` into `\u000Crac`, `\theta` into `\theta` (Tab+heta), `\tan` into Tab+an,
     * `\beta` into Backspace+eta, etc.
     */
    fun repairCorruptedLatexEscapes(raw: String): String {
        if (raw.isEmpty()) return raw
        var s = raw
        // Recover \f (Form feed \u000C) -> \frac, \forall
        if (s.contains('\u000C')) {
            s = s.replace("\u000Crac", "\\frac")
                .replace("\u000Corall", "\\forall")
                .replace("\u000C", "\\f")
        }
        // Recover \b (Backspace \u0008) -> \beta, \binom, \bar, \because, \begin, \mathbf
        if (s.contains('\u0008')) {
            s = s.replace("\u0008eta", "\\beta")
                .replace("\u0008inom", "\\binom")
                .replace("\u0008ar", "\\bar")
                .replace("\u0008ecause", "\\because")
                .replace("\u0008egin", "\\begin")
                .replace("\u0008", "\\b")
        }
        // Recover \t (Tab \u0009) -> \theta, \tan, \times, \tfrac, \to, \triangle, \therefore, \tau, \text
        if (s.contains('\t')) {
            s = s.replace("\theta", "\\theta")
                .replace("\tan", "\\tan")
                .replace("\times", "\\times")
                .replace("\tfrac", "\\tfrac")
                .replace("\triangle", "\\triangle")
                .replace("\therefore", "\\therefore")
                .replace("\tau", "\\tau")
                .replace("\text", "\\text")
                .replace("\to", "\\to")
        }
        return s
    }

    fun containsStructuralLatexNode(nodes: List<LatexNode>): Boolean {
        for (node in nodes) {
            when (node) {
                is LatexNode.Fraction,
                is LatexNode.Binomial,
                is LatexNode.Sqrt,
                is LatexNode.Overline,
                is LatexNode.BigOp,
                is LatexNode.MatrixNode -> return true
                is LatexNode.Delimited -> {
                    if (containsStructuralLatexNode(node.inner)) return true
                }
                is LatexNode.Script -> {
                    if (node.base != null && node.base !is LatexNode.TextRun) return true
                    if (node.superscript != null && node.subscript != null) return true
                    if (node.superscript != null && containsStructuralLatexNode(node.superscript)) return true
                    if (node.subscript != null && containsStructuralLatexNode(node.subscript)) return true
                }
                is LatexNode.TextRun -> Unit
            }
        }
        return false
    }

    fun splitPreservingSpaces(text: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        for (ch in text) {
            if (ch == ' ') {
                sb.append(' ')
                result.add(sb.toString())
                sb.clear()
            } else {
                sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) {
            result.add(sb.toString())
        }
        return result
    }

    /**
     * Parses raw text (containing LaTeX `$...$`, `\frac{}{}`, `\sqrt{}`, `^{}`, `_{}`, slash fractions, etc.)
     * into a list of [LatexNode]s.
     */
    fun parseLatexNodes(rawText: String): List<LatexNode> {
        if (rawText.isEmpty()) return emptyList()
        val repaired = repairCorruptedLatexEscapes(rawText)
        val preprocessed = preprocessRawLatex(repaired, convertSlashFractions = true)
        return parseNodesInternal(preprocessed)
    }

    private fun preprocessRawLatex(input: String, convertSlashFractions: Boolean): String {
        var s = input
        // Strip outer/inline LaTeX delimiters while keeping their content
        s = s.replace("$$", "")
        s = s.replace("\\(", "").replace("\\)", "")
        s = s.replace("\\[", "").replace("\\]", "")
        if (s.count { it == '$' } >= 2) {
            s = s.replace("$", "")
        }

        // Convert {a \over b} -> \frac{a}{b}
        if (s.contains("\\over")) {
            s = convertOverToFrac(s)
        }

        // Replace \mathbb{R} -> ℝ, etc.
        s = replaceSingleArgCommand(s, listOf("\\mathbb")) { inner ->
            BLACKBOARD_MAP[inner.trim()] ?: inner.trim()
        }

        // Replace \text{...}, \mathrm{...}, \operatorname{...}, \mbox{...}
        s = replaceSingleArgCommand(s, listOf("\\text", "\\mathrm", "\\operatorname", "\\mbox")) { inner ->
            inner
        }

        // Replace simple LaTeX symbols & Greek letters FIRST so that \pi/2 becomes π/2 before any slash analysis
        for ((cmd, symbol) in LATEX_SYMBOLS) {
            if (s.contains(cmd)) {
                val lastChar = cmd.last()
                s = if (lastChar.isLetter()) {
                    // If a trig/log function like \sin is immediately followed by a Greek symbol or letter (e.g. \sin\theta -> \sin θ), add a space
                    val isMathFn = symbol in MATH_FUNCTIONS
                    s.replace(Regex(Regex.escape(cmd) + """(?![a-zA-Z])""")) { matchResult ->
                        val nextIdx = matchResult.range.last + 1
                        val nextChar = if (nextIdx < s.length) s[nextIdx] else ' '
                        if (isMathFn && (nextChar == '\\' || nextChar in "θαβγδΔπφωΩ" || nextChar.isLetterOrDigit())) {
                            "$symbol "
                        } else {
                            symbol
                        }
                    }
                } else {
                    s.replace(cmd, symbol)
                }
            }
        }

        // Clean up multiple spaces created around trig functions and thin spaces (\,)
        s = s.replace(Regex("""\b(sin|cos|tan|cot|sec|cosec|csc|log|ln)\s+\^"""), "$1^")
        s = s.replace(Regex("""\b(sin|cos|tan|cot|sec|cosec|csc|log|ln)\s+\("""), "$1(")
        s = s.replace(Regex(""" {2,}"""), " ")

        // Convert plain math slash fractions (like "1 / sin θ", "sin θ / cos θ", "√3 / 2") into \frac{a}{b}
        // ONLY if the expression is plain math (not already using \frac, \int, \sum, \lim, or ^{...} / _{...} limits)
        val hasStructuralLatex = s.contains("\\frac") ||
            s.contains("\\dfrac") ||
            s.contains("\\tfrac") ||
            s.contains("\\int") ||
            s.contains("\\oint") ||
            s.contains("\\sum") ||
            s.contains("\\prod") ||
            s.contains("\\lim")
        if (convertSlashFractions && s.contains('/') && !hasStructuralLatex) {
            s = convertSlashFractionsToLatex(s)
        }

        return s
    }

    private fun convertOverToFrac(text: String): String {
        var current = text
        var searchIdx = 0
        while (true) {
            val overIdx = current.indexOf("\\over", searchIdx)
            if (overIdx == -1) break
            // Find enclosing { ... }
            var openIdx = overIdx - 1
            var depth = 0
            while (openIdx >= 0) {
                if (current[openIdx] == '}') depth++
                else if (current[openIdx] == '{') {
                    if (depth == 0) break
                    depth--
                }
                openIdx--
            }
            if (openIdx >= 0 && current[openIdx] == '{') {
                val extracted = extractBalancedBraces(current, openIdx)
                if (extracted != null) {
                    val inner = extracted.first
                    val localOver = inner.indexOf("\\over")
                    if (localOver != -1) {
                        val num = inner.substring(0, localOver).trim()
                        val den = inner.substring(localOver + 5).trim()
                        val replacement = "\\frac{$num}{$den}"
                        current = current.substring(0, openIdx) + replacement + current.substring(extracted.second)
                        searchIdx = openIdx + replacement.length
                        continue
                    }
                }
            }
            searchIdx = overIdx + 5
        }
        return current
    }

    private fun parseNodesInternal(s: String): List<LatexNode> {
        val result = mutableListOf<LatexNode>()
        val textBuffer = StringBuilder()
        var i = 0
        val len = s.length

        fun flushText() {
            if (textBuffer.isNotEmpty()) {
                result.add(LatexNode.TextRun(textBuffer.toString()))
                textBuffer.clear()
            }
        }

        fun popLastBaseForScript(): LatexNode? {
            if (textBuffer.isNotEmpty()) {
                // Pop the last math token or character from textBuffer as the base of ^ or _
                val str = textBuffer.toString()
                var baseStart = str.length - 1
                // If it ends with a trig/log word like "sin", "cos", "tan", "log", pop the whole word
                val wordMatch = Regex("""(arcsin|arccos|arctan|cosec|sin|cos|tan|cot|sec|csc|log|ln|exp|[a-zA-Z0-9θπΦαβγΔΩ]+)$""").find(str)
                if (wordMatch != null) {
                    baseStart = wordMatch.range.first
                }
                val prefix = str.substring(0, baseStart)
                val baseStr = str.substring(baseStart)
                textBuffer.clear()
                if (prefix.isNotEmpty()) {
                    result.add(LatexNode.TextRun(prefix))
                }
                return LatexNode.TextRun(baseStr)
            }
            if (result.isNotEmpty()) {
                return result.removeAt(result.lastIndex)
            }
            return null
        }

        while (i < len) {
            // 1. Check Matrix environments: \begin{pmatrix} ... \end{pmatrix}, etc.
            if (s.startsWith("\\begin{", i)) {
                val envExtracted = extractBalancedBraces(s, i + 6)
                if (envExtracted != null) {
                    val envName = envExtracted.first.trim()
                    val endTag = "\\end{$envName}"
                    val endIdx = s.indexOf(endTag, envExtracted.second)
                    if (endIdx != -1) {
                        flushText()
                        val body = s.substring(envExtracted.second, endIdx).trim()
                        val rowStrings = body.split("\\\\").map { it.trim() }.filter { it.isNotEmpty() }
                        val parsedRows = rowStrings.map { rowStr ->
                            rowStr.split("&").map { cellStr ->
                                parseNodesInternal(cellStr.trim())
                            }
                        }
                        val (leftD, rightD) = when (envName) {
                            "pmatrix" -> "(" to ")"
                            "bmatrix" -> "[" to "]"
                            "Bmatrix" -> "{" to "}"
                            "vmatrix" -> "|" to "|"
                            "Vmatrix" -> "‖" to "‖"
                            else -> "" to ""
                        }
                        result.add(LatexNode.MatrixNode(parsedRows, leftD, rightD))
                        i = endIdx + endTag.length
                        continue
                    }
                }
            }

            // 2. Check \left ... \right
            if (s.startsWith("\\left", i) && (i + 5 >= len || !s[i + 5].isLetter())) {
                val leftRes = readDelimiterToken(s, i + 5)
                if (leftRes != null) {
                    val rightMatch = findMatchingRightDelimiter(s, leftRes.second)
                    if (rightMatch != null) {
                        flushText()
                        val innerStr = s.substring(leftRes.second, rightMatch.first)
                        result.add(
                            LatexNode.Delimited(
                                left = leftRes.first,
                                right = rightMatch.second,
                                inner = parseNodesInternal(innerStr)
                            )
                        )
                        i = rightMatch.third
                        continue
                    }
                }
            }

            // 3. Check Fractions: \frac{a}{b}, \dfrac{a}{b}, \tfrac{a}{b}, \cfrac{a}{b}
            val fracCmd = listOf("\\dfrac", "\\tfrac", "\\cfrac", "\\frac").firstOrNull { cmd ->
                s.startsWith(cmd, i) && (i + cmd.length >= len || !s[i + cmd.length].isLetter())
            }
            if (fracCmd != null) {
                var cursor = i + fracCmd.length
                while (cursor < len && s[cursor] == ' ') cursor++
                val numExtracted = readLatexArgument(s, cursor)
                if (numExtracted != null) {
                    var denCursor = numExtracted.second
                    while (denCursor < len && s[denCursor] == ' ') denCursor++
                    val denExtracted = readLatexArgument(s, denCursor)
                    if (denExtracted != null) {
                        flushText()
                        result.add(
                            LatexNode.Fraction(
                                numerator = parseNodesInternal(numExtracted.first.trim()),
                                denominator = parseNodesInternal(denExtracted.first.trim())
                            )
                        )
                        i = denExtracted.second
                        continue
                    }
                }
            }

            // 4. Check Binomials: \binom{n}{k}, \dbinom{n}{k}, \tbinom{n}{k}
            val binomCmd = listOf("\\dbinom", "\\tbinom", "\\binom").firstOrNull { cmd ->
                s.startsWith(cmd, i) && (i + cmd.length >= len || !s[i + cmd.length].isLetter())
            }
            if (binomCmd != null) {
                var cursor = i + binomCmd.length
                while (cursor < len && s[cursor] == ' ') cursor++
                val topExtracted = readLatexArgument(s, cursor)
                if (topExtracted != null) {
                    var botCursor = topExtracted.second
                    while (botCursor < len && s[botCursor] == ' ') botCursor++
                    val botExtracted = readLatexArgument(s, botCursor)
                    if (botExtracted != null) {
                        flushText()
                        result.add(
                            LatexNode.Binomial(
                                top = parseNodesInternal(topExtracted.first.trim()),
                                bottom = parseNodesInternal(botExtracted.first.trim())
                            )
                        )
                        i = botExtracted.second
                        continue
                    }
                }
            }

            // 5. Check Roots: \sqrt[n]{x} or \sqrt{x}
            if (s.startsWith("\\sqrt", i) && (i + 5 >= len || !s[i + 5].isLetter())) {
                var cursor = i + 5
                while (cursor < len && s[cursor] == ' ') cursor++
                var degreeNodes: List<LatexNode>? = null
                if (cursor < len && s[cursor] == '[') {
                    val closeBracket = s.indexOf(']', cursor + 1)
                    if (closeBracket != -1) {
                        val degStr = s.substring(cursor + 1, closeBracket).trim()
                        if (degStr.isNotEmpty() && degStr != "2") {
                            degreeNodes = parseNodesInternal(degStr)
                        }
                        cursor = closeBracket + 1
                        while (cursor < len && s[cursor] == ' ') cursor++
                    }
                }
                val radExtracted = readLatexArgument(s, cursor)
                if (radExtracted != null) {
                    flushText()
                    result.add(
                        LatexNode.Sqrt(
                            degree = degreeNodes,
                            radicand = parseNodesInternal(radExtracted.first.trim())
                        )
                    )
                    i = radExtracted.second
                    continue
                }
            }

            // Also support Unicode √(...) or ∛(...) with vinculum bar when followed by (...) or {...}
            if ((s[i] == '√' || s[i] == '∛' || s[i] == '∜') && i + 1 < len && (s[i + 1] == '(' || s[i + 1] == '{')) {
                val deg = when (s[i]) {
                    '∛' -> listOf(LatexNode.TextRun("3"))
                    '∜' -> listOf(LatexNode.TextRun("4"))
                    else -> null
                }
                val extracted = if (s[i + 1] == '{') extractBalancedBraces(s, i + 1) else extractBalancedParens(s, i + 1)
                if (extracted != null) {
                    flushText()
                    result.add(
                        LatexNode.Sqrt(
                            degree = deg,
                            radicand = parseNodesInternal(extracted.first.trim())
                        )
                    )
                    i = extracted.second
                    continue
                }
            }

            // 6. Check Overline / Vector / Bar / Hat: \overline{...}, \bar{...}, \vec{...}, \overrightarrow{...}, \hat{...}
            val overCmd = listOf(
                "\\overrightarrow" to OverlineKind.VECTOR,
                "\\overline" to OverlineKind.BAR,
                "\\bar" to OverlineKind.BAR,
                "\\vec" to OverlineKind.VECTOR,
                "\\hat" to OverlineKind.HAT
            ).firstOrNull { (cmd, _) ->
                s.startsWith(cmd, i) && (i + cmd.length >= len || !s[i + cmd.length].isLetter())
            }
            if (overCmd != null) {
                var cursor = i + overCmd.first.length
                while (cursor < len && s[cursor] == ' ') cursor++
                val argExtracted = readLatexArgument(s, cursor)
                if (argExtracted != null) {
                    flushText()
                    result.add(
                        LatexNode.Overline(
                            content = parseNodesInternal(argExtracted.first.trim()),
                            kind = overCmd.second
                        )
                    )
                    i = argExtracted.second
                    continue
                }
            }

            // 7. Check Big Operators: \lim, \sum, \prod, \int, \oint
            val bigOpMatch = listOf(
                "\\lim" to ("lim" to true),
                "\\sum" to ("∑" to true),
                "\\prod" to ("∏" to true),
                "\\oint" to ("∮" to false),
                "\\int" to ("∫" to false)
            ).firstOrNull { (cmd, _) ->
                s.startsWith(cmd, i) && (i + cmd.length >= len || !s[i + cmd.length].isLetter())
            }
            if (bigOpMatch != null) {
                var cursor = i + bigOpMatch.first.length
                var lowerNodes: List<LatexNode>? = null
                var upperNodes: List<LatexNode>? = null

                // Read optional _{...} and ^{...} in any order
                repeat(2) {
                    while (cursor < len && s[cursor] == ' ') cursor++
                    if (cursor < len && s[cursor] == '_' && cursor + 1 < len) {
                        val subArg = readScriptArgument(s, cursor + 1)
                        if (subArg != null) {
                            lowerNodes = parseNodesInternal(subArg.first)
                            cursor = subArg.second
                        }
                    } else if (cursor < len && s[cursor] == '^' && cursor + 1 < len) {
                        val supArg = readScriptArgument(s, cursor + 1)
                        if (supArg != null) {
                            upperNodes = parseNodesInternal(supArg.first)
                            cursor = supArg.second
                        }
                    }
                }

                flushText()
                result.add(
                    LatexNode.BigOp(
                        symbol = bigOpMatch.second.first,
                        lower = lowerNodes,
                        upper = upperNodes,
                        limitsUnderOver = bigOpMatch.second.second
                    )
                )
                i = cursor
                continue
            }

            // 8. Check Bold / Italic commands: \textbf{...}, \mathbf{...}, \textit{...}, \mathit{...}
            val boldCmd = listOf("\\textbf", "\\mathbf", "\\boldsymbol").firstOrNull { cmd ->
                s.startsWith(cmd, i) && (i + cmd.length >= len || !s[i + cmd.length].isLetter())
            }
            if (boldCmd != null) {
                var cursor = i + boldCmd.length
                while (cursor < len && s[cursor] == ' ') cursor++
                val extracted = extractBalancedBraces(s, cursor)
                if (extracted != null) {
                    flushText()
                    result.add(LatexNode.TextRun(toPlainMathText(extracted.first), bold = true))
                    i = extracted.second
                    continue
                }
            }

            // 9. Check Markdown Bold **...** and Inline Code `...` (kept for backward compatibility)
            if (i + 1 < len && s[i] == '*' && s[i + 1] == '*') {
                val closeIdx = s.indexOf("**", i + 2)
                if (closeIdx != -1 && closeIdx > i + 2) {
                    flushText()
                    result.add(LatexNode.TextRun(s.substring(i + 2, closeIdx), bold = true))
                    i = closeIdx + 2
                    continue
                }
            }
            if (s[i] == '`') {
                val closeIdx = s.indexOf('`', i + 1)
                if (closeIdx != -1 && closeIdx > i + 1) {
                    flushText()
                    result.add(LatexNode.TextRun(s.substring(i + 1, closeIdx), monospace = true))
                    i = closeIdx + 1
                    continue
                }
            }

            // 10. Check Superscripts (^) and Subscripts (_)
            if ((s[i] == '^' || s[i] == '_') && i + 1 < len) {
                val isFirstSup = s[i] == '^'
                val firstArg = readScriptArgument(s, i + 1)
                if (firstArg != null) {
                    var supNodes: List<LatexNode>? = if (isFirstSup) parseNodesInternal(firstArg.first) else null
                    var subNodes: List<LatexNode>? = if (!isFirstSup) parseNodesInternal(firstArg.first) else null
                    var nextCursor = firstArg.second

                    // Check if immediately followed by the complementary script (e.g., x_1^2 or x^2_1)
                    if (nextCursor + 1 < len) {
                        val secondChar = s[nextCursor]
                        if ((isFirstSup && secondChar == '_') || (!isFirstSup && secondChar == '^')) {
                            val secondArg = readScriptArgument(s, nextCursor + 1)
                            if (secondArg != null) {
                                if (secondChar == '^') {
                                    supNodes = parseNodesInternal(secondArg.first)
                                } else {
                                    subNodes = parseNodesInternal(secondArg.first)
                                }
                                nextCursor = secondArg.second
                            }
                        }
                    }

                    val baseNode = popLastBaseForScript()
                    result.add(
                        LatexNode.Script(
                            base = baseNode,
                            superscript = supNodes,
                            subscript = subNodes
                        )
                    )
                    i = nextCursor
                    continue
                }
            }

            // 11. Strip stray curly braces { ... } used for grouping in LaTeX
            if (s[i] == '{') {
                val extracted = extractBalancedBraces(s, i)
                if (extracted != null) {
                    flushText()
                    val innerNodes = parseNodesInternal(extracted.first)
                    if (innerNodes.size == 1) {
                        result.add(innerNodes[0])
                    } else if (innerNodes.isNotEmpty()) {
                        result.add(LatexNode.Delimited("", "", innerNodes))
                    }
                    i = extracted.second
                    continue
                }
            }

            // 12. Strip unknown LaTeX backslash commands gracefully (e.g. \displaystyle, \textstyle, \limits, \nolimits)
            if (s[i] == '\\' && i + 1 < len && s[i + 1].isLetter()) {
                var cmdEnd = i + 1
                while (cmdEnd < len && s[cmdEnd].isLetter()) cmdEnd++
                val unknownCmd = s.substring(i + 1, cmdEnd)
                if (unknownCmd in setOf("displaystyle", "textstyle", "scriptstyle", "limits", "nolimits", "left", "right", "big", "Big", "bigg", "Bigg", "rm", "it", "bf")) {
                    i = cmdEnd
                    if (i < len && s[i] == ' ') i++
                    continue
                } else {
                    textBuffer.append(unknownCmd)
                    i = cmdEnd
                    continue
                }
            }

            textBuffer.append(s[i])
            i++
        }

        flushText()
        return result
    }

    private fun readLatexArgument(s: String, startIdx: Int): Pair<String, Int>? {
        if (startIdx >= s.length) return null
        if (s[startIdx] == '{') {
            return extractBalancedBraces(s, startIdx)
        }
        // Single token without braces, e.g. \frac 1 2 or \sqrt 2 or \vec a
        if (s[startIdx] == '\\' && startIdx + 1 < s.length) {
            var end = startIdx + 1
            while (end < s.length && s[end].isLetter()) end++
            return Pair(s.substring(startIdx, end), end)
        }
        return Pair(s[startIdx].toString(), startIdx + 1)
    }

    private fun readScriptArgument(s: String, startIdx: Int): Pair<String, Int>? {
        if (startIdx >= s.length) return null
        when (s[startIdx]) {
            '{' -> return extractBalancedBraces(s, startIdx)
            '(' -> return extractBalancedParens(s, startIdx)
            '\\' -> {
                var end = startIdx + 1
                while (end < s.length && s[end].isLetter()) end++
                if (end > startIdx + 1) {
                    return Pair(s.substring(startIdx, end), end)
                }
            }
        }
        val tokenEnd = readSingleScriptToken(s, startIdx)
        if (tokenEnd > startIdx) {
            return Pair(s.substring(startIdx, tokenEnd), tokenEnd)
        }
        return null
    }

    private fun readDelimiterToken(s: String, startIdx: Int): Pair<String, Int>? {
        var i = startIdx
        while (i < s.length && s[i] == ' ') i++
        if (i >= s.length) return null
        if (s.startsWith("\\{", i)) return "{" to (i + 2)
        if (s.startsWith("\\}", i)) return "}" to (i + 2)
        if (s.startsWith("\\|", i)) return "‖" to (i + 2)
        if (s.startsWith("\\langle", i)) return "⟨" to (i + 7)
        if (s.startsWith("\\rangle", i)) return "⟩" to (i + 7)
        val ch = s[i]
        if (ch in "()[]{}|.") {
            return ch.toString() to (i + 1)
        }
        return null
    }

    private fun findMatchingRightDelimiter(s: String, searchStart: Int): Triple<Int, String, Int>? {
        var depth = 1
        var i = searchStart
        while (i < s.length) {
            if (s.startsWith("\\left", i) && (i + 5 >= s.length || !s[i + 5].isLetter())) {
                val leftTok = readDelimiterToken(s, i + 5)
                if (leftTok != null) {
                    depth++
                    i = leftTok.second
                    continue
                }
            }
            if (s.startsWith("\\right", i) && (i + 6 >= s.length || !s[i + 6].isLetter())) {
                val rightTok = readDelimiterToken(s, i + 6)
                if (rightTok != null) {
                    depth--
                    if (depth == 0) {
                        return Triple(i, rightTok.first, rightTok.second)
                    }
                    i = rightTok.second
                    continue
                }
            }
            i++
        }
        return null
    }

    /**
     * Converts inline math slash fractions like "1 / sin θ", "sin θ / cos θ", "√3 / 2", "(a+b)/(c+d)"
     * into `\frac{num}{den}` so the LaTeX AST parser turns them into [LatexNode.Fraction].
     */
    private fun convertSlashFractionsToLatex(text: String): String {
        if (text.contains("://") || Regex("""\d+\s*/\s*\d+\s*/\s*\d+""").containsMatchIn(text)) {
            return text
        }

        val slashCount = text.count { it == '/' }
        if (slashCount == 1 && !text.contains("^{") && !text.contains("_{") && !text.contains("^(") && !text.contains("_(")) {
            val slashIdx = text.indexOf('/')
            val leftPart = text.substring(0, slashIdx).trim()
            val rightPart = text.substring(slashIdx + 1).trim()
            if (isMathFractionOperand(leftPart) && isMathFractionOperand(rightPart)) {
                val cleanLeft = stripOuterParens(leftPart)
                val cleanRight = stripOuterParens(rightPart)
                return "\\frac{$cleanLeft}{$cleanRight}"
            }
        }

        val mathTermPattern = """(?:\([^()]+\)|(?:cosec|arcsin|arccos|arctan|sin|cos|tan|cot|sec|csc|log|ln)(?:\^[0-9+\-{}()a-zA-Z]+|[²³⁴])?\s*[a-zA-Z0-9θπΦαβγΔ°]+|[0-9√∛∜πθαβγΔΩ°²³⁴⁵⁶⁷⁸⁹⁺⁻₀₁₂₃₄₅₆₇₈₉a-zA-Z^_{}()+-]+)"""
        val embeddedFractionRegex = Regex("""($mathTermPattern)\s*/\s*($mathTermPattern)""")

        return embeddedFractionRegex.replace(text) { match ->
            // Do not convert a slash that sits inside ^{...}, _{...}, ^(...), or _(...)
            val matchStart = match.range.first
            val prefixBeforeMatch = text.substring(0, matchStart)
            val insideBraces = prefixBeforeMatch.count { it == '{' } > prefixBeforeMatch.count { it == '}' }
            val insideScriptParens = Regex("""[\^_]\([^()]*$""").containsMatchIn(prefixBeforeMatch)
            if (insideBraces || insideScriptParens) {
                return@replace match.value
            }

            val left = match.groupValues[1].trim()
            val right = match.groupValues[2].trim()
            if (isMathFractionOperand(left) && isMathFractionOperand(right)) {
                val cleanLeft = stripOuterParens(left)
                val cleanRight = stripOuterParens(right)
                "\\frac{$cleanLeft}{$cleanRight}"
            } else {
                match.value
            }
        }
    }

    private fun stripOuterParens(s: String): String {
        val trimmed = s.trim()
        if (trimmed.length >= 3 && trimmed.first() == '(' && trimmed.last() == ')') {
            var depth = 0
            for (i in 0 until trimmed.length - 1) {
                if (trimmed[i] == '(') depth++
                else if (trimmed[i] == ')') depth--
                if (depth == 0) return trimmed
            }
            return trimmed.substring(1, trimmed.length - 1).trim()
        }
        return trimmed
    }

    private fun isMathFractionOperand(part: String): Boolean {
        if (part.isEmpty() || part.length > 28) return false
        if (part.any { it in '\u0900'..'\u097F' }) return false

        val unwrapped = stripOuterParens(part)
        if (unwrapped.isEmpty()) return false

        val lower = unwrapped.lowercase()
        if (MATH_FUNCTIONS.any { fn ->
                lower == fn ||
                    lower.startsWith("$fn ") ||
                    lower.startsWith("$fn^") ||
                    lower.startsWith("${fn}²") ||
                    lower.startsWith("$fn(") ||
                    lower.startsWith("${fn}θ") ||
                    lower.startsWith("$fn\\")
            }
        ) {
            return true
        }

        val hasMathSymbolOrDigit = unwrapped.any { ch ->
            ch.isDigit() || ch in "√∛∜πθαβγδΔλμσωΩφ°^_+-±×·²³⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉\\"
        }
        if (hasMathSymbolOrDigit) {
            val words = Regex("""[a-zA-Z]+""").findAll(unwrapped).map { it.value.lowercase() }.toList()
            if (words.all { it.length <= 3 || it in MATH_FUNCTIONS }) {
                return true
            }
        }

        if (unwrapped.all { it.isLetter() } && unwrapped.length <= 3) {
            return true
        }

        return false
    }

    fun nodesToAnnotatedString(nodes: List<LatexNode>): AnnotatedString {
        return buildAnnotatedString {
            appendNodesToBuilder(nodes)
        }
    }

    private fun AnnotatedString.Builder.appendNodesToBuilder(nodes: List<LatexNode>) {
        for (node in nodes) {
            when (node) {
                is LatexNode.TextRun -> {
                    val style = SpanStyle(
                        fontWeight = if (node.bold) FontWeight.ExtraBold else null,
                        fontStyle = if (node.italic) FontStyle.Italic else null,
                        fontFamily = if (node.monospace) FontFamily.Monospace else null
                    )
                    withStyle(style) {
                        append(node.text)
                    }
                }
                is LatexNode.Script -> {
                    if (node.base != null) {
                        appendNodesToBuilder(listOf(node.base))
                    }
                    if (!node.subscript.isNullOrEmpty()) {
                        withStyle(
                            SpanStyle(
                                baselineShift = BaselineShift.Subscript,
                                fontSize = 0.75.em,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(nodesToPlainString(node.subscript))
                        }
                    }
                    if (!node.superscript.isNullOrEmpty()) {
                        withStyle(
                            SpanStyle(
                                baselineShift = BaselineShift.Superscript,
                                fontSize = 0.75.em,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(nodesToPlainString(node.superscript))
                        }
                    }
                }
                is LatexNode.Delimited -> {
                    if (node.left != ".") append(node.left)
                    appendNodesToBuilder(node.inner)
                    if (node.right != ".") append(node.right)
                }
                is LatexNode.Fraction -> {
                    val numStr = nodesToPlainString(node.numerator)
                    val denStr = nodesToPlainString(node.denominator)
                    append("$numStr/$denStr")
                }
                is LatexNode.Binomial -> {
                    val topStr = nodesToPlainString(node.top)
                    val botStr = nodesToPlainString(node.bottom)
                    append("C($topStr, $botStr)")
                }
                is LatexNode.Sqrt -> {
                    if (!node.degree.isNullOrEmpty()) {
                        val deg = nodesToPlainString(node.degree)
                        when (deg) {
                            "3" -> append("∛(")
                            "4" -> append("∜(")
                            else -> append("^($deg)√(")
                        }
                    } else {
                        append("√(")
                    }
                    appendNodesToBuilder(node.radicand)
                    append(")")
                }
                is LatexNode.Overline -> {
                    appendNodesToBuilder(node.content)
                    when (node.kind) {
                        OverlineKind.BAR -> append("\u0304")
                        OverlineKind.VECTOR -> append("\u20D7")
                        OverlineKind.HAT -> append("\u0302")
                    }
                }
                is LatexNode.BigOp -> {
                    append(node.symbol)
                    if (!node.lower.isNullOrEmpty()) {
                        withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.72.em)) {
                            append(nodesToPlainString(node.lower))
                        }
                    }
                    if (!node.upper.isNullOrEmpty()) {
                        withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.72.em)) {
                            append(nodesToPlainString(node.upper))
                        }
                    }
                    append(" ")
                }
                is LatexNode.MatrixNode -> {
                    append(node.leftDelim)
                    node.rows.forEachIndexed { rIdx, row ->
                        if (rIdx > 0) append("; ")
                        row.forEachIndexed { cIdx, cell ->
                            if (cIdx > 0) append(", ")
                            appendNodesToBuilder(cell)
                        }
                    }
                    append(node.rightDelim)
                }
            }
        }
    }

    private fun nodesToPlainString(nodes: List<LatexNode>): String {
        val sb = StringBuilder()
        for (node in nodes) {
            when (node) {
                is LatexNode.TextRun -> sb.append(node.text)
                is LatexNode.Script -> {
                    if (node.base != null) sb.append(nodesToPlainString(listOf(node.base)))
                    if (!node.subscript.isNullOrEmpty()) {
                        val sub = nodesToPlainString(node.subscript)
                        val uni = sub.map { SUBSCRIPT_MAP[it] }.takeIf { l -> l.all { it != null } }?.joinToString("")
                        sb.append(uni ?: "_($sub)")
                    }
                    if (!node.superscript.isNullOrEmpty()) {
                        val sup = nodesToPlainString(node.superscript)
                        val uni = sup.map { SUPERSCRIPT_MAP[it] }.takeIf { l -> l.all { it != null } }?.joinToString("")
                        sb.append(uni ?: "^($sup)")
                    }
                }
                is LatexNode.Fraction -> {
                    val num = nodesToPlainString(node.numerator)
                    val den = nodesToPlainString(node.denominator)
                    val wrapNum = if (num.any { it == '+' || it == '-' || it == ' ' }) "($num)" else num
                    val wrapDen = if (den.any { it == '+' || it == '-' || it == ' ' }) "($den)" else den
                    sb.append("$wrapNum/$wrapDen")
                }
                is LatexNode.Binomial -> {
                    sb.append("(${nodesToPlainString(node.top)} choose ${nodesToPlainString(node.bottom)})")
                }
                is LatexNode.Sqrt -> {
                    val rad = nodesToPlainString(node.radicand)
                    val deg = node.degree?.let { nodesToPlainString(it) }
                    val prefix = when (deg) {
                        null, "", "2" -> "√"
                        "3" -> "∛"
                        "4" -> "∜"
                        else -> "^($deg)√"
                    }
                    if (rad.length <= 2 && rad.all { it.isLetterOrDigit() }) {
                        sb.append("$prefix$rad")
                    } else {
                        sb.append("$prefix($rad)")
                    }
                }
                is LatexNode.Overline -> {
                    sb.append(nodesToPlainString(node.content))
                }
                is LatexNode.Delimited -> {
                    if (node.left != ".") sb.append(node.left)
                    sb.append(nodesToPlainString(node.inner))
                    if (node.right != ".") sb.append(node.right)
                }
                is LatexNode.BigOp -> {
                    sb.append(node.symbol)
                    if (!node.lower.isNullOrEmpty()) {
                        sb.append("_(${nodesToPlainString(node.lower)})")
                    }
                    if (!node.upper.isNullOrEmpty()) {
                        sb.append("^(${nodesToPlainString(node.upper)})")
                    }
                    sb.append(" ")
                }
                is LatexNode.MatrixNode -> {
                    sb.append(node.leftDelim)
                    sb.append(
                        node.rows.joinToString("; ") { row ->
                            row.joinToString(", ") { cell -> nodesToPlainString(cell) }
                        }
                    )
                    sb.append(node.rightDelim)
                }
            }
        }
        return sb.toString()
    }

    /**
     * Converts a raw string with LaTeX into a single-line [AnnotatedString].
     */
    fun formatMathAndMarkdown(
        rawText: String,
        codeBackground: Color = Color.Unspecified
    ): AnnotatedString {
        if (rawText.isEmpty()) return AnnotatedString("")
        val nodes = parseLatexNodes(rawText)
        return nodesToAnnotatedString(nodes)
    }

    /**
     * Converts LaTeX into clean Unicode plain text (with Unicode superscripts/subscripts)
     * for PDF Question Paper rendering and Text-to-Speech.
     */
    fun toPlainMathText(rawText: String): String {
        if (rawText.isEmpty()) return ""
        val nodes = parseLatexNodes(rawText)
        return nodesToPlainString(nodes)
    }

    private fun readSingleScriptToken(text: String, startIdx: Int): Int {
        if (startIdx >= text.length) return startIdx
        var idx = startIdx
        if (text[idx] == '+' || text[idx] == '-') {
            idx++
        }
        if (idx < text.length && text[idx].isDigit()) {
            while (idx < text.length && text[idx].isDigit()) {
                idx++
            }
            return idx
        }
        if (idx < text.length && text[idx].isLetter()) {
            return idx + 1
        }
        return startIdx
    }

    private fun replaceSingleArgCommand(
        text: String,
        commands: List<String>,
        transform: (String) -> String
    ): String {
        var current = text
        for (cmd in commands) {
            var searchIdx = 0
            while (true) {
                val foundIdx = current.indexOf(cmd, searchIdx)
                if (foundIdx == -1) break
                val afterCmd = foundIdx + cmd.length
                if (afterCmd < current.length && current[afterCmd].isLetter()) {
                    searchIdx = afterCmd
                    continue
                }
                var braceStart = afterCmd
                while (braceStart < current.length && current[braceStart] == ' ') braceStart++
                if (braceStart < current.length && current[braceStart] == '{') {
                    val extracted = extractBalancedBraces(current, braceStart)
                    if (extracted != null) {
                        val replacement = transform(extracted.first)
                        current = current.substring(0, foundIdx) + replacement + current.substring(extracted.second)
                        searchIdx = foundIdx + replacement.length
                        continue
                    }
                }
                searchIdx = afterCmd
            }
        }
        return current
    }

    private fun extractBalancedBraces(text: String, openBraceIdx: Int): Pair<String, Int>? {
        if (openBraceIdx >= text.length || text[openBraceIdx] != '{') return null
        var depth = 0
        var i = openBraceIdx
        while (i < text.length) {
            val c = text[i]
            if (c == '{') {
                depth++
            } else if (c == '}') {
                depth--
                if (depth == 0) {
                    return Pair(text.substring(openBraceIdx + 1, i), i + 1)
                }
            }
            i++
        }
        return null
    }

    private fun extractBalancedParens(text: String, openParenIdx: Int): Pair<String, Int>? {
        if (openParenIdx >= text.length || text[openParenIdx] != '(') return null
        var depth = 0
        var i = openParenIdx
        while (i < text.length) {
            val c = text[i]
            if (c == '(') {
                depth++
            } else if (c == ')') {
                depth--
                if (depth == 0) {
                    return Pair(text.substring(openParenIdx + 1, i), i + 1)
                }
            }
            i++
        }
        return null
    }
}
