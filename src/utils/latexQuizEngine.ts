import katex from 'katex';

export type QuizMode = 'PRACTICE' | 'EXAM';

export interface QuizQuestion {
  q: string;
  o: string[];
  a: number;
  _timedOut?: boolean;
  userAnswerIndex?: number | null;
}

export interface SavedQuiz {
  id: string;
  name: string;
  category: string;
  data: string;
  timestamp: number;
  questionCount: number;
}
export interface ParseQuizResult {
  validQuestions: QuizQuestion[];
  skippedQuestions: number;
}

export interface ImportQuizzesResult {
  quizzes: SavedQuiz[];
  starredQuestions: QuizQuestion[];
  importedCount: number;
}

export const DEFAULT_QUIZ_QUESTIONS: QuizQuestion[] = [
  {
    q: 'यह 📌 इमोजी किस काम के लिए इस्तेमाल किया जाता है?',
    o: [
      'मैसेज डिलीट करने के लिए',
      'मैसेज को पिन (Pin) करने के लिए',
      'लोकेशन भेजने के लिए',
      'सिलाई करने के लिए',
    ],
    a: 1,
  },
  {
    q: 'यह इमोजी ♻️ किस प्रक्रिया को दर्शाता है?',
    o: [
      'वीडियो प्ले करना',
      'रीसायकल या पुनर्चक्रण (Recycle)',
      'रास्ता भटकना',
      'डिलीट करना',
    ],
    a: 1,
  },
  {
    q: 'टेक्स्ट करते समय 🚫 इमोजी का क्या अर्थ होता है?',
    o: [
      'आगे बढ़ें',
      'निषेध या मनाही (Prohibited)',
      'पार्किंग',
      'खतरा',
    ],
    a: 1,
  },
];

export type PromptDifficulty = 'Easy' | 'Medium' | 'Hard' | 'Exam';
export type PromptLanguageStyle = 'Hinglish' | 'Hindi' | 'English';

export interface BuildAiPromptOptions {
  topicOrNotes?: string;
  questionCount?: number;
  difficulty?: PromptDifficulty | string;
  languageStyle?: PromptLanguageStyle | string;
}

/**
 * Generates the customizable AI Quiz Prompt with Topic, Question Count,
 * Difficulty Level, Language Style, and full LaTeX Math formatting instructions.
 */
