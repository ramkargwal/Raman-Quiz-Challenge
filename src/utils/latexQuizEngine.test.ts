import test from 'node:test';
import assert from 'node:assert/strict';
import {
  parseQuizText,
  safeParseQuiz,
  renderMixedLatexToHtml,
  latexToSpokenText,
  prepareQuizQuestions,
  getGrade,
  QUIZ_AI_PROMPT,
  buildAiPrompt,
  exportQuizzesToEditableText,
  importQuizzesFromText,
  upsertQuizInList,
  resolveAnswerIndex,
  formatExamTime,
  evaluateExamSubmission,
} from './latexQuizEngine';
import { buildOfflineQuizHtml } from './offlineExporter';

test('parseQuizText parses complex LaTeX with both single and double backslashes and nested braces', () => {
  const rawInput = `
    { q: "यदि $\\frac{a}{b} = \\sqrt{\\tan\\theta} + \\beta$ है, तो $\\int_{0}^{\\pi} \\sin(x) dx$ क्या है?", o: ["$\\frac{\\pi}{2}$", "$2$", "$\\sqrt{3}$", "$0$"], a: 1 },
    { q: 'Matrix $A = \\begin{pmatrix} 1 & 2 \\\\ 3 & 4 \\end{pmatrix}$ का trace क्या है?', o: ['5', '4', '3', '2'], a: 0 },
  `;

  const { validQuestions, skippedQuestions } = parseQuizText(rawInput);
  assert.equal(skippedQuestions, 0);
  assert.equal(validQuestions.length, 2);
  assert.ok(validQuestions[0].q.includes('\\frac{a}{b}'));
  assert.ok(validQuestions[0].q.includes('\\tan\\theta'));
  assert.ok(validQuestions[0].q.includes('\\beta'));
  assert.equal(validQuestions[0].o[0], '$\\frac{\\pi}{2}$');
  assert.ok(validQuestions[1].q.includes('\\begin{pmatrix}'));
});

test('exportQuizzesToEditableText and importQuizzesFromText roundtrip multi-quiz bundles and Starred Questions Bank', () => {
  const quizzes = upsertQuizInList(
    [],
    'Trigonometry Test',
    'Math',
    '{ q: "$\\sin^2\\theta + \\cos^2\\theta$ का मान क्या है?", o: ["1", "0", "2", "-1"], a: 0 },'
  );
  const moreQuizzes = upsertQuizInList(
    quizzes,
    'Physics Wave Quiz',
    'Science',
    '{ q: "तरंग समीकरण क्या है?", o: ["$v = f\\lambda$", "$F = ma$", "$E = mc^2$", "$P = VI$"], a: 0 },'
  );
  const starred = [
    {
      q: 'Important Starred Q: $\\int_0^1 x\\,dx$',
      o: ['$\\frac{1}{2}$', '$1$', '$0$', '$2$'],
      a: 0,
    },
  ];

  const exportedText = exportQuizzesToEditableText(moreQuizzes, starred);
  assert.ok(exportedText.includes('=== QUIZ: Physics Wave Quiz ==='));
  assert.ok(exportedText.includes('CATEGORY: Science'));
  assert.ok(exportedText.includes('=== QUIZ: Trigonometry Test ==='));
  assert.ok(exportedText.includes('CATEGORY: Math'));
  assert.ok(exportedText.includes('=== QUIZ: Starred Questions Bank ==='));
  assert.ok(exportedText.includes('CATEGORY: __STARRED_BANK__'));

  const imported = importQuizzesFromText(exportedText, [], []);
  assert.equal(imported.importedCount, 3);
  assert.equal(imported.quizzes.length, 2);
  assert.equal(imported.starredQuestions.length, 1);
  assert.equal(imported.starredQuestions[0].q, 'Important Starred Q: $\\int_0^1 x\\,dx$');
});

test('resolveAnswerIndex handles 0-based, 1-based, letter A-D, and text answer matches', () => {
  const opts = ['Alpha', 'Beta', 'Gamma', 'Delta'];
  assert.equal(resolveAnswerIndex('B', opts), 1);
  assert.equal(resolveAnswerIndex('Gamma', opts), 2);
  assert.equal(resolveAnswerIndex(4, opts), 3);
  assert.equal(resolveAnswerIndex('2', opts), 2);
});

