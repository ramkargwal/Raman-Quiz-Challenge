/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  QuizQuestion,
  SavedQuiz,
  QuizMode,
  DEFAULT_QUIZ_QUESTIONS,
  buildAiPrompt,
  BuildAiPromptOptions,
  parseQuizText,
  shuffleArray,
  prepareQuizQuestions,
  getGrade,
  latexToSpokenText,
  upsertQuizInList,
  importQuizzesFromText,
  formatExamTime,
  evaluateExamSubmission,
} from './utils/latexQuizEngine';
import { buildOfflineQuizHtml } from './utils/offlineExporter';
import { LatexText } from './components/LatexText';
import { QuizDatabaseScreen } from './components/QuizDatabaseScreen';
import {
  ExamBottomBar,
  ExamPaletteDialog,
  ExamSubmitConfirmDialog,
  ExitQuizConfirmDialog,
  QuestionTimingDialog,
  QuestionQtyDialog,
  CopyPromptDialog,
} from './components/ExamAndSettingsModals';

const DEFAULT_TIMER_SECONDS = 30;
const STORAGE_KEY_QUIZZES = 'savedQuizzes';
const STORAGE_KEY_STARRED = 'starredQuestionsBank';
const STORAGE_KEY_DARK_MODE = 'dark_mode_override';
const STORAGE_KEY_QUESTION_TIME = 'question_time_seconds';
const STORAGE_KEY_QUIZ_MODE = 'quiz_mode';

function loadInitialSavedQuizzes(): SavedQuiz[] {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_KEY_QUIZZES) || '[]');
    if (!Array.isArray(raw)) return [];
    return raw.map((item: Record<string, unknown>, idx: number) => {
      const dataStr = String(item.data || '');
      const qCount =
        typeof item.questionCount === 'number' && item.questionCount > 0
          ? item.questionCount
          : parseQuizText(dataStr).validQuestions.length;
      const ts =
        typeof item.timestamp === 'number'
          ? item.timestamp
          : item.date
          ? new Date(String(item.date)).getTime() || Date.now()
          : Date.now();
      return {
        id: String(item.id || `quiz_${ts}_${idx}`),
        name: String(item.name || 'Untitled Quiz').trim() || 'Untitled Quiz',
        category: String(item.category || 'General').trim() || 'General',
        data: dataStr,
        timestamp: ts,
        questionCount: qCount,
      };
    });
  } catch {
    return [];
  }
}

function loadInitialStarredQuestions(): QuizQuestion[] {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_KEY_STARRED) || '[]');
    if (!Array.isArray(raw)) return [];
    return raw.filter(
      (q) => q && typeof q.q === 'string' && Array.isArray(q.o) && q.o.length >= 2
    );
  } catch {
    return [];
  }
}