export function buildAiPrompt(options: BuildAiPromptOptions = {}): string {
  const {
    topicOrNotes = '',
    questionCount = 50,
    difficulty = 'Medium',
    languageStyle = 'Hinglish',
  } = options;

  const safeCount = Math.max(1, Math.min(200, Math.round(questionCount || 50)));

  const difficultyDescription =
    difficulty === 'Easy'
      ? 'Easy (Foundational concepts, direct definitions, and basic formula recall)'
      : difficulty === 'Hard'
      ? 'Hard (Challenging multi-step analytical, numerical, and deep conceptual questions)'
      : difficulty === 'Exam'
      ? 'Competitive Exam Level (SSC, Railway, Banking, JEE/NEET, UPSC/State PCS style high-yield exam questions with close distractors)'
      : 'Medium (Standard conceptual and application-based questions suitable for school/board and competitive exams)';

  const languageRules =
    languageStyle === 'English'
      ? `* Write all questions and options in clear, accurate English.\n* Keep all technical terms, scientific names, programming syntax, HTML/code tags (e.g., <br>, <div>, <p>), units, and mathematical formulas in clean standard notation.`
      : languageStyle === 'Hindi'
      ? `* Write all questions and options in clear Hindi (Devanagari script).\n* Keep mathematical symbols, formulas, chemical equations, and code/HTML tags (e.g., <br>, <div>, <p>) in standard English/mathematical notation.`
      : `* Write all questions primarily in Hindi (Devanagari script).\n* Keep all technical terms, scientific names, mathematical terms, programming concepts, HTML/code tags (e.g., <br>, <div>, <p>), tools, software, acronyms, units, and formulas in English.`;

  const cleanTopic = topicOrNotes.trim();
  const topicSection = cleanTopic
    ? `### TOPIC / STUDY NOTES TO GENERATE QUIZ FROM:\n${cleanTopic}\n\nNow generate all ${safeCount} questions immediately inside a single \`\`\`text code block using the exact format above.`
    : `If you understand all rules above, respond ONLY with: "Ready for your topic/content!" and wait for the user to provide the topic or study notes.`;

  return `You are an Expert Quiz Master, Educational Content Analyst, and Assessment Designer.
Your task is to carefully analyze the topic, notes, or study material provided and generate a high-quality multiple-choice quiz.

### CRITICAL REQUIREMENT 1: Question Quantity & Difficulty
* Question Quantity: Generate EXACTLY ${safeCount} UNIQUE multiple-choice questions.
* Difficulty Level: ${difficultyDescription}.

### CRITICAL REQUIREMENT 2: Language Rules (${languageStyle})
${languageRules}

### CRITICAL REQUIREMENT 3: Pure LaTeX Math & Formula Formatting ($...$)
The quiz app uses a dedicated LaTeX Math Engine. Write ALL mathematical expressions, equations, fractions, powers, subscripts, roots, calculus limits, matrices, vectors, and chemical formulas in standard LaTeX wrapped in \`$...$\` (do NOT use Markdown formatting):
* Fractions (भिन्न / बटा): Always use \`\\frac{a}{b}\` inside \`$...$\`, e.g., \`$\\frac{1}{\\sin\\theta}$\`, \`$\\frac{\\sin\\theta}{\\cos\\theta}$\`, \`$\\frac{x + 1}{x - 1}$\`, \`$\\frac{\\sqrt{3}}{2}$\`.
* Exponents / Powers (Superscripts): Always use \`^{...}\` in LaTeX, e.g., \`$z^n + z^{-n}$\`, \`$x^{n+1}$\`, \`$10^{-6}$\`, \`$e^{i\\theta}$\`, \`$\\sin^2\\theta + \\cos^2\\theta = 1$\`, \`$\\left(\\frac{a}{b}\\right)^n$\`.
* Subscripts & Chemical Formulas: Always use \`_{...}\` in LaTeX, e.g., \`$a_n$\`, \`$a_{n-1}$\`, \`$\\log_{10}(x)$\`, \`$\\text{H}_2\\text{O}$\`, \`$\\text{CO}_2$\`, \`$\\text{H}_2\\text{SO}_4$\`.
* Square & Nth Roots (वर्गमूल): Always use \`\\sqrt{...}\` or \`\\sqrt[n]{...}\`, e.g., \`$\\sqrt{3}$\`, \`$\\sqrt{x^2 + y^2}$\`, \`$\\sqrt[3]{x}$\`, \`$\\sqrt{\\frac{1 - \\cos\\theta}{1 + \\cos\\theta}}$\`.
* Calculus, Limits, Vectors & Binomials: Use standard LaTeX commands such as \`$\\lim_{x \\to 0} \\frac{\\sin x}{x}$\`, \`$\\int_{0}^{\\pi/2} \\sin x \\, dx$\`, \`$\\sum_{k=1}^{n} k^2$\`, \`$\\binom{n}{r}$\`, \`$\\vec{a} \\cdot \\vec{b}$\`, \`$\\overline{AB}$\`, \`$\\hat{i}$\`.
* Greek Letters & Math Symbols: Use standard LaTeX commands: \`\\theta\`, \`\\pi\`, \`\\alpha\`, \`\\beta\`, \`\\gamma\`, \`\\Delta\`, \`\\Omega\`, \`^\\circ\`, \`\\pm\`, \`\\times\`, \`\\div\`, \`\\cdot\`, \`\\leq\`, \`\\geq\`, \`\\neq\`, \`\\approx\`, \`\\equiv\`, \`\\infty\`, \`\\rightarrow\`, \`\\Rightarrow\`, \`\\rightleftharpoons\`.
* Keep every question and option on a SINGLE line (use inline \`$...$\`, do NOT use multiline \`$$\` blocks).

### CRITICAL REQUIREMENT 4: Options & Escaping Quality
* Exactly 4 non-empty options per question. Only one correct answer index (0-indexed: 0, 1, 2, or 3).
* NEVER leave any option blank or empty.
* Keep HTML tags like <br>, <hr>, <p>, <div> as literal text on a single line (do NOT render them as actual line breaks).
* Inside double-quoted strings, always use single quotes ('...') instead of unescaped double quotes.

### CRITICAL REQUIREMENT 5: Strict Output Format
Return ONLY the raw quiz objects wrapped inside a single \`\`\`text code block so that LaTeX commands, backslashes, and tags are preserved verbatim:
\`\`\`text
{ q: "यदि $z = \\cos\\theta + i\\sin\\theta$ है, तो $z^n + z^{-n}$ किसके बराबर है?", o: ["$2\\cos(n\\theta)$", "$2\\sin(n\\theta)$", "$2i\\sin(n\\theta)$", "$2i\\cos(n\\theta)$"], a: 0 },
\`\`\`

${topicSection}`;
}

export const QUIZ_AI_PROMPT = buildAiPrompt();

/**
 * Repairs control characters that were accidentally produced if single-backslash LaTeX
 * commands like \frac (\f), \tan/\theta/\text/\times (\t), \beta/\begin/\binom (\b),
 * \right/\rho (\r), or \nabla/\neq/\nu (\n) were interpreted as JS escape sequences.
 */
export function repairControlCharsInLatex(input: string): string {
  if (!input) return '';
  return input
    .replace(/\x0c(?=[a-zA-Z])/g, '\\f')
    .replace(/\x08(?=[a-zA-Z])/g, '\\b')
    .replace(/\x09(?=[a-zA-Z])/g, '\\t')
    .replace(/\x0d(?=[a-zA-Z])/g, '\\r');
}