test('renderMixedLatexToHtml renders inline $...$, display $$...$$, \\(...\\), \\[...\\], matrices, and naked LaTeX', () => {
  const inlineHtml = renderMixedLatexToHtml('द्विघात सूत्र: $x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}$');
  assert.ok(inlineHtml.includes('katex'));
  assert.ok(inlineHtml.includes('द्विघात सूत्र:'));

  const displayHtml = renderMixedLatexToHtml('समाकलन: $$\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}$$');
  assert.ok(displayHtml.includes('katex-display'));

  const bracketHtml = renderMixedLatexToHtml('\\[ \\sum_{n=1}^{\\infty} \\frac{1}{n^2} = \\frac{\\pi^2}{6} \\]');
  assert.ok(bracketHtml.includes('katex-display'));

  const parenHtml = renderMixedLatexToHtml('ऊर्जा समीकरण \\( E = mc^2 \\) है।');
  assert.ok(parenHtml.includes('katex'));

  const nakedHtml = renderMixedLatexToHtml('\\frac{\\pi}{4} + \\sqrt{2}');
  assert.ok(nakedHtml.includes('katex'));
});

test('renderMixedLatexToHtml repairs accidental JS control characters like \\f (form feed) and \\t (tab)', () => {
  const broken = 'मान ज्ञात करें: $\x0crac{\\sin\x09heta}{\x08eta}$';
  const html = renderMixedLatexToHtml(broken);
  assert.ok(html.includes('katex'));
  assert.ok(!html.includes('\x0c'));
});

test('QUIZ_AI_PROMPT and buildAiPrompt include explicit LaTeX formatting, topic, count, difficulty, and language instructions', () => {
  assert.ok(QUIZ_AI_PROMPT.includes('LaTeX'));
  assert.ok(QUIZ_AI_PROMPT.includes('\\frac'));
  assert.ok(QUIZ_AI_PROMPT.includes('\\sqrt'));
  assert.ok(QUIZ_AI_PROMPT.includes('50 UNIQUE'));

  const customPrompt = buildAiPrompt({
    topicOrNotes: 'Trigonometry & Inverse Trig',
    questionCount: 30,
    difficulty: 'Exam',
    languageStyle: 'English',
  });
  assert.ok(customPrompt.includes('30 UNIQUE'));
  assert.ok(customPrompt.includes('Competitive Exam Level'));
  assert.ok(customPrompt.includes('Language Rules (English)'));
  assert.ok(customPrompt.includes('Trigonometry & Inverse Trig'));
});

test('latexToSpokenText converts LaTeX formulas into readable speech text', () => {
  const spoken = latexToSpokenText('यदि $\\frac{a}{b} = \\sqrt{x}$ है');
  assert.ok(spoken.includes('a over b'));
  assert.ok(spoken.includes('square root of x'));
});

test('prepareQuizQuestions shuffles options and preserves correct answer mapping', () => {
  const src = [{ q: 'Test?', o: ['Correct', 'W1', 'W2', 'W3'], a: 0 }];
  const prepared = prepareQuizQuestions(src);
  assert.equal(prepared.length, 1);
  assert.equal(prepared[0].o[prepared[0].a], 'Correct');
});

test('getGrade returns expected grades for accuracy brackets', () => {
  assert.equal(getGrade(95).grade, 'A+');
  assert.equal(getGrade(82).grade, 'A');
  assert.equal(getGrade(75).grade, 'B');
  assert.equal(getGrade(65).grade, 'C');
  assert.equal(getGrade(45).grade, 'D');
  assert.equal(getGrade(20).grade, 'F');
});

test('formatExamTime and evaluateExamSubmission compute CBT Exam Mode results accurately', () => {
  assert.equal(formatExamTime(90), '01:30');
  assert.equal(formatExamTime(5), '00:05');

  const questions = [
    { q: 'Q1', o: ['A', 'B', 'C', 'D'], a: 0 },
    { q: 'Q2', o: ['A', 'B', 'C', 'D'], a: 1 },
    { q: 'Q3', o: ['A', 'B', 'C', 'D'], a: 2 },
  ];
  const examAnswers: Record<number, number> = {
    0: 0, // Correct
    1: 3, // Wrong (selected index 3 instead of 1)
    // 2 is skipped
  };

  const result = evaluateExamSubmission(questions, examAnswers);
  assert.equal(result.score, 1);
  assert.equal(result.skipped, 1);
  assert.equal(result.wrongQuestions.length, 2);
  assert.equal(result.wrongQuestions[0].userAnswerIndex, 3);
  assert.equal(result.wrongQuestions[0]._timedOut, false);
  assert.equal(result.wrongQuestions[1]._timedOut, true);
});

test('safeParseQuiz returns empty array instead of throwing on malformed input and buildOfflineQuizHtml escapes non-math HTML', () => {
  assert.deepEqual(safeParseQuiz('{ invalid :: ?? '), []);
  const html = buildOfflineQuizHtml({
    id: 'test_1',
    name: 'HTML & Math <Test>',
    category: 'CS',
    data: '{ q: "Which tag creates a line break?", o: ["<br>", "<hr>", "<div>", "<p>"], a: 0 },',
    timestamp: 1000,
    questionCount: 1,
  });
  assert.ok(html.includes('HTML &amp; Math &lt;Test&gt;'));
  assert.ok(html.includes('escapeHtml(s.slice(lastIdx, match.index))'));
});