export default function App() {
  const [screen, setScreen] = useState<'home' | 'database' | 'quiz' | 'result'>(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('autostart') === '1') return 'quiz';
    if (params.get('screen') === 'database') return 'database';
    return 'home';
  });

  // Theme state
  const [isDark, setIsDark] = useState<boolean>(() => {
    const saved = localStorage.getItem(STORAGE_KEY_DARK_MODE);
    if (saved !== null) return saved === 'true';
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
  });

  useEffect(() => {
    const root = document.documentElement;
    if (isDark) {
      root.classList.add('dark');
      root.classList.remove('light');
    } else {
      root.classList.remove('dark');
      root.classList.add('light');
    }
    localStorage.setItem(STORAGE_KEY_DARK_MODE, String(isDark));
  }, [isDark]);

  // Quiz Mode (PRACTICE vs EXAM) & Question Timing
  const [quizMode, setQuizMode] = useState<QuizMode>(() => {
    const saved = localStorage.getItem(STORAGE_KEY_QUIZ_MODE);
    return saved === 'EXAM' ? 'EXAM' : 'PRACTICE';
  });
  const [questionTimeSeconds, setQuestionTimeSeconds] = useState<number>(() => {
    const saved = Number.parseInt(
      localStorage.getItem(STORAGE_KEY_QUESTION_TIME) || '',
      10
    );
    return !isNaN(saved) && saved >= 5 && saved <= 600
      ? saved
      : DEFAULT_TIMER_SECONDS;
  });

  // Modals state
  const [showTimerDialog, setShowTimerDialog] = useState<boolean>(false);
  const [showQtyDialog, setShowQtyDialog] = useState<boolean>(false);
  const [showPaletteDialog, setShowPaletteDialog] = useState<boolean>(false);
  const [showSubmitConfirmDialog, setShowSubmitConfirmDialog] =
    useState<boolean>(false);
  const [showExitConfirmDialog, setShowExitConfirmDialog] =
    useState<boolean>(false);
  const [showCopyPromptDialog, setShowCopyPromptDialog] =
    useState<boolean>(false);

  // Home page state
  const [quizInput, setQuizInput] = useState<string>('');
  const [quizName, setQuizName] = useState<string>('');
  const [quizCategory, setQuizCategory] = useState<string>('General');
  const [isDragOver, setIsDragOver] = useState<boolean>(false);
  const [copySuccess, setCopySuccess] = useState<boolean>(false);
  const [saveSuccess, setSaveSuccess] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Quiz Database state
  const [savedQuizzes, setSavedQuizzes] = useState<SavedQuiz[]>(
    loadInitialSavedQuizzes
  );
  const [starredQuestions, setStarredQuestions] = useState<QuizQuestion[]>(
    loadInitialStarredQuestions
  );
  const [selectedCategoryFilter, setSelectedCategoryFilter] =
    useState<string>('All');
  const [savedQuizSearchQuery, setSavedQuizSearchQuery] = useState<string>('');

  // Quiz pool & active quiz state
  const [quiz, setQuiz] = useState<QuizQuestion[]>(DEFAULT_QUIZ_QUESTIONS);
  const unplayedPoolRef = useRef<QuizQuestion[]>(
    shuffleArray([...DEFAULT_QUIZ_QUESTIONS])
  );
  const [activeQuiz, setActiveQuiz] = useState<QuizQuestion[]>(() =>
    prepareQuizQuestions(DEFAULT_QUIZ_QUESTIONS)
  );
  const [numQuestionsInput, setNumQuestionsInput] = useState<number>(
    DEFAULT_QUIZ_QUESTIONS.length
  );
  const [wrongQuestions, setWrongQuestions] = useState<QuizQuestion[]>([]);
  const [current, setCurrent] = useState<number>(0);
  const [score, setScore] = useState<number>(0);
  const [skipped, setSkipped] = useState<number>(0);
  const [locked, setLocked] = useState<boolean>(false);
  const [selectedIdx, setSelectedIdx] = useState<number | null>(null);
  const [examAnswers, setExamAnswers] = useState<Record<number, number>>({});
  const [examBookmarkedIndices, setExamBookmarkedIndices] = useState<
    Set<number>
  >(() => new Set());
  const [examTotalTimeSeconds, setExamTotalTimeSeconds] = useState<number>(
    () => DEFAULT_QUIZ_QUESTIONS.length * questionTimeSeconds
  );
  const [feedbackHtml, setFeedbackHtml] = useState<{
    text: string;
    color: string;
  } | null>(null);
  const [scorePop, setScorePop] = useState<boolean>(false);
  const [showReview, setShowReview] = useState<boolean>(false);
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);

  // Timer state
  const [timeLeft, setTimeLeft] = useState<number>(() =>
    quizMode === 'EXAM'
      ? DEFAULT_QUIZ_QUESTIONS.length * questionTimeSeconds
      : questionTimeSeconds
  );
  const [timerPaused, setTimerPaused] = useState<boolean>(false);

  // Refs
  const nextTimeoutRef = useRef<number | null>(null);
  const toastTimeoutRef = useRef<number | null>(null);
  const scorePopTimeoutRef = useRef<number | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);

  const showToast = useCallback((msg: string) => {
    if (toastTimeoutRef.current) {
      clearTimeout(toastTimeoutRef.current);
    }
    setToastMessage(msg);
    toastTimeoutRef.current = window.setTimeout(() => {
      setToastMessage((prev) => (prev === msg ? null : prev));
      toastTimeoutRef.current = null;
    }, 3000);
  }, []);

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(Boolean(document.fullscreenElement));
    };
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      if (nextTimeoutRef.current) clearTimeout(nextTimeoutRef.current);
      if (toastTimeoutRef.current) clearTimeout(toastTimeoutRef.current);
      if (scorePopTimeoutRef.current) clearTimeout(scorePopTimeoutRef.current);
    };
  }, []);

  const persistQuizzes = useCallback((list: SavedQuiz[]) => {
    setSavedQuizzes(list);
    localStorage.setItem(STORAGE_KEY_QUIZZES, JSON.stringify(list));
  }, []);

  const persistStarred = useCallback((list: QuizQuestion[]) => {
    setStarredQuestions(list);
    localStorage.setItem(STORAGE_KEY_STARRED, JSON.stringify(list));
  }, []);

  // Warm Marimba / Wood Sound Effects
  const playSound = useCallback(
    (type: 'correct' | 'wrong' | 'hover' | 'click') => {
      try {
        const AC =
          window.AudioContext ||
          (window as unknown as { webkitAudioContext: typeof AudioContext })
            .webkitAudioContext;
        if (!AC) return;
        if (!audioCtxRef.current || audioCtxRef.current.state === 'closed') {
          audioCtxRef.current = new AC();
        }
        const ctx = audioCtxRef.current;
        if (ctx.state === 'suspended') {
          ctx.resume().catch(() => {});
        }

        const playWoodNote = (
          startSec: number,
          durationSec: number,
          freqHz: number,
          amp: number
        ) => {
          const osc1 = ctx.createOscillator();
          const osc2 = ctx.createOscillator();
          const gain1 = ctx.createGain();
          const gain2 = ctx.createGain();

          osc1.type = 'sine';
          osc1.frequency.setValueAtTime(freqHz, ctx.currentTime + startSec);

          osc2.type = 'sine';
          osc2.frequency.setValueAtTime(
            freqHz * 2.0,
            ctx.currentTime + startSec
          );

          gain1.gain.setValueAtTime(0.001, ctx.currentTime + startSec);
          gain1.gain.linearRampToValueAtTime(
            amp,
            ctx.currentTime + startSec + 0.006
          );
          gain1.gain.exponentialRampToValueAtTime(
            0.001,
            ctx.currentTime + startSec + durationSec
          );

          gain2.gain.setValueAtTime(0.001, ctx.currentTime + startSec);
          gain2.gain.linearRampToValueAtTime(
            amp * 0.15,
            ctx.currentTime + startSec + 0.005
          );
          gain2.gain.exponentialRampToValueAtTime(
            0.0005,
            ctx.currentTime + startSec + durationSec * 0.65
          );

          osc1.connect(gain1);
          osc2.connect(gain2);
          gain1.connect(ctx.destination);
          gain2.connect(ctx.destination);

          osc1.start(ctx.currentTime + startSec);
          osc2.start(ctx.currentTime + startSec);
          osc1.stop(ctx.currentTime + startSec + durationSec);
          osc2.stop(ctx.currentTime + startSec + durationSec);
        };

        if (type === 'correct') {
          // Two-note ascending warm marimba tone (C5 -> G5)
          playWoodNote(0, 0.12, 523.25, 0.35);
          playWoodNote(0.08, 0.16, 783.99, 0.4);
        } else if (type === 'wrong') {
          // Two-note descending warm marimba tone (G4 -> Eb4)
          playWoodNote(0, 0.17, 392.0, 0.35);
          playWoodNote(0.115, 0.245, 311.13, 0.38);
        } else {
          // Subtle organic soft tap (680Hz, 28ms)
          playWoodNote(0, 0.03, 680.0, 0.12);
        }
      } catch {
        // Ignore audio context errors prior to interaction
      }
    },
    []
  );

  // Speech synthesis with LaTeX-to-spoken-math support
  const speakQuestion = useCallback(() => {
    if (!('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const currentQ = activeQuiz[current];
    if (!currentQ) return;
    const clean = latexToSpokenText(currentQ.q);
    const u = new SpeechSynthesisUtterance(clean);
    u.rate = 0.85;
    u.pitch = 1;
    u.lang = 'hi-IN';
    const voices = window.speechSynthesis.getVoices();
    const best =
      voices.find((v) => v.lang === 'hi-IN' && v.name.includes('Google')) ||
      voices.find((v) => v.lang.startsWith('hi')) ||
      voices[0];
    if (best) u.voice = best;
    window.speechSynthesis.speak(u);
  }, [activeQuiz, current]);

  // Start / Restart Quiz with Smart Non-Repeating Pool (Supports both PRACTICE and EXAM modes)
  const startQuizWithPool = useCallback(
    (
      sourcePool: QuizQuestion[],
      requestedCount?: number,
      modeOverride?: QuizMode,
      timePerQOverride?: number
    ) => {
      if (nextTimeoutRef.current) {
        clearTimeout(nextTimeoutRef.current);
      }
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
      }

      const activeMode = modeOverride ?? quizMode;
      const perQSec = Math.min(
        600,
        Math.max(5, timePerQOverride ?? questionTimeSeconds)
      );

      let n = requestedCount ?? numQuestionsInput;
      if (isNaN(n) || n < 1) n = sourcePool.length;
      if (n > sourcePool.length) n = sourcePool.length;
      setNumQuestionsInput(n);

      let selectedForRound: QuizQuestion[] = [];
      const currentPool = unplayedPoolRef.current;

      if (currentPool.length >= n) {
        selectedForRound = currentPool.splice(0, n);
      } else {
        selectedForRound = [...currentPool];
        const deficit = n - currentPool.length;
        const fullPool = shuffleArray([...sourcePool]);
        let safeRefill = fullPool.filter(
          (q) => !selectedForRound.some((sq) => sq.q === q.q)
        );
        if (safeRefill.length < deficit) safeRefill = fullPool;
        unplayedPoolRef.current = safeRefill;
        selectedForRound = selectedForRound.concat(
          unplayedPoolRef.current.splice(0, deficit)
        );
      }

      const prepared = prepareQuizQuestions(selectedForRound);
      const initialTime =
        activeMode === 'EXAM'
          ? Math.max(perQSec, prepared.length * perQSec)
          : perQSec;

      setActiveQuiz(prepared);
      setWrongQuestions([]);
      setExamAnswers({});
      setExamBookmarkedIndices(new Set());
      setExamTotalTimeSeconds(initialTime);
      setCurrent(0);
      setScore(0);
      setSkipped(0);
      setLocked(false);
      setSelectedIdx(null);
      setFeedbackHtml(null);
      setShowReview(false);
      setShowPaletteDialog(false);
      setShowSubmitConfirmDialog(false);
      setShowExitConfirmDialog(false);
      setTimeLeft(initialTime);
      setTimerPaused(false);
      setScreen('quiz');
    },
    [quizMode, questionTimeSeconds, numQuestionsInput]
  );

  // Submit Exam Mode
  const submitExam = useCallback(() => {
    if (nextTimeoutRef.current) clearTimeout(nextTimeoutRef.current);
    if ('speechSynthesis' in window) window.speechSynthesis.cancel();

    const evaluated = evaluateExamSubmission(activeQuiz, examAnswers);
    setScore(evaluated.score);
    setSkipped(evaluated.skipped);
    setWrongQuestions(evaluated.wrongQuestions);
    setLocked(false);
    setShowReview(false);
    setShowPaletteDialog(false);
    setShowSubmitConfirmDialog(false);
    playSound('correct');
    setScreen('result');
  }, [activeQuiz, examAnswers, playSound]);

  // Advance to next question or result screen (Practice Mode)
  const goNext = useCallback(() => {
    setCurrent((prevCurrent) => {
      const nextIdx = prevCurrent + 1;
      if (nextIdx >= activeQuiz.length) {
        setScreen('result');
        return prevCurrent;
      } else {
        setLocked(false);
        setSelectedIdx(null);
        setFeedbackHtml(null);
        setTimeLeft(questionTimeSeconds);
        return nextIdx;
      }
    });
  }, [activeQuiz.length, questionTimeSeconds]);

  // Handle Time Up / Skip
  const handleTimeUp = useCallback(() => {
    if (locked || screen !== 'quiz') return;

    if (quizMode === 'EXAM') {
      submitExam();
      return;
    }

    setLocked(true);
    setSkipped((s) => s + 1);
    if ('speechSynthesis' in window) window.speechSynthesis.cancel();

    const currentQ = activeQuiz[current];
    if (currentQ) {
      setWrongQuestions((prev) => [
        ...prev,
        { ...currentQ, _timedOut: true, userAnswerIndex: null },
      ]);
    }
    setFeedbackHtml({
      text: '⏰ समय खत्म / Skipped! सही जवाब हाइलाइट किया गया।',
      color: '#dc2626',
    });
    playSound('wrong');

    nextTimeoutRef.current = window.setTimeout(() => {
      goNext();
    }, 1200);
  }, [locked, screen, quizMode, submitExam, activeQuiz, current, playSound, goNext]);

  // Timer countdown effect
  useEffect(() => {
    const isPaused =
      quizMode === 'EXAM'
        ? false
        : timerPaused || showExitConfirmDialog || showQtyDialog;
    if (screen !== 'quiz' || locked || isPaused) return;

    const interval = window.setInterval(() => {
      setTimeLeft((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);

    return () => clearInterval(interval);
  }, [
    screen,
    locked,
    timerPaused,
    quizMode,
    showExitConfirmDialog,
    showQtyDialog,
  ]);

  // Trigger time-up handler when countdown reaches 0
  useEffect(() => {
    if (screen === 'quiz' && !locked && timeLeft <= 0) {
      handleTimeUp();
    }
  }, [screen, locked, timeLeft, handleTimeUp]);

  // Check Option Answer (Supports both EXAM mode selection and PRACTICE mode instant check)
  const checkAnswer = useCallback(
    (index: number) => {
      if (locked || screen !== 'quiz') return;

      if (quizMode === 'EXAM') {
        playSound('click');
        setExamAnswers((prev) => ({ ...prev, [current]: index }));
        setSelectedIdx(index);
        return;
      }

      setLocked(true);
      setSelectedIdx(index);
      if ('speechSynthesis' in window) window.speechSynthesis.cancel();

      const currentQ = activeQuiz[current];
      const correct = currentQ.a;

      if (index === correct) {
        setScore((s) => s + 1);
        setScorePop(false);
        if (scorePopTimeoutRef.current) {
          clearTimeout(scorePopTimeoutRef.current);
        }
        scorePopTimeoutRef.current = window.setTimeout(() => {
          setScorePop(true);
          scorePopTimeoutRef.current = null;
        }, 10);
        setFeedbackHtml({ text: '✅ सही जवाब!', color: '#16a34a' });
        playSound('correct');
      } else {
        setWrongQuestions((prev) => [
          ...prev,
          { ...currentQ, _timedOut: false, userAnswerIndex: index },
        ]);
        setFeedbackHtml({ text: '❌ गलत जवाब!', color: '#dc2626' });
        playSound('wrong');
      }

      nextTimeoutRef.current = window.setTimeout(() => {
        goNext();
      }, 1200);
    },
    [locked, screen, quizMode, activeQuiz, current, playSound, goNext]
  );

  // Exam Mode Navigation Helpers
  const clearExamOption = useCallback(() => {
    if (quizMode !== 'EXAM' || screen !== 'quiz') return;
    playSound('click');
    setExamAnswers((prev) => {
      const next = { ...prev };
      delete next[current];
      return next;
    });
    setSelectedIdx(null);
  }, [quizMode, screen, current, playSound]);

  const goToExamQuestion = useCallback(
    (targetIdx: number) => {
      if (quizMode !== 'EXAM' || screen !== 'quiz') return;
      const clamped = Math.max(0, Math.min(activeQuiz.length - 1, targetIdx));
      if (clamped === current) return;
      if ('speechSynthesis' in window) window.speechSynthesis.cancel();
      playSound('click');
      setCurrent(clamped);
      setSelectedIdx(examAnswers[clamped] ?? null);
    },
    [quizMode, screen, activeQuiz.length, current, examAnswers, playSound]
  );

  // Toggle Bookmark for Attempt Later (Separate from Starred Important Questions)
  const toggleExamBookmark = useCallback(
    (questionIdx: number) => {
      playSound('click');
      setExamBookmarkedIndices((prev) => {
        const next = new Set(prev);
        if (next.has(questionIdx)) {
          next.delete(questionIdx);
        } else {
          next.add(questionIdx);
        }
        return next;
      });
    },
    [playSound]
  );

  // Import multi-quiz or single-quiz text into Quiz Database
  const handleImportQuizzesFromText = useCallback(
    (rawText: string, fallbackName = 'Imported Quiz') => {
      const res = importQuizzesFromText(
        rawText,
        savedQuizzes,
        starredQuestions,
        fallbackName
      );
      if (res.importedCount > 0) {
        persistQuizzes(res.quizzes);
        persistStarred(res.starredQuestions);
        playSound('correct');
        showToast(`✅ ${res.importedCount} Quiz(zes) Imported into Database!`);
      } else {
        showToast('⚠️ कोई Valid Quiz डेटा नहीं मिला।');
      }
      return res.importedCount;
    },
    [
      savedQuizzes,
      starredQuestions,
      persistQuizzes,
      persistStarred,
      playSound,
      showToast,
    ]
  );

  // Create Quiz From Textarea Input
  const createQuizFromText = useCallback(
    (overrideText?: string) => {
      const txt = (overrideText ?? quizInput).trim();
      if (!txt) {
        showToast('कृपया Quiz Data Paste करें');
        return;
      }

      const { validQuestions, skippedQuestions } = parseQuizText(txt);

      if (validQuestions.length === 0) {
        showToast('कोई भी Valid Question नहीं मिला');
        return;
      }

      if (skippedQuestions > 0) {
        showToast(
          `${skippedQuestions} खराब Questions Skip कर दिए गए। ${validQuestions.length} Questions Quiz में शामिल किए गए।`
        );
      }

      setQuiz(validQuestions);
      unplayedPoolRef.current = shuffleArray([...validQuestions]);
      setNumQuestionsInput(validQuestions.length);
      startQuizWithPool(validQuestions, validQuestions.length);
    },
    [quizInput, showToast, startQuizWithPool]
  );

  // Copy LaTeX-supported AI Prompt
  const copyQuizPrompt = useCallback(
    (options?: BuildAiPromptOptions) => {
      const promptText = buildAiPrompt(options);
      const onSuccess = () => {
        playSound('click');
        setCopySuccess(true);
        window.setTimeout(() => setCopySuccess(false), 2000);
        showToast(
          `✅ AI Prompt Copied (${options?.questionCount ?? 50} Qs • ${
            options?.difficulty ?? 'Medium'
          } • ${options?.languageStyle ?? 'Hinglish'})`
        );
      };

      const fallbackCopy = () => {
        try {
          const ta = document.createElement('textarea');
          ta.value = promptText;
          ta.style.position = 'fixed';
          ta.style.opacity = '0';
          document.body.appendChild(ta);
          ta.focus();
          ta.select();
          document.execCommand('copy');
          document.body.removeChild(ta);
          onSuccess();
        } catch {
          showToast('Clipboard access नहीं मिला।');
        }
      };

      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard
          .writeText(promptText)
          .then(onSuccess)
          .catch(fallbackCopy);
      } else {
        fallbackCopy();
      }
    },
    [playSound, showToast]
  );

  // Save Quiz to Quiz Database
  const saveQuizToStorage = useCallback(() => {
    const txt = quizInput.trim();
    if (!txt) {
      showToast('कृपया पहले quiz data paste करें।');
      return;
    }
    const { validQuestions } = parseQuizText(txt);
    if (validQuestions.length === 0) {
      showToast('कोई भी Valid Question नहीं मिला।');
      return;
    }
    const name =
      quizName.trim() || 'Quiz ' + new Date().toLocaleDateString('hi-IN');
    const category = quizCategory.trim() || 'General';
    const updated = upsertQuizInList(
      savedQuizzes,
      name,
      category,
      txt,
      validQuestions.length
    );
    persistQuizzes(updated);
    setSaveSuccess(true);
    window.setTimeout(() => setSaveSuccess(false), 1500);
    showToast(`✅ Quiz Saved: ${name} (${category})`);
  }, [
    quizInput,
    quizName,
    quizCategory,
    savedQuizzes,
    persistQuizzes,
    showToast,
  ]);

  // Delete Saved Quiz by ID
  const deleteSavedQuiz = useCallback(
    (quizId: string) => {
      const updated = savedQuizzes.filter((q) => q.id !== quizId);
      persistQuizzes(updated);
      showToast('🗑 Quiz deleted');
    },
    [savedQuizzes, persistQuizzes, showToast]
  );

  // Clear All Quiz Database & Starred Questions
  const clearAllDatabase = useCallback(() => {
    persistQuizzes([]);
    persistStarred([]);
    setSelectedCategoryFilter('All');
    showToast('🧹 All saved quizzes & starred questions cleared');
  }, [persistQuizzes, persistStarred, showToast]);

  // Select Saved Quiz from Database
  const selectSavedQuiz = useCallback(
    (saved: SavedQuiz, startImmediately: boolean) => {
      if (startImmediately) {
        const { validQuestions } = parseQuizText(saved.data);
        if (validQuestions.length === 0) {
          showToast('कोई भी Valid Question नहीं मिला');
          return;
        }
        setQuiz(validQuestions);
        unplayedPoolRef.current = shuffleArray([...validQuestions]);
        setNumQuestionsInput(validQuestions.length);
        startQuizWithPool(validQuestions, validQuestions.length);
      } else {
        setQuizInput(saved.data);
        setQuizName(saved.name);
        setQuizCategory(saved.category || 'General');
        setScreen('home');
        showToast(`📂 Loaded: ${saved.name}`);
      }
    },
    [showToast, startQuizWithPool]
  );

  // Export Offline HTML with KaTeX
  const exportOfflineHtml = useCallback(
    (item: SavedQuiz) => {
      const safeName =
        item.name.replace(/[^a-z0-9]/gi, '_').toLowerCase() || 'quiz';
      const finalHTML = buildOfflineQuizHtml(item);

      const blob = new Blob([finalHTML], { type: 'text/html' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${safeName}_offline.html`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      showToast(`✅ Offline Quiz Exported: ${item.name}`);
    },
    [showToast]
  );

  // Starred Questions Bank Helpers
  const isQuestionStarred = useCallback(
    (q?: QuizQuestion | null) => {
      if (!q) return false;
      const target = q.q.trim().toLowerCase();
      return starredQuestions.some(
        (item) => item.q.trim().toLowerCase() === target
      );
    },
    [starredQuestions]
  );

  const toggleStarQuestion = useCallback(
    (q: QuizQuestion) => {
      const target = q.q.trim();
      if (!target) return;
      playSound('click');
      const existingIdx = starredQuestions.findIndex(
        (item) => item.q.trim().toLowerCase() === target.toLowerCase()
      );
      if (existingIdx >= 0) {
        const updated = starredQuestions.filter((_, i) => i !== existingIdx);
        persistStarred(updated);
      } else {
        const cleanQ: QuizQuestion = {
          q: target,
          o: [...q.o],
          a: Math.max(0, Math.min(q.o.length - 1, q.a)),
        };
        persistStarred([cleanQ, ...starredQuestions]);
      }
    },
    [starredQuestions, persistStarred, playSound]
  );

  const removeStarredQuestion = useCallback(
    (questionText: string) => {
      const target = questionText.trim().toLowerCase();
      const updated = starredQuestions.filter(
        (item) => item.q.trim().toLowerCase() !== target
      );
      persistStarred(updated);
    },
    [starredQuestions, persistStarred]
  );

  const startStarredQuestionsQuiz = useCallback(() => {
    if (starredQuestions.length === 0) return;
    setQuiz(starredQuestions);
    unplayedPoolRef.current = shuffleArray([...starredQuestions]);
    setNumQuestionsInput(starredQuestions.length);
    startQuizWithPool(starredQuestions, starredQuestions.length);
  }, [starredQuestions, startQuizWithPool]);

  // Retry Wrong Answers
  const retryWrongAnswers = useCallback(() => {
    if (wrongQuestions.length === 0) return;
    const src = wrongQuestions.map((q) => {
      const c = { ...q };
      delete c._timedOut;
      delete c.userAnswerIndex;
      return c;
    });
    const prepared = prepareQuizQuestions(src);
    const initialTime =
      quizMode === 'EXAM'
        ? Math.max(questionTimeSeconds, prepared.length * questionTimeSeconds)
        : questionTimeSeconds;

    setActiveQuiz(prepared);
    setNumQuestionsInput(src.length);
    setWrongQuestions([]);
    setExamAnswers({});
    setExamBookmarkedIndices(new Set());
    setExamTotalTimeSeconds(initialTime);
    setCurrent(0);
    setScore(0);
    setSkipped(0);
    setLocked(false);
    setSelectedIdx(null);
    setFeedbackHtml(null);
    setShowReview(false);
    setTimeLeft(initialTime);
    setTimerPaused(false);
    setScreen('quiz');
  }, [wrongQuestions, quizMode, questionTimeSeconds]);

  // Go Home (Immediate)
  const goHome = useCallback(() => {
    if (nextTimeoutRef.current) clearTimeout(nextTimeoutRef.current);
    if ('speechSynthesis' in window) window.speechSynthesis.cancel();
    setShowPaletteDialog(false);
    setShowSubmitConfirmDialog(false);
    setShowExitConfirmDialog(false);
    setShowQtyDialog(false);
    setScreen('home');
  }, []);

  // Request Exit from Quiz / Exam with Confirmation Dialog
  const requestExitQuiz = useCallback(() => {
    playSound('click');
    if (screen === 'quiz') {
      setShowExitConfirmDialog(true);
    } else {
      goHome();
    }
  }, [screen, playSound, goHome]);

  // Toggle Fullscreen
  const toggleFullscreen = useCallback(() => {
    if (!document.fullscreenElement) {
      document.documentElement
        .requestFullscreen()
        .then(() => setIsFullscreen(true))
        .catch(() => {});
    } else {
      if (document.exitFullscreen) {
        document
          .exitFullscreen()
          .then(() => setIsFullscreen(false))
          .catch(() => {});
      }
    }
  }, []);

  // Drag & Drop File Upload
  const handleDrop = (e: React.DragEvent<HTMLTextAreaElement>) => {
    e.preventDefault();
    setIsDragOver(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      if (
        file.name.endsWith('.json') ||
        file.name.endsWith('.txt') ||
        file.type.match('text.*')
      ) {
        const reader = new FileReader();
        reader.onload = (event) => {
          const content = String(event.target?.result || '');
          const fileNameWithoutExt = file.name.replace(/\.[^/.]+$/, '');
          if (/===\s*QUIZ\s*:/i.test(content)) {
            handleImportQuizzesFromText(content, fileNameWithoutExt);
          } else {
            setQuizInput(content);
            setQuizName(fileNameWithoutExt);
          }
        };
        reader.readAsText(file);
      } else {
        showToast('Error: कृपया केवल .txt या .json फ़ाइलें ही ड्रॉप करें।');
      }
    }
  };

  // Global Keyboard & Right-Click Shortcuts
  useEffect(() => {
    const handleContextMenu = (e: MouseEvent) => {
      if (screen === 'quiz' && quizMode === 'PRACTICE') {
        e.preventDefault();
        setTimerPaused((p) => !p);
      }
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      const key = e.key.toLowerCase();
      const target = e.target as HTMLElement | null;
      const isInput =
        target && (target.tagName === 'TEXTAREA' || target.tagName === 'INPUT');

      const anyModalOpen =
        showCopyPromptDialog ||
        showTimerDialog ||
        showQtyDialog ||
        showPaletteDialog ||
        showSubmitConfirmDialog ||
        showExitConfirmDialog;

      if (anyModalOpen) {
        if (key === 'escape') {
          e.preventDefault();
          if (showSubmitConfirmDialog) setShowSubmitConfirmDialog(false);
          else if (showPaletteDialog) setShowPaletteDialog(false);
          else if (showExitConfirmDialog) setShowExitConfirmDialog(false);
          else if (showQtyDialog) setShowQtyDialog(false);
          else if (showTimerDialog) setShowTimerDialog(false);
          else if (showCopyPromptDialog) setShowCopyPromptDialog(false);
        }
        return;
      }

      if (key === 'q' && (screen === 'quiz' || screen === 'result') && !isInput) {
        e.preventDefault();
        setShowQtyDialog(true);
        return;
      }

      if (isInput) return;

      if (e.code === 'Space' && screen === 'quiz') {
        e.preventDefault();
        speakQuestion();
        return;
      }

      if (key === 'n' && screen === 'quiz' && !locked) {
        if (quizMode === 'EXAM') {
          if (current < activeQuiz.length - 1) {
            goToExamQuestion(current + 1);
          }
        } else {
          handleTimeUp();
        }
        return;
      }

      if (key === 'p' && screen === 'quiz' && quizMode === 'PRACTICE') {
        setTimerPaused((p) => !p);
        return;
      }

      if (key === 'escape' && screen !== 'home') {
        if (document.querySelector('[role="dialog"]')) {
          return;
        }
        e.preventDefault();
        if (screen === 'quiz') {
          setShowExitConfirmDialog(true);
        } else {
          goHome();
        }
        return;
      }

      if (key === 'c' && screen === 'home') {
        e.preventDefault();
        setShowCopyPromptDialog(true);
        return;
      }

      if (e.key === 'Enter' && screen === 'home') {
        createQuizFromText();
        return;
      }

      if (key === 'h' && screen === 'result') {
        goHome();
        return;
      }

      if (key === 't' && screen === 'result') {
        startQuizWithPool(quiz);
        return;
      }

      if (key === 'f') {
        toggleFullscreen();
        return;
      }

      if (!locked && ['1', '2', '3', '4'].includes(e.key) && screen === 'quiz') {
        const idx = parseInt(e.key, 10) - 1;
        checkAnswer(idx);
        return;
      }

      if (key === 's' && (screen === 'quiz' || screen === 'result')) {
        startQuizWithPool(quiz);
        return;
      }

      if (key === 'w' && screen === 'result' && wrongQuestions.length > 0) {
        setShowReview((r) => !r);
        return;
      }

      if (key === 'r' && screen === 'result' && wrongQuestions.length > 0) {
        retryWrongAnswers();
        return;
      }

      if (key === 'm' && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel();
      }
    };

    window.addEventListener('contextmenu', handleContextMenu);
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('contextmenu', handleContextMenu);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [
    screen,
    quizMode,
    locked,
    current,
    activeQuiz.length,
    quiz,
    wrongQuestions.length,
    showCopyPromptDialog,
    showTimerDialog,
    showQtyDialog,
    showPaletteDialog,
    showSubmitConfirmDialog,
    showExitConfirmDialog,
    speakQuestion,
    goToExamQuestion,
    handleTimeUp,
    goHome,
    createQuizFromText,
    startQuizWithPool,
    toggleFullscreen,
    checkAnswer,
    retryWrongAnswers,
  ]);

  // Derived result metrics
  const totalQuestions = activeQuiz.length;
  const accuracy =
    totalQuestions > 0 ? Math.round((score / totalQuestions) * 100) : 0;
  const wrongCount = wrongQuestions.filter((q) => !q._timedOut).length;
  const timedOutCount = wrongQuestions.filter((q) => q._timedOut).length;
  const { grade, msg: gradeMsg } = getGrade(accuracy);
  const ringColors =
    accuracy >= 80
      ? ['#16a34a', '#15803d']
      : accuracy >= 60
      ? ['#2563eb', '#1d4ed8']
      : accuracy >= 40
      ? ['#f59e0b', '#d97706']
      : ['#dc2626', '#b91c1c'];

  const currentQuestion = activeQuiz[current];
  const isExamMode = quizMode === 'EXAM';
  const maxTimeForBar = isExamMode
    ? Math.max(1, examTotalTimeSeconds)
    : Math.max(1, questionTimeSeconds);
  const timerPct = Math.min(100, Math.max(0, (timeLeft / maxTimeForBar) * 100));
  const timerStatusClass =
    timeLeft <= 5 ? 'danger' : timeLeft <= 10 ? 'warn' : '';
  const isCurrentQuestionStarred = isQuestionStarred(currentQuestion);

  return (
    <main className="min-h-screen w-full">
      {toastMessage && (
        <div
          role="status"
          aria-live="polite"
          className="fixed top-4 right-4 z-50 px-5 py-3 rounded-xl bg-slate-900 text-white text-sm font-semibold shadow-xl border border-slate-700 transition-all"
        >
          {toastMessage}
        </div>
      )}

      {/* Copy AI Prompt Modal */}
      {showCopyPromptDialog && (
        <CopyPromptDialog
          onDismiss={() => setShowCopyPromptDialog(false)}
          onCopyPrompt={(opts) => copyQuizPrompt(opts)}
        />
      )}

      {/* Question Timing Modal */}
      {showTimerDialog && (
        <QuestionTimingDialog
          initialSeconds={questionTimeSeconds}
          onDismiss={() => setShowTimerDialog(false)}
          onApply={(sec) => {
            setQuestionTimeSeconds(sec);
            localStorage.setItem(STORAGE_KEY_QUESTION_TIME, String(sec));
            playSound('click');
            showToast(`⏱️ Question Timing set to ${sec}s`);
          }}
        />
      )}

      {/* Question Quantity Selector Modal */}
      {showQtyDialog && (
        <QuestionQtyDialog
          currentQty={numQuestionsInput}
          maxQuestions={quiz.length}
          onDismiss={() => setShowQtyDialog(false)}
          onApply={(qty) => {
            setNumQuestionsInput(qty);
            playSound('click');
            startQuizWithPool(quiz, qty);
          }}
        />
      )}

      {/* Exam Question Palette Modal */}
      {showPaletteDialog && isExamMode && (
        <ExamPaletteDialog
          totalCount={activeQuiz.length}
          currentIndex={current}
          examAnswers={examAnswers}
          bookmarkedIndices={examBookmarkedIndices}
          onGoToQuestion={goToExamQuestion}
          onDismiss={() => setShowPaletteDialog(false)}
          onRequestSubmit={() => setShowSubmitConfirmDialog(true)}
        />
      )}

      {/* Exam Submit Confirmation Modal */}
      {showSubmitConfirmDialog && isExamMode && (
        <ExamSubmitConfirmDialog
          totalCount={activeQuiz.length}
          answeredCount={Object.keys(examAnswers).length}
          bookmarkedCount={examBookmarkedIndices.size}
          onDismiss={() => setShowSubmitConfirmDialog(false)}
          onConfirmSubmit={submitExam}
        />
      )}

      {/* Exit Quiz / Exam Confirmation Modal */}
      {showExitConfirmDialog && (
        <ExitQuizConfirmDialog
          isExamMode={isExamMode}
          currentIndex={current}
          totalCount={activeQuiz.length}
          onDismiss={() => setShowExitConfirmDialog(false)}
          onConfirmExit={goHome}
        />
      )}

      {/* ═══════════════════════════════════════════════
          HOME PAGE
      ═══════════════════════════════════════════════ */}
      {screen === 'home' && (
        <div
          id="homePage"
          className="min-h-screen p-4 sm:p-6 flex flex-col justify-center items-center"
        >
          <div
            className="w-full max-w-7xl flex flex-col py-6 gap-4"
            style={{ minHeight: '90vh' }}
          >
            {/* Header Banner */}
            <div className="flex flex-col text-center bg-gradient-to-br from-blue-600 via-blue-700 to-indigo-800 p-6 rounded-2xl shadow-xl relative overflow-hidden border border-blue-400">
              <div className="absolute -top-5 -right-5 text-white opacity-10 text-8xl rotate-12 pointer-events-none">
                🧩
              </div>
              <div className="absolute -bottom-6 -left-6 text-white opacity-10 text-7xl -rotate-12 pointer-events-none">
                ✨
              </div>

              {/* Top-Right Quick Controls: Timing & Dark Mode Toggle */}
              <div className="absolute top-3.5 right-4 z-20 flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setShowTimerDialog(true)}
                  data-testid="question-timing-button"
                  title={`Question Timing (${questionTimeSeconds}s)`}
                  className="px-3 py-1.5 rounded-xl bg-white/20 hover:bg-white/30 text-white text-xs font-bold flex items-center gap-1.5 backdrop-blur-sm cursor-pointer transition"
                >
                  <span>{questionTimeSeconds}s</span>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setIsDark((d) => !d);
                    playSound('click');
                  }}
                  data-testid="toggle-dark-mode-button"
                  title={isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
                  aria-label={
                    isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode'
                  }
                  className="px-3 py-1.5 rounded-xl bg-white/20 hover:bg-white/30 text-white text-xs font-bold flex items-center justify-center backdrop-blur-sm cursor-pointer transition"
                >
                  {isDark ? 'Light' : 'Dark'}
                </button>
              </div>

              <h1 className="text-3xl font-extrabold text-white mb-1 relative z-10 drop-shadow-lg">
                📝 Create a Quiz
              </h1>
              <p className="text-blue-100 font-medium relative z-10 text-sm">
                AI से data generate करें, यहाँ paste करें, Drag &amp; Drop करें और quiz शुरू करें।
              </p>
            </div>

            <textarea
              id="quizInput"
              aria-label="Quiz Data Input"
              wrap="off"
              rows={12}
              value={quizInput}
              onChange={(e) => {
                const val = e.target.value;
                if (
                  /===\s*QUIZ\s*:/i.test(val) &&
                  Math.abs(val.length - quizInput.length) > 20
                ) {
                  handleImportQuizzesFromText(val);
                  return;
                }
                setQuizInput(val);
              }}
              onDragOver={(e) => {
                e.preventDefault();
                setIsDragOver(true);
              }}
              onDragLeave={(e) => {
                e.preventDefault();
                setIsDragOver(false);
              }}
              onDrop={handleDrop}
              placeholder='{ q: "Question", o:["A","B","C","D"], a:0 },'
              className={`w-full p-4 border rounded-xl shadow-sm resize-none outline-none text-sm leading-relaxed overflow-x-auto ${
                isDragOver ? 'drag-over' : ''
              }`}
              style={{ flexGrow: 1, minHeight: '200px' }}
            />

            {/* Practice Mode vs Exam Mode Selector + Action Controls */}
            <div className="flex gap-2.5 items-center justify-center flex-wrap">
              {/* Mode Selector Pill Group */}
              <div className="flex items-center bg-white/90 dark:bg-slate-800 p-1 rounded-xl border border-slate-300 dark:border-slate-700 shadow-sm">
                <button
                  type="button"
                  data-testid="mode-practice-button"
                  onClick={() => {
                    setQuizMode('PRACTICE');
                    localStorage.setItem(STORAGE_KEY_QUIZ_MODE, 'PRACTICE');
                    playSound('click');
                  }}
                  className={`px-3 py-2 rounded-lg text-xs font-extrabold flex items-center gap-1.5 cursor-pointer transition whitespace-nowrap ${
                    quizMode === 'PRACTICE'
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'text-slate-600 dark:text-slate-300 hover:text-slate-900'
                  }`}
                >
                  <span>Practice Mode</span>
                </button>

                <button
                  type="button"
                  data-testid="mode-exam-button"
                  onClick={() => {
                    setQuizMode('EXAM');
                    localStorage.setItem(STORAGE_KEY_QUIZ_MODE, 'EXAM');
                    playSound('click');
                  }}
                  className={`px-3 py-2 rounded-lg text-xs font-extrabold flex items-center gap-1.5 cursor-pointer transition whitespace-nowrap ${
                    quizMode === 'EXAM'
                      ? 'bg-indigo-600 text-white shadow-sm'
                      : 'text-slate-600 dark:text-slate-300 hover:text-slate-900'
                  }`}
                >
                  <span>Exam Mode</span>
                </button>
              </div>

              <input
                id="quizName"
                type="text"
                aria-label="Quiz का नाम (optional)"
                value={quizName}
                onChange={(e) => setQuizName(e.target.value)}
                placeholder="Quiz का नाम (optional)"
                className="flex-1 min-w-[140px] max-w-[200px] px-3.5 py-2.5 rounded-xl border border-gray-300 dark:border-slate-700 text-sm font-medium outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-200 bg-white/90 dark:bg-slate-800 dark:text-white"
              />

              <input
                id="quizCategory"
                type="text"
                aria-label="Category (optional)"
                value={quizCategory}
                onChange={(e) => setQuizCategory(e.target.value)}
                placeholder="Category (e.g. Science, Math)"
                className="flex-1 min-w-[125px] max-w-[175px] px-3.5 py-2.5 rounded-xl border border-gray-300 dark:border-slate-700 text-sm font-medium outline-none focus:border-indigo-400 focus:ring-2 focus:ring-indigo-200 bg-white/90 dark:bg-slate-800 dark:text-white"
              />

              <button
                type="button"
                onClick={() => setShowCopyPromptDialog(true)}
                id="copyPromptBtn"
                data-testid="open-copy-prompt-dialog-button"
                className={`px-4 py-2.5 rounded-xl font-bold text-white text-sm transition-all shadow bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 hover:-translate-y-0.5 cursor-pointer whitespace-nowrap ${
                  copySuccess ? 'from-green-600 to-emerald-700' : ''
                }`}
              >
                {copySuccess ? 'Copied!' : 'Copy Prompt'}
              </button>

              <button
                type="button"
                onClick={saveQuizToStorage}
                className={`px-4 py-2.5 rounded-xl font-bold text-white text-sm transition-all shadow bg-gradient-to-r ${
                  saveSuccess
                    ? 'from-green-600 to-emerald-700'
                    : 'from-violet-500 to-purple-600 hover:from-violet-600 hover:to-purple-700'
                } hover:-translate-y-0.5 cursor-pointer whitespace-nowrap`}
              >
                {saveSuccess ? 'Saved!' : 'Save'}
              </button>

              <button
                type="button"
                onClick={() => setScreen('database')}
                id="openQuizDatabaseBtn"
                className="px-4 py-2.5 rounded-xl font-bold text-white text-sm transition-all shadow bg-gradient-to-r from-sky-600 to-blue-700 hover:from-sky-700 hover:to-blue-800 hover:-translate-y-0.5 cursor-pointer whitespace-nowrap"
              >
                Database ({savedQuizzes.length})
              </button>

              <button
                type="button"
                onClick={() => createQuizFromText()}
                className="px-6 py-2.5 rounded-xl font-bold text-white text-base transition-all shadow bg-gradient-to-r from-blue-600 to-indigo-700 hover:from-blue-700 hover:to-indigo-800 hover:-translate-y-0.5 cursor-pointer whitespace-nowrap"
              >
                Start
              </button>
            </div>

            <div className="mt-2 p-3 bg-white/70 dark:bg-slate-800/80 rounded-xl border border-blue-200 dark:border-slate-700 text-center w-full max-w-3xl mx-auto shadow-sm">
              <div className="flex flex-wrap justify-center gap-x-5 gap-y-2 text-xs font-semibold text-gray-600 dark:text-slate-300">
                <span>⌨️ शॉर्टकट्स:</span>
                <span>
                  <b className="text-blue-600 dark:text-blue-400">Enter</b> : Start
                </span>
                <span>
                  <b className="text-blue-600 dark:text-blue-400">C</b> : Copy Prompt
                </span>
                <span>
                  <b className="text-blue-600 dark:text-blue-400">Drag &amp; Drop</b> : Load File
                </span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ═══════════════════════════════════════════════
          QUIZ DATABASE SCREEN
      ═══════════════════════════════════════════════ */}
      {screen === 'database' && (
        <QuizDatabaseScreen
          savedQuizzes={savedQuizzes}
          starredQuestions={starredQuestions}
          selectedCategoryFilter={selectedCategoryFilter}
          searchQuery={savedQuizSearchQuery}
          onSelectCategoryFilter={setSelectedCategoryFilter}
          onSearchQueryChange={setSavedQuizSearchQuery}
          onBackHome={() => setScreen('home')}
          onSelectSavedQuiz={selectSavedQuiz}
          onDeleteSavedQuiz={deleteSavedQuiz}
          onClearAllDatabase={clearAllDatabase}
          onImportText={handleImportQuizzesFromText}
          onExportOfflineHtml={exportOfflineHtml}
          onStartStarredQuiz={startStarredQuestionsQuiz}
          onRemoveStarredQuestion={removeStarredQuestion}
          onClearAllStarred={() => {
            persistStarred([]);
            showToast('🧹 Cleared all starred questions');
          }}
          onShowToast={showToast}
        />
      )}

      {/* ═══════════════════════════════════════════════
          QUIZ & RESULT WRAPPER
      ═══════════════════════════════════════════════ */}
      {(screen === 'quiz' || screen === 'result') && (
        <div
          id="mainQuizWrapper"
          className="w-full min-h-screen flex items-center justify-center p-4"
        >
          <div className="quiz-card" id="mainQuiz">
            <div className="header">
              <div className="header-row">
                <div className="quiz-controls">
                  {/* Home / Exit Button with Confirmation */}
                  <button
                    type="button"
                    id="quizHomeBtn"
                    data-testid="quiz-home-button"
                    onClick={requestExitQuiz}
                    title="Exit to Home (Esc)"
                    aria-label="Exit to Home"
                    className="!w-auto px-3 py-1.5 rounded-lg bg-white/20 hover:bg-white/30 text-white text-lg flex items-center justify-center cursor-pointer transition"
                  >
                    🏠
                  </button>

                  {/* Question Quantity Selector Pill Button (Opens QuestionQtyDialog) */}
                  <button
                    type="button"
                    id="numQuestions"
                    data-testid="quiz-qty-selector-button"
                    onClick={() => setShowQtyDialog(true)}
                    title="Select Number of Questions (Press Q)"
                    className="!w-auto px-3 py-1.5 rounded-lg bg-white/20 hover:bg-white/30 text-white text-sm font-bold flex items-center gap-1.5 cursor-pointer transition"
                  >
                    <span>Q: {numQuestionsInput}</span>
                  </button>

                  <button
                    type="button"
                    id="startBtn"
                    onClick={() => startQuizWithPool(quiz, numQuestionsInput)}
                    className="!w-auto px-3 py-1.5 rounded-lg bg-white/20 hover:bg-white/30 text-white text-sm font-bold flex items-center justify-center cursor-pointer transition"
                  >
                    Restart (S)
                  </button>
                </div>

                <div className="title">
                  {isExamMode
                    ? '🏆 Raman Quiz • Exam Mode 🏆'
                    : '🏆 Raman Quiz Challenge 🏆'}
                </div>

                <div
                  style={{ display: 'flex', alignItems: 'center', gap: '10px' }}
                >
                  {isExamMode && screen === 'quiz' ? (
                    <button
                      type="button"
                      data-testid="exam-palette-button"
                      onClick={() => setShowPaletteDialog(true)}
                      className="score-box !px-4 !py-1.5 !text-sm flex items-center gap-2 cursor-pointer hover:scale-105 transition !text-blue-700 font-extrabold shadow-md"
                      title="Open Question Palette"
                    >
                      <span>
                        Palette ({Object.keys(examAnswers).length}/{activeQuiz.length})
                      </span>
                    </button>
                  ) : (
                    <div
                      className={`score-box ${scorePop ? 'pop' : ''}`}
                      id="scoreBox"
                    >
                      Score: {score}
                    </div>
                  )}

                  <button
                    type="button"
                    onClick={() => {
                      setIsDark((d) => !d);
                      playSound('click');
                    }}
                    title={isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
                    aria-label="Toggle Dark Mode"
                    className="px-3 py-1.5 rounded-lg bg-white/20 hover:bg-white/30 text-white text-xs font-bold cursor-pointer transition"
                  >
                    {isDark ? 'Light' : 'Dark'}
                  </button>

                  <button
                    type="button"
                    id="fullscreenBtn"
                    onClick={toggleFullscreen}
                    title={
                      isFullscreen
                        ? 'Exit Fullscreen (F)'
                        : 'Toggle Fullscreen (F)'
                    }
                    aria-label="Toggle Fullscreen"
                  >
                    {isFullscreen ? '✖' : '⛶'}
                  </button>
                </div>
              </div>
            </div>

            {screen === 'quiz' && currentQuestion && (
              <div id="quizScreen">
                <div className="progress-area">
                  <div className="progress-info">
                    <span id="questionNumber">
                      प्रश्न {current + 1} / {activeQuiz.length}
                    </span>

                    <span
                      className="hidden sm:inline-block"
                      style={{
                        fontSize: '11px',
                        color: 'var(--muted)',
                        textAlign: 'center',
                        fontWeight: 500,
                      }}
                    >
                      ⌨️ Space: आवाज़ | N: Next | P / Right-Click: Pause | Q:
                      Change Qty | S: Restart | Esc: Home
                    </span>

                    <span id="remaining">
                      {activeQuiz.length - current} बाकी
                    </span>
                  </div>
                  <div className="progress-bar">
                    <div
                      className="progress-fill"
                      id="progressFill"
                      style={{
                        width: `${(current / activeQuiz.length) * 100}%`,
                      }}
                    />
                  </div>
                </div>

                {/* Timer Row */}
                <div className="timer-row">
                  {isExamMode ? (
                    <span
                      style={{ fontSize: '18px', userSelect: 'none' }}
                      title="Exam Timer"
                    >
                      ⏱️
                    </span>
                  ) : (
                    <button
                      type="button"
                      id="pauseTimerBtn"
                      onClick={() => setTimerPaused((p) => !p)}
                      style={{
                        fontSize: '12px',
                        fontWeight: 700,
                        color: 'var(--text)',
                        cursor: 'pointer',
                        userSelect: 'none',
                        background: 'none',
                        border: 'none',
                        padding: 0,
                      }}
                      title={
                        timerPaused
                          ? 'Resume Timer (P / Right-Click)'
                          : 'Pause Timer (P / Right-Click)'
                      }
                      aria-label={timerPaused ? 'Resume Timer' : 'Pause Timer'}
                    >
                      {timerPaused ? 'Resume' : 'Pause'}
                    </button>
                  )}

                  <div className="timer-bar-wrap">
                    <div
                      className={`timer-bar ${timerStatusClass}`}
                      id="timerBar"
                      style={{
                        width: `${timerPct}%`,
                        opacity: !isExamMode && timerPaused ? 0.4 : 1,
                      }}
                    />
                  </div>
                  <span
                    className={`timer-count ${timerStatusClass}`}
                    id="timerCount"
                    style={{
                      opacity: !isExamMode && timerPaused ? 0.4 : 1,
                      minWidth: isExamMode ? '48px' : '28px',
                    }}
                  >
                    {isExamMode ? formatExamTime(timeLeft) : timeLeft}
                  </span>
                </div>

                <div className="content">
                  <div className="question-wrapper">
                    <div className="question" id="question">
                      <LatexText text={currentQuestion.q} />
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      {/* Star Button: Specifically for saving Important Questions to Starred Questions Bank in Database */}
                      <button
                        type="button"
                        data-testid="quiz-star-question-button"
                        onClick={() => toggleStarQuestion(currentQuestion)}
                        title={
                          isCurrentQuestionStarred
                            ? 'Remove from Important Questions (Starred Bank)'
                            : 'Star as Important Question (Save to Database)'
                        }
                        aria-label={
                          isCurrentQuestionStarred
                            ? 'Remove from Important Questions'
                            : 'Star as Important Question'
                        }
                        className={`px-3 h-9 rounded-full border flex items-center justify-center text-xs font-bold cursor-pointer transition ${
                          isCurrentQuestionStarred
                            ? 'bg-amber-100 border-amber-400 text-amber-600 shadow-sm'
                            : 'bg-slate-100 dark:bg-slate-800 border-slate-300 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:text-amber-500'
                        }`}
                      >
                        {isCurrentQuestionStarred ? 'Starred' : 'Star'}
                      </button>
                      <button
                        type="button"
                        id="speakBtn"
                        onClick={speakQuestion}
                        className="speak-btn"
                        title="सवाल सुनें (Space)"
                        aria-label="सवाल सुनें"
                      >
                        Speak
                      </button>
                    </div>
                  </div>

                  <div className="options" id="options">
                    {currentQuestion.o.map((opt, index) => {
                      const isCorrect = index === currentQuestion.a;
                      const isSelectedByPlayer = isExamMode
                        ? examAnswers[current] === index
                        : selectedIdx === index;

                      const showCorrect = !isExamMode && locked && isCorrect;
                      const isWrongSelected =
                        !isExamMode && locked && isSelectedByPlayer && !isCorrect;
                      const isExamSelected = isExamMode && isSelectedByPlayer;

                      const optionClasses = [
                        'option',
                        !isExamMode && locked ? 'disabled' : '',
                        showCorrect ? 'correct' : '',
                        isWrongSelected ? 'wrong' : '',
                        isExamSelected ? 'exam-selected' : '',
                      ]
                        .filter(Boolean)
                        .join(' ');

                      return (
                        <button
                          key={index}
                          type="button"
                          className={optionClasses}
                          disabled={!isExamMode && locked}
                          onMouseEnter={() => {
                            if (isExamMode || !locked) playSound('hover');
                          }}
                          onClick={() => checkAnswer(index)}
                        >
                          <div className="option-label">{index + 1}</div>
                          <LatexText text={opt} className="flex-1" />
                        </button>
                      );
                    })}
                  </div>

                  {!isExamMode && (
                    <div className="feedback" id="feedback">
                      {feedbackHtml && (
                        <span style={{ color: feedbackHtml.color }}>
                          {feedbackHtml.text}
                        </span>
                      )}
                    </div>
                  )}
                </div>

                {/* Bottom Bar for Exam Mode */}
                {isExamMode && (
                  <ExamBottomBar
                    currentIndex={current}
                    totalCount={activeQuiz.length}
                    hasSelectedAnswer={examAnswers[current] !== undefined}
                    isBookmarked={examBookmarkedIndices.has(current)}
                    onClearOption={clearExamOption}
                    onPrevQuestion={() => goToExamQuestion(current - 1)}
                    onNextQuestion={() => goToExamQuestion(current + 1)}
                    onToggleBookmark={() => toggleExamBookmark(current)}
                    onRequestSubmit={() => setShowSubmitConfirmDialog(true)}
                  />
                )}
              </div>
            )}

            {screen === 'result' && (
              <div className="result" id="result">
                <div
                  className="result-score-ring"
                  id="scoreRing"
                  style={{
                    background: `linear-gradient(135deg, ${ringColors[0]}, ${ringColors[1]})`,
                  }}
                >
                  <div id="finalScore">{score}</div>
                  <div className="sub" id="finalFraction">
                    / {totalQuestions}
                  </div>
                </div>

                <div
                  className="grade-badge"
                  id="gradeBadge"
                  style={{ background: ringColors[0], color: 'white' }}
                >
                  {grade}
                </div>
                <p
                  id="gradeMsg"
                  style={{
                    fontSize: '15px',
                    color: 'var(--muted)',
                    margin: '4px 0 16px',
                  }}
                >
                  {gradeMsg}
                </p>

                <div className="stats-row">
                  <div className="stat-pill">
                    Accuracy<span id="statAccuracy">{accuracy}%</span>
                  </div>
                  <div className="stat-pill">
                    Correct<span id="statCorrect">{score}</span>
                  </div>
                  <div className="stat-pill">
                    Wrong<span id="statWrong">{wrongCount}</span>
                  </div>
                  <div className="stat-pill">
                    Skipped<span id="statSkipped">{skipped || timedOutCount}</span>
                  </div>
                </div>

                <div className="result-actions">
                  {wrongQuestions.length > 0 && (
                    <>
                      <button
                        type="button"
                        className="btn-action amber"
                        id="toggleReviewBtn"
                        onClick={() => setShowReview((r) => !r)}
                      >
                        {showReview ? 'छुपाएं (W)' : 'Wrong Answers (W)'}
                      </button>
                      <button
                        type="button"
                        className="btn-action rose"
                        id="retryBtn"
                        onClick={retryWrongAnswers}
                      >
                        Retry Wrong (R)
                      </button>
                    </>
                  )}
                  <button
                    type="button"
                    className="btn-action"
                    onClick={() => startQuizWithPool(quiz, numQuestionsInput)}
                  >
                    Try Again (T)
                  </button>
                  <button
                    type="button"
                    className="btn-action"
                    onClick={goHome}
                    style={{
                      background: 'linear-gradient(135deg, #475569, #334155)',
                    }}
                  >
                    Home (H)
                  </button>
                </div>

                {showReview && wrongQuestions.length > 0 && (
                  <div id="reviewAnswers">
                    <h3
                      style={{
                        fontWeight: 700,
                        marginBottom: '12px',
                        color: 'var(--text)',
                      }}
                    >
                      गलत जवाब:
                    </h3>
                    {wrongQuestions.map((q, idx) => {
                      const starred = isQuestionStarred(q);
                      const userIdx = q.userAnswerIndex;
                      return (
                        <div key={idx} className="review-item">
                          <div className="flex items-start justify-between gap-2">
                            <div className="rq flex-1">
                              Q: <LatexText text={q.q} />
                            </div>
                            <button
                              type="button"
                              onClick={() => toggleStarQuestion(q)}
                              className="px-2.5 py-1 rounded-lg border border-amber-400/60 text-xs font-bold text-amber-600 dark:text-amber-400 cursor-pointer"
                              title={starred ? 'Unstar Question' : 'Star Question'}
                            >
                              {starred ? 'Starred' : 'Star'}
                            </button>
                          </div>

                          {userIdx !== undefined && userIdx !== null ? (
                            <div className="text-sm font-semibold text-red-600 dark:text-red-400 mb-1">
                              ✖ आपका जवाब: <LatexText text={String(q.o[userIdx] ?? '')} />
                            </div>
                          ) : q._timedOut ? (
                            <div className="text-sm font-semibold text-amber-600 dark:text-amber-400 mb-1">
                              ⏰ Status: Skipped / Not Answered
                            </div>
                          ) : null}

                          <div className="ra">
                            ✔ सही जवाब: <LatexText text={String(q.o[q.a])} />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </main>
  );
}
