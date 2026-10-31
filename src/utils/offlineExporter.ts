import { SavedQuiz, parseQuizText } from './latexQuizEngine';

/**
 * Generates a self-contained offline HTML file for a saved quiz,
 * preserving the full Raman Quiz Challenge interface AND embedding
 * complete KaTeX support for all LaTeX mathematical formats.
 */
export function buildOfflineQuizHtml(item: SavedQuiz): string {
  const parsedQuestions = parseQuizText(item.data).validQuestions;
  const serializedQuestions = JSON.stringify(parsedQuestions).replace(
    /<\//g,
    '<\\/'
  );
  const escapedName = item.name
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  return `<!DOCTYPE html>
<html lang="hi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapedName} - Raman Quiz Challenge</title>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css">
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js"></script>
    <style>
      :root {
        --card: #ffffff; --text: #1e293b; --muted: #64748b; --bg: #f8fafc;
        --border: #e2e8f0; --option-bg: #ffffff; --header-from: #2563eb;
        --header-to: #4f46e5; --progress-bg: #e5e7eb;
      }
      @media (prefers-color-scheme: dark) {
        :root {
          --card: #1e293b; --text: #f1f5f9; --muted: #94a3b8; --bg: #0f172a;
          --border: #334155; --option-bg: #1e293b; --progress-bg: #334155;
        }
      }
      * { font-family: "Poppins", sans-serif; box-sizing: border-box; }
      body { background: linear-gradient(135deg, #d1fae5 0%, #cffafe 40%, #dbeafe 100%); min-height: 100vh; margin: 0; padding: 0; }
      @media (prefers-color-scheme: dark) { body { background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); } }
      .quiz-card { width: 100%; max-width: 1200px; background: var(--card); border-radius: 24px; overflow: visible; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.13); margin: auto; }
      .header { padding: 20px 24px; background: linear-gradient(135deg, var(--header-from), var(--header-to)); color: white; border-radius: 24px 24px 0 0; }
      .header-row { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
      .quiz-controls { display: flex; gap: 8px; align-items: center; flex-shrink: 0; }
      #startBtn { padding: 6px 14px; border: none; border-radius: 8px; background: white; color: #2563eb; font-weight: 700; font-size: 14px; cursor: pointer; }
      .title { font-size: 24px; font-weight: 700; text-align: center; flex: 1; min-width: 160px; }
      .score-box { background: white; color: #111; padding: 6px 16px; border-radius: 999px; font-weight: 700; font-size: 14px; }
      .progress-area { padding: 16px 24px 0; }
      .progress-info { display: flex; justify-content: space-between; align-items: center; font-size: 13px; margin-bottom: 8px; color: var(--muted); }
      .progress-bar { height: 20px; background: var(--progress-bg); border-radius: 999px; overflow: hidden; }
      .progress-fill { height: 100%; width: 0%; background: linear-gradient(90deg, #2563eb, #60a5fa); transition: width 0.4s ease; }
      .content { padding: 24px; height: auto; overflow: visible; }
      .question-wrapper { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 20px; height: auto; overflow: visible; }
      .question { font-size: 24px; font-weight: 700; line-height: 1.65; color: var(--text); flex: 1; min-width: 0; height: auto; overflow: visible; white-space: normal; word-wrap: break-word; overflow-wrap: anywhere; }
      .options { display: flex; flex-direction: column; gap: 12px; height: auto; overflow: visible; }
      .option { width: 100%; text-align: left; padding: 14px 18px; border: 2px solid var(--border); border-radius: 16px; cursor: pointer; font-size: 18px; font-weight: 500; line-height: 1.55; background: var(--option-bg); color: var(--text); transition: all 0.22s ease; display: flex; align-items: center; gap: 12px; height: auto; overflow: visible; white-space: normal; word-wrap: break-word; overflow-wrap: anywhere; }
      .option-label { width: 30px; height: 30px; border-radius: 50%; background: #eff6ff; color: #2563eb; font-size: 13px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
      .option.correct { background: linear-gradient(135deg, #d1fae5, #a7f3d0); color: #065f46; border-color: #10b981; }
      .option.wrong { background: linear-gradient(135deg, #fee2e2, #fecaca); color: #991b1b; border-color: #ef4444; }
      .option.disabled { opacity: 0.7; pointer-events: none; }
      .katex { font-size: 1.08em !important; white-space: normal !important; }
      .katex .katex-html, .katex .base { white-space: normal !important; }
      .katex-display { margin: 0.65em 0 !important; overflow: visible !important; white-space: normal !important; }
    </style>
</head>
<body class="p-4 flex items-center justify-center min-h-screen">
  <div class="quiz-card">
    <div class="header">
      <div class="header-row">
        <div class="quiz-controls">
          <button id="startBtn" onclick="startQuiz()">🔄 Restart</button>
        </div>
        <div class="title">🏆 ${escapedName} 🏆</div>
        <div class="score-box" id="scoreBox">Score: 0</div>
      </div>
    </div>
    <div id="quizScreen">
      <div class="progress-area">
        <div class="progress-info">
          <span id="questionNumber"></span>
          <span id="remaining"></span>
        </div>
        <div class="progress-bar"><div class="progress-fill" id="progressFill"></div></div>
      </div>
      <div class="content">
        <div class="question-wrapper"><div class="question" id="question"></div></div>
        <div class="options" id="options"></div>
        <div class="feedback" id="feedback" style="margin-top:14px;font-weight:700;min-height:28px;"></div>
      </div>
    </div>
  </div>
  <script>
    var QUIZ_DATA = ${serializedQuestions};
    var activeQuestions = [];
    var currentIdx = 0;
    var score = 0;
    var locked = false;
    var nextTimer = null;

    function escapeHtml(s) {
      return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function renderMath(str) {
      var s = String(str);
      if (!window.katex) return escapeHtml(s);
      var regex = /(\\$\\$[\\s\\S]+?\\$\\$|\\\\\\[[\\s\\S]+?\\\\\\]|\\\\\\([\\s\\S]+?\\\\\\)|(?<!\\\\)\\$[^\\$\\n]+?(?<!\\\\)\\$)/g;
      var out = '';
      var lastIdx = 0;
      var match;
      while ((match = regex.exec(s)) !== null) {
        if (match.index > lastIdx) {
          out += escapeHtml(s.slice(lastIdx, match.index));
        }
        var m = match[0];
        var display = m.startsWith('$$') || m.startsWith('\\\\[');
        var inner = display ? m.slice(2, -2) : (m.startsWith('\\\\(') ? m.slice(2, -2) : m.slice(1, -1));
        try { out += katex.renderToString(inner, { displayMode: display, throwOnError: false }); }
        catch(e) { out += escapeHtml(m); }
        lastIdx = match.index + m.length;
      }
      if (lastIdx < s.length) {
        out += escapeHtml(s.slice(lastIdx));
      }
      return out;
    }

    function startQuiz() {
      if (nextTimer) {
        clearTimeout(nextTimer);
        nextTimer = null;
      }
      activeQuestions = QUIZ_DATA.slice();
      currentIdx = 0;
      score = 0;
      locked = false;
      document.getElementById('scoreBox').textContent = 'Score: 0';
      loadQuestion();
    }

    function loadQuestion() {
      var total = activeQuestions.length;
      if (total === 0) {
        document.getElementById('question').textContent = 'No questions found.';
        return;
      }
      if (currentIdx >= total) {
        var pct = Math.round((score / total) * 100);
        document.getElementById('questionNumber').textContent = 'Quiz Completed';
        document.getElementById('remaining').textContent = 'Accuracy: ' + pct + '%';
        document.getElementById('progressFill').style.width = '100%';
        document.getElementById('question').innerHTML = '🎉 आपने ' + total + ' में से ' + score + ' सही उत्तर दिए (' + pct + '%)!';
        document.getElementById('options').innerHTML = '';
        document.getElementById('feedback').textContent = '';
        return;
      }
      locked = false;
      var q = activeQuestions[currentIdx];
      document.getElementById('questionNumber').textContent = 'प्रश्न ' + (currentIdx + 1) + ' / ' + total;
      document.getElementById('remaining').textContent = 'शेष: ' + (total - currentIdx - 1);
      document.getElementById('progressFill').style.width = Math.round(((currentIdx + 1) / total) * 100) + '%';
      document.getElementById('question').innerHTML = renderMath(q.q);
      document.getElementById('feedback').textContent = '';

      var optsEl = document.getElementById('options');
      optsEl.innerHTML = '';
      var labels = ['A', 'B', 'C', 'D'];
      q.o.forEach(function(optText, idx) {
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'option';
        btn.innerHTML = '<span class="option-label">' + (labels[idx] || (idx + 1)) + '</span><span style="flex:1">' + renderMath(optText) + '</span>';
        btn.onclick = function() { checkAnswer(idx); };
        optsEl.appendChild(btn);
      });
    }

    function checkAnswer(idx) {
      if (locked) return;
      locked = true;
      var q = activeQuestions[currentIdx];
      var buttons = document.querySelectorAll('#options .option');
      var fb = document.getElementById('feedback');
      if (idx === q.a) {
        score++;
        document.getElementById('scoreBox').textContent = 'Score: ' + score;
        buttons[idx].classList.add('correct');
        fb.style.color = '#16a34a';
        fb.textContent = '✅ सही जवाब!';
      } else {
        buttons[idx].classList.add('wrong');
        if (buttons[q.a]) buttons[q.a].classList.add('correct');
        fb.style.color = '#dc2626';
        fb.textContent = '❌ गलत जवाब!';
      }
      buttons.forEach(function(b, i) {
        if (i !== idx && i !== q.a) b.classList.add('disabled');
      });
      nextTimer = setTimeout(function() {
        nextTimer = null;
        currentIdx++;
        loadQuestion();
      }, 1100);
    }

    window.onload = startQuiz;
  </script>
</body>
</html>`;
}