/**
 * Sanitizes pasted quiz text so that accidental newlines inside quoted options
 * (such as `<br>` converted to a newline on copy-paste) are restored to `<br>`.
 */
export function sanitizeQuizText(rawText: string): string {
  if (!rawText) return '';
  // Restore quoted options that contain only a newline `"\n"` to `"<br>"`
  return rawText.replace(/(["'])\s*\r?\n\s*\1/g, '$1<br>$1');
}

function normalizeJsonStringContent(inner: string): string {
  let res = '';
  let i = 0;
  const len = inner.length;

  while (i < len) {
    const ch = inner[i];
    if (ch === '\r') {
      i++;
      continue;
    }
    if (ch === '\n') {
      res += '\\n';
      i++;
      continue;
    }
    if (ch === '\t') {
      res += '\\t';
      i++;
      continue;
    }
    if (ch === '\\') {
      const next = inner[i + 1];
      if (next === undefined) {
        res += '\\\\';
        i++;
        continue;
      }
      if (next === '\\') {
        res += '\\\\';
        i += 2;
        continue;
      }
      if (next === '"' || next === '/') {
        res += '\\' + next;
        i += 2;
        continue;
      }
      if (next === 'u' && /^[0-9a-fA-F]{4}$/.test(inner.slice(i + 2, i + 6))) {
        res += inner.slice(i, i + 6);
        i += 6;
        continue;
      }
      res += '\\\\' + next;
      i += 2;
      continue;
    }
    res += ch;
    i++;
  }
  return res;
}

/**
 * Resolves flexible answer formats (0-indexed number, 1-indexed number like 4,
 * letter "A"/"B"/"C"/"D", "Option B", or exact option text) into a 0-based index.
 */
export function resolveAnswerIndex(rawAnswer: unknown, options: string[]): number {
  if (!options || options.length === 0 || rawAnswer === null || rawAnswer === undefined) {
    return 0;
  }

  if (typeof rawAnswer === 'number' && !isNaN(rawAnswer)) {
    const num = Math.trunc(rawAnswer);
    if (num >= 0 && num < options.length) return num;
    if (num >= 1 && num <= options.length) return num - 1;
    return 0;
  }

  if (typeof rawAnswer === 'string') {
    const s = rawAnswer.trim();
    const asInt = Number.parseInt(s, 10);
    if (!isNaN(asInt) && String(asInt) === s) {
      if (asInt >= 0 && asInt < options.length) return asInt;
      if (asInt >= 1 && asInt <= options.length) return asInt - 1;
    }

    const exactIdx = options.findIndex(
      (opt) => opt.trim().toLowerCase() === s.toLowerCase()
    );
    if (exactIdx >= 0) return exactIdx;

    if (s.length === 1) {
      const code = s.toUpperCase().charCodeAt(0) - 65;
      if (code >= 0 && code < options.length) return code;
    }

    const optMatch = /^(?:option|opt|ans|choice)\s*([a-zA-Z]|\d+)$/i.exec(s);
    if (optMatch) {
      const cap = optMatch[1];
      const capNum = Number.parseInt(cap, 10);
      if (!isNaN(capNum)) {
        if (capNum >= 1 && capNum <= options.length) return capNum - 1;
        if (capNum >= 0 && capNum < options.length) return capNum;
      }
      const capCode = cap.toUpperCase().charCodeAt(0) - 65;
      if (capCode >= 0 && capCode < options.length) return capCode;
    }
  }

  return 0;
}

/**
 * Robust parser for quiz objects supporting raw JS object syntax, single/double quotes,
 * trailing commas, and unescaped or double-escaped LaTeX expressions.
 */
export function safeParseQuiz(raw: string): QuizQuestion[] {
  let s = sanitizeQuizText(raw).trim();
  if (!s) return [];
  if (s.startsWith('[') && s.endsWith(']')) {
    s = s.slice(1, -1).trim();
  }
  if (s.endsWith(',')) s = s.slice(0, -1).trim();

  let out = '[';
  let i = 0;
  const len = s.length;

  try {
    while (i < len) {
      const ch = s[i];

      if (ch === '"') {
        let inner = '';
        i++;
        while (i < len) {
          const c = s[i];
          if (c === '\\') {
            inner += c + (s[i + 1] ?? '');
            i += 2;
            continue;
          }
          if (c === '"') {
            i++;
            break;
          }
          inner += c;
          i++;
        }
        out += '"' + normalizeJsonStringContent(inner) + '"';
        continue;
      }

      if (ch === "'") {
        let inner = '';
        i++;
        while (i < len) {
          const c = s[i];
          if (c === '\\') {
            if (s[i + 1] === "'") {
              inner += "'";
              i += 2;
              continue;
            }
            inner += c + (s[i + 1] ?? '');
            i += 2;
            continue;
          }
          if (c === "'") {
            i++;
            break;
          }
          if (c === '"') {
            inner += '\\"';
            i++;
            continue;
          }
          inner += c;
          i++;
        }
        out += '"' + normalizeJsonStringContent(inner) + '"';
        continue;
      }

      if (/[a-zA-Z_]/.test(ch)) {
        let ident = '';
        let j = i;
        while (j < len && /[\w]/.test(s[j])) {
          ident += s[j];
          j++;
        }
        let k = j;
        while (
          k < len &&
          (s[k] === ' ' || s[k] === '\t' || s[k] === '\n' || s[k] === '\r')
        ) {
          k++;
        }
        if (s[k] === ':') {
          out += `"${ident}"`;
          i = j;
          continue;
        }
        out += ident;
        i = j;
        continue;
      }

      out += ch;
      i++;
    }
    out += ']';
    out = out.replace(/,(\s*[\]}])/g, '$1');
    const parsed = JSON.parse(out);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    try {
      let cleanStr = s.replace(/([{,]\s*)([a-zA-Z0-9_]+)\s*:/g, '$1"$2":');
      cleanStr = cleanStr.replace(/,(\s*[\]}])/g, '$1');
      const parsed = JSON.parse(`[${cleanStr}]`);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
}

/**
 * Extracts individual `{ ... }` question objects from multi-line or pasted text,
 * properly respecting nested braces inside LaTeX like `\frac{a}{b}` or `\begin{pmatrix}...\end{pmatrix}`.
 */
export function extractQuizObjectsFromText(rawText: string): string[] {
  const results: string[] = [];
  let i = 0;
  const len = rawText.length;

  while (i < len) {
    while (i < len && rawText[i] !== '{') i++;
    if (i >= len) break;

    const start = i;
    let depth = 0;
    let inString: '"' | "'" | null = null;

    while (i < len) {
      const ch = rawText[i];

      if (inString) {
        if (ch === '\\') {
          i += 2;
          continue;
        }
        if (ch === inString) {
          inString = null;
        }
        i++;
        continue;
      } else {
        if (ch === '"' || ch === "'") {
          inString = ch;
          i++;
          continue;
        }
        if (ch === '{') {
          depth++;
        } else if (ch === '}') {
          depth--;
          if (depth === 0) {
            i++;
            results.push(rawText.slice(start, i));
            break;
          }
        }
        i++;
      }
    }
  }

  return results;
}

/**
 * Parses full quiz text into valid questions and count of skipped invalid items.
 */
export function parseQuizText(rawText: string): ParseQuizResult {
  const txt = sanitizeQuizText(rawText).trim();
  if (!txt) return { validQuestions: [], skippedQuestions: 0 };

  const validQuestions: QuizQuestion[] = [];
  let skippedQuestions = 0;

  const objects = extractQuizObjectsFromText(txt);

  objects.forEach((obj) => {
    try {
      const result = safeParseQuiz(obj);
      const rawItem = result[0] as unknown as Record<string, unknown>;
      if (!rawItem) {
        skippedQuestions++;
        return;
      }
      const qField =
        (rawItem.q as string) ||
        (rawItem.question as string) ||
        (rawItem.ques as string) ||
        (rawItem.title as string) ||
        '';
      const oField =
        rawItem.o ||
        rawItem.options ||
        rawItem.opts ||
        rawItem.choices ||
        rawItem.answers;
      const aField =
        rawItem.a ??
        rawItem.ans ??
        rawItem.answer ??
        rawItem.correct ??
        rawItem.correctAnswer ??
        rawItem.correct_answer ??
        rawItem.right;

      if (
        typeof qField === 'string' &&
        qField.trim().length > 0 &&
        Array.isArray(oField) &&
        oField.length >= 2
      ) {
        const cleanOpts = oField
          .slice(0, 4)
          .map((opt) => repairControlCharsInLatex(String(opt).trim()));
        while (cleanOpts.length < 4) {
          cleanOpts.push(`Option ${cleanOpts.length + 1}`);
        }
        const resolvedA = resolveAnswerIndex(aField, cleanOpts);
        validQuestions.push({
          q: repairControlCharsInLatex(qField.trim()),
          o: cleanOpts,
          a: resolvedA,
        });
      } else {
        skippedQuestions++;
      }
    } catch {
      skippedQuestions++;
    }
  });

  return { validQuestions, skippedQuestions };
}

/**
 * Formats a list of QuizQuestions into standard `{ q: "...", o: [...], a: 0 },` lines.
 */
export function formatQuestionsAsQuizText(questions: QuizQuestion[]): string {
  if (!questions || questions.length === 0) return '';
  return questions
    .map((q) => {
      const escapedQ = q.q.replace(/"/g, "'");
      const escapedOpts = q.o
        .map((opt) => `"${String(opt).replace(/"/g, "'")}"`)
        .join(', ');
      const safeA = Math.max(0, Math.min(q.o.length - 1, q.a));
      return `{ q: "${escapedQ}", o: [${escapedOpts}], a: ${safeA} },`;
    })
    .join('\n');
}

/**
 * Exports one or more quizzes (and optionally the Starred Questions Bank) into the
 * editable text format:
 *
 * === QUIZ: Quiz Name ===
 * CATEGORY: Category Name
 * { q: "Question 1", o: ["A", "B", "C", "D"], a: 0 },
 */
export function exportQuizzesToEditableText(
  quizzes: SavedQuiz[],
  starredQuestions: QuizQuestion[] = []
): string {
  if (quizzes.length === 0 && starredQuestions.length === 0) return '';
  const blocks: string[] = [];

  quizzes.forEach((quiz) => {
    const cleanName = quiz.name.trim() || 'Untitled Quiz';
    const cleanCat = (quiz.category || 'General').trim() || 'General';
    blocks.push(
      `=== QUIZ: ${cleanName} ===\nCATEGORY: ${cleanCat}\n${quiz.data.trim()}`
    );
  });

  if (starredQuestions.length > 0) {
    blocks.push(
      `=== QUIZ: Starred Questions Bank ===\nCATEGORY: __STARRED_BANK__\n${formatQuestionsAsQuizText(
        starredQuestions
      )}`
    );
  }

  return blocks.join('\n\n').trim();
}

/**
 * Helper to insert or update a quiz by (name, category) in a list of SavedQuizzes.
 */
export function upsertQuizInList(
  currentList: SavedQuiz[],
  name: string,
  category: string,
  data: string,
  questionCount?: number
): SavedQuiz[] {
  const cleanName =
    name.split(/\r?\n/)[0]?.trim().slice(0, 60) ||
    'Quiz ' + new Date().toLocaleDateString('hi-IN');
  const rawCat = category.split(/\r?\n/)[0]?.trim() || 'General';
  const cleanCat =
    !rawCat || rawCat.includes('{') || rawCat.length > 35 ? 'General' : rawCat;
  const qCount = questionCount ?? parseQuizText(data).validQuestions.length;

  const list = [...currentList];
  const existingIdx = list.findIndex(
    (item) =>
      item.name.toLowerCase() === cleanName.toLowerCase() &&
      (item.category || 'General').toLowerCase() === cleanCat.toLowerCase()
  );

  if (existingIdx >= 0) {
    const updated: SavedQuiz = {
      ...list[existingIdx],
      name: cleanName,
      category: cleanCat,
      data: data.trim(),
      timestamp: Date.now(),
      questionCount: qCount,
    };
    list.splice(existingIdx, 1);
    list.unshift(updated);
  } else {
    const created: SavedQuiz = {
      id:
        typeof crypto !== 'undefined' && crypto.randomUUID
          ? crypto.randomUUID()
          : `quiz_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      name: cleanName,
      category: cleanCat,
      data: data.trim(),
      timestamp: Date.now(),
      questionCount: qCount,
    };
    list.unshift(created);
  }

  return list;
}

/**
 * Imports quizzes from the editable text format (`=== QUIZ: Name ===` / `CATEGORY: Cat` / `{ q: ... }`),
 * OR from a JSON array backup, OR from a plain block of `{ q: ... }` questions.
 * Also detects and restores `Starred Questions Bank` (`__STARRED_BANK__`) blocks.
 */
export function importQuizzesFromText(
  rawText: string,
  existingQuizzes: SavedQuiz[],
  existingStarred: QuizQuestion[] = [],
  fallbackName = 'Imported Quiz'
): ImportQuizzesResult {
  const trimmed = rawText.trim();
  if (!trimmed) {
    return {
      quizzes: existingQuizzes,
      starredQuestions: existingStarred,
      importedCount: 0,
    };
  }

  let currentList = [...existingQuizzes];
  const currentStarred = [...existingStarred];
  let importedCount = 0;

  // 1. Check if the text uses === QUIZ: ... === or # QUIZ: ... or QUIZ: ... headers
  const headerRegex =
    /^(?:===\s*QUIZ\s*:|#\s*QUIZ\s*:|QUIZ\s*:)\s*(.+?)\s*(?:===)?\s*$/gim;
  const matches = Array.from(trimmed.matchAll(headerRegex));

  if (matches.length > 0) {
    for (let i = 0; i < matches.length; i++) {
      const match = matches[i];
      const quizName = (match[1] || '').trim() || `Imported Quiz ${i + 1}`;
      const blockStart = (match.index ?? 0) + match[0].length;
      const blockEnd =
        i + 1 < matches.length
          ? matches[i + 1].index ?? trimmed.length
          : trimmed.length;
      const blockContent = trimmed.slice(blockStart, blockEnd).trim();

      let category = 'General';
      const contentLines = blockContent.split(/\r?\n/);
      const categoryLineIdx = contentLines.findIndex((line) =>
        line.trim().toLowerCase().startsWith('category:')
      );
      if (categoryLineIdx >= 0) {
        const rawCat = contentLines[categoryLineIdx]
          .slice(contentLines[categoryLineIdx].indexOf(':') + 1)
          .trim();
        category = rawCat || 'General';
        contentLines.splice(categoryLineIdx, 1);
      }

      const quizBody = sanitizeQuizText(contentLines.join('\n')).trim();
      const parsed = parseQuizText(quizBody);
      if (parsed.validQuestions.length > 0) {
        const isStarredBank =
          category.toLowerCase() === '__starred_bank__' ||
          quizName.toLowerCase() === 'starred questions bank' ||
          (category.toLowerCase() === 'starred' &&
            quizName.toLowerCase().includes('starred'));

        if (isStarredBank) {
          for (const sq of parsed.validQuestions) {
            const exists = currentStarred.some(
              (eq) => eq.q.trim().toLowerCase() === sq.q.trim().toLowerCase()
            );
            if (!exists) {
              currentStarred.push(sq);
            }
          }
          importedCount++;
        } else {
          currentList = upsertQuizInList(
            currentList,
            quizName,
            category,
            quizBody,
            parsed.validQuestions.length
          );
          importedCount++;
        }
      }
    }

    if (importedCount > 0) {
      return {
        quizzes: currentList,
        starredQuestions: currentStarred,
        importedCount,
      };
    }
  }

  // 2. Check if it is a JSON Array of saved quizzes [{"name":..., "category":..., "data":...}]
  if (trimmed.startsWith('[')) {
    try {
      const arr = JSON.parse(trimmed);
      if (Array.isArray(arr)) {
        for (const obj of arr) {
          if (obj && typeof obj === 'object' && 'data' in obj && 'name' in obj) {
            const qName = String(obj.name || 'Imported Quiz').trim();
            const qCat = String(obj.category || 'General').trim() || 'General';
            const qData = sanitizeQuizText(String(obj.data || '')).trim();
            const parsed = parseQuizText(qData);
            if (parsed.validQuestions.length > 0) {
              currentList = upsertQuizInList(
                currentList,
                qName,
                qCat,
                qData,
                parsed.validQuestions.length
              );
              importedCount++;
            }
          }
        }
        if (importedCount > 0) {
          return {
            quizzes: currentList,
            starredQuestions: currentStarred,
            importedCount,
          };
        }
      }
    } catch {
      // Fall through to plain block import
    }
  }

  // 3. Fallback: Plain quiz questions block without === QUIZ: === header
  let singleCategory = 'General';
  const singleName = fallbackName.trim() || 'Imported Quiz';
  const lines = trimmed.split(/\r?\n/);
  const catIdx = lines.findIndex((l) =>
    l.trim().toLowerCase().startsWith('category:')
  );
  if (catIdx >= 0) {
    singleCategory =
      lines[catIdx].slice(lines[catIdx].indexOf(':') + 1).trim() || 'General';
    lines.splice(catIdx, 1);
  }
  const body = sanitizeQuizText(lines.join('\n')).trim();
  const parsed = parseQuizText(body);
  if (parsed.validQuestions.length > 0) {
    currentList = upsertQuizInList(
      currentList,
      singleName,
      singleCategory,
      body,
      parsed.validQuestions.length
    );
    return {
      quizzes: currentList,
      starredQuestions: currentStarred,
      importedCount: 1,
    };
  }

  return {
    quizzes: existingQuizzes,
    starredQuestions: existingStarred,
    importedCount: 0,
  };
}

/**
 * Escapes HTML special characters in plain text segments outside math mode.
 */
function escapeHtml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/**
 * Renders a single LaTeX formula string with KaTeX.
 */
export function renderKatexFormula(latex: string, displayMode: boolean): string {
  const cleaned = repairControlCharsInLatex(latex).trim();
  if (!cleaned) return '';
  try {
    const needsDisplayStyle =
      !displayMode &&
      !/^\\(?:displaystyle|textstyle)/.test(cleaned) &&
      /\\(?:frac|int|oint|iint|iiint|sum|prod|lim|binom)/.test(cleaned);
    const formulaToRender = needsDisplayStyle ? `\\displaystyle ${cleaned}` : cleaned;

    return katex.renderToString(formulaToRender, {
      displayMode,
      throwOnError: false,
      strict: false,
      trust: true,
      output: 'htmlAndMathml',
    });
  } catch {
    return `<code class="latex-fallback">${escapeHtml(cleaned)}</code>`;
  }
}

const NAKED_LATEX_REGEX =
  /(\\begin\{([a-zA-Z*]+)\}[\s\S]*?\\end\{\2\}|\\(?:frac|dfrac|tfrac|binom)\s*\{(?:[^{}]|\{[^{}]*\})*\}\s*\{(?:[^{}]|\{[^{}]*\})*\}|\\sqrt(?:\[[^\]]*\])?\s*\{(?:[^{}]|\{[^{}]*\})*\}|\\(?:int|oint|iint|iiint|sum|prod|lim|bigcup|bigcap)(?:\s*[_^]\s*(?:\{(?:[^{}]|\{[^{}]*\})*\}|[a-zA-Z0-9\\]+))*|[a-zA-Z0-9)]+(?:\s*[_^]\s*\{(?:[^{}]|\{[^{}]*\})*\})+|\\(?:vec|hat|bar|overline|underline|mathbf|mathrm|mathbb|mathcal|text)\s*\{(?:[^{}]|\{[^{}]*\})*\}|\\(?:alpha|beta|gamma|delta|epsilon|varepsilon|zeta|eta|theta|vartheta|iota|kappa|lambda|mu|nu|xi|pi|rho|sigma|tau|upsilon|phi|varphi|chi|psi|omega|Gamma|Delta|Theta|Lambda|Xi|Pi|Sigma|Upsilon|Phi|Psi|Omega|infty|nabla|partial|hbar|ell|forall|exists|in|notin|subset|subseteq|cup|cap|pm|mp|times|div|cdot|ast|star|circ|bullet|oplus|otimes|leq|le|geq|ge|neq|ne|approx|equiv|cong|propto|sim|rightarrow|to|leftarrow|leftrightarrow|Rightarrow|Leftarrow|Leftrightarrow|uparrow|downarrow|sin|cos|tan|cot|sec|csc|arcsin|arccos|arctan|sinh|cosh|tanh|ln|log|exp|det|deg)(?![a-zA-Z]))/g;

function renderPlainTextWithFallbackLatex(text: string): string {
  const repaired = repairControlCharsInLatex(text);
  if (!repaired.includes('\\') && !repaired.includes('^{') && !repaired.includes('_{')) {
    return escapeHtml(repaired);
  }

  const hasDevanagari = /[\u0900-\u097F]/.test(repaired);
  if (
    !hasDevanagari &&
    /^\s*\\(?:frac|dfrac|sqrt|int|sum|prod|lim|begin|left|pmatrix|bmatrix)/.test(repaired)
  ) {
    const isBlock =
      /\\begin\{(?:equation|align|gather|pmatrix|bmatrix|vmatrix|matrix|cases)/.test(
        repaired
      );
    return renderKatexFormula(repaired, isBlock);
  }

  let result = '';
  let lastIndex = 0;
  NAKED_LATEX_REGEX.lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = NAKED_LATEX_REGEX.exec(repaired)) !== null) {
    if (match.index > lastIndex) {
      result += escapeHtml(repaired.slice(lastIndex, match.index));
    }
    const token = match[0];
    const isBlock = /^\\begin\{(?:equation|align|gather)/.test(token);
    result += renderKatexFormula(token, isBlock);
    lastIndex = match.index + token.length;
  }

  if (lastIndex < repaired.length) {
    result += escapeHtml(repaired.slice(lastIndex));
  }

  return result;
}

export function renderMixedLatexToHtml(input: string): string {
  if (input === null || input === undefined) return '';
  const str = repairControlCharsInLatex(String(input));
  if (!str) return '';

  const DELIMITED_MATH_REGEX =
    /(\$\$[\s\S]+?\$\$|\\\[[\s\S]+?\\\]|\\\([\s\S]+?\\\)|\\begin\{([a-zA-Z*]+)\}[\s\S]+?\\end\{\2\}|(?<!\\)\$[^\$\n]+?(?<!\\)\$)/g;

  let html = '';
  let lastIdx = 0;
  let match: RegExpExecArray | null;

  while ((match = DELIMITED_MATH_REGEX.exec(str)) !== null) {
    if (match.index > lastIdx) {
      html += renderPlainTextWithFallbackLatex(str.slice(lastIdx, match.index));
    }

    const rawToken = match[0];
    if (rawToken.startsWith('$$') && rawToken.endsWith('$$')) {
      html += renderKatexFormula(rawToken.slice(2, -2), true);
    } else if (rawToken.startsWith('\\[') && rawToken.endsWith('\\]')) {
      html += renderKatexFormula(rawToken.slice(2, -2), true);
    } else if (rawToken.startsWith('\\(') && rawToken.endsWith('\\)')) {
      html += renderKatexFormula(rawToken.slice(2, -2), false);
    } else if (rawToken.startsWith('$') && rawToken.endsWith('$')) {
      const inner = rawToken.slice(1, -1);
      const isDisplayEnv = /\\begin\{(?:equation|align|gather)/.test(inner);
      html += renderKatexFormula(inner, isDisplayEnv);
    } else if (rawToken.startsWith('\\begin{')) {
      const isBlock = /^\\begin\{(?:equation|align|gather)/.test(rawToken);
      html += renderKatexFormula(rawToken, isBlock);
    }

    lastIdx = match.index + rawToken.length;
  }

  if (lastIdx < str.length) {
    html += renderPlainTextWithFallbackLatex(str.slice(lastIdx));
  }

  return html;
}

export function latexToSpokenText(input: string): string {
  if (!input) return '';
  let s = repairControlCharsInLatex(input);

  s = s.replace(/[\p{Emoji_Presentation}\p{Extended_Pictographic}]/gu, '');
  s = s.replace(/\$\$/g, ' ').replace(/\$/g, ' ').replace(/\\\[|\\\]|\\\(|\\\)/g, ' ');

  s = s.replace(/\\frac\s*\{([^{}]*)\}\s*\{([^{}]*)\}/g, '$1 over $2');
  s = s.replace(/\\sqrt\s*\[([^\]]*)\]\s*\{([^{}]*)\}/g, '$1th root of $2');
  s = s.replace(/\\sqrt\s*\{([^{}]*)\}/g, 'square root of $1');
  s = s.replace(/\\int_\{([^{}]*)\}\^\{([^{}]*)\}/g, 'integral from $1 to $2 of ');
  s = s.replace(/\\int/g, 'integral ');
  s = s.replace(/\\sum_\{([^{}]*)\}\^\{([^{}]*)\}/g, 'sum from $1 to $2 of ');
  s = s.replace(/\\sum/g, 'sum ');
  s = s.replace(/\\lim_\{([^{}]*)\}/g, 'limit as $1 of ');
  s = s.replace(/\\to|\\rightarrow/g, ' approaches ');
  s = s.replace(/\\infty/g, ' infinity ');
  s = s.replace(/\\pi/g, ' pi ');
  s = s.replace(/\\theta/g, ' theta ');
  s = s.replace(/\\alpha/g, ' alpha ');
  s = s.replace(/\\beta/g, ' beta ');
  s = s.replace(/\\gamma/g, ' gamma ');
  s = s.replace(/\\lambda/g, ' lambda ');
  s = s.replace(/\\omega/g, ' omega ');
  s = s.replace(/\\pm/g, ' plus or minus ');
  s = s.replace(/\\times/g, ' times ');
  s = s.replace(/\\div/g, ' divided by ');
  s = s.replace(/\\neq/g, ' not equal to ');
  s = s.replace(/\\leq|\\le/g, ' less than or equal to ');
  s = s.replace(/\\geq|\\ge/g, ' greater than or equal to ');
  s = s.replace(/\^2(?![0-9])/g, ' squared ');
  s = s.replace(/\^3(?![0-9])/g, ' cubed ');
  s = s.replace(/\^\{([^{}]*)\}/g, ' to the power of $1 ');
  s = s.replace(/_\{([^{}]*)\}/g, ' sub $1 ');
  s = s.replace(/\\begin\{[a-zA-Z*]+\}|\\end\{[a-zA-Z*]+\}/g, ' ');
  s = s.replace(/\\[a-zA-Z]+/g, ' ');
  s = s.replace(/[{}\\]/g, ' ');

  return s.replace(/\s+/g, ' ').trim();
}

export function shuffleArray<T>(arr: T[]): T[] {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

export function prepareQuizQuestions(source: QuizQuestion[]): QuizQuestion[] {
  const cloned: QuizQuestion[] = JSON.parse(JSON.stringify(source));
  cloned.forEach((q) => {
    const opts = q.o.map((t, i) => ({ t, ok: i === q.a }));
    const s = shuffleArray(opts);
    q.o = s.map((x) => x.t);
    q.a = s.findIndex((x) => x.ok);
  });
  return cloned;
}

export function getGrade(acc: number): { grade: string; msg: string } {
  if (acc >= 90) return { grade: 'A+', msg: 'शानदार! आप बहुत होशियार हैं 🎉' };
  if (acc >= 80) return { grade: 'A', msg: 'बहुत अच्छा! बस थोड़ा और मेहनत करें 👏' };
  if (acc >= 70) return { grade: 'B', msg: 'अच्छा प्रदर्शन! और बेहतर हो सकते हैं 👍' };
  if (acc >= 60) return { grade: 'C', msg: 'ठीक है, और पढ़ाई करें 📚' };
  if (acc >= 40) return { grade: 'D', msg: 'थोड़ा और ध्यान दें 🔁' };
  return { grade: 'F', msg: 'हिम्मत रखें! फिर कोशिश करें 💪' };
}

/**
 * Formats seconds into MM:SS for Exam Mode timer.
 */
export function formatExamTime(seconds: number): string {
  const safe = Math.max(0, Math.floor(seconds));
  const mins = Math.floor(safe / 60);
  const secs = safe % 60;
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

/**
 * Evaluates an Exam Mode submission against activeQuiz and examAnswers map.
 */
export function evaluateExamSubmission(
  activeQuiz: QuizQuestion[],
  examAnswers: Record<number, number>
): {
  score: number;
  skipped: number;
  wrongQuestions: QuizQuestion[];
} {
  let correctCount = 0;
  let skippedCount = 0;
  const wrongs: QuizQuestion[] = [];

  activeQuiz.forEach((q, idx) => {
    const chosen = examAnswers[idx];
    if (chosen === undefined || chosen === null) {
      skippedCount++;
      wrongs.push({ ...q, _timedOut: true, userAnswerIndex: null });
    } else if (chosen === q.a) {
      correctCount++;
    } else {
      wrongs.push({ ...q, _timedOut: false, userAnswerIndex: chosen });
    }
  });

  return {
    score: correctCount,
    skipped: skippedCount,
    wrongQuestions: wrongs,
  };
}

