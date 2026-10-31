import React, { useState, useMemo, useRef } from 'react';
import {
  SavedQuiz,
  QuizQuestion,
  exportQuizzesToEditableText,
} from '../utils/latexQuizEngine';
import { LatexText } from './LatexText';

interface QuizDatabaseScreenProps {
  savedQuizzes: SavedQuiz[];
  starredQuestions: QuizQuestion[];
  selectedCategoryFilter: string;
  searchQuery: string;
  onSelectCategoryFilter: (cat: string) => void;
  onSearchQueryChange: (q: string) => void;
  onBackHome: () => void;
  onSelectSavedQuiz: (quiz: SavedQuiz, startImmediately: boolean) => void;
  onDeleteSavedQuiz: (quizId: string) => void;
  onClearAllDatabase: () => void;
  onImportText: (text: string, fallbackName?: string) => void;
  onExportOfflineHtml: (quiz: SavedQuiz) => void;
  onStartStarredQuiz: () => void;
  onRemoveStarredQuestion: (questionText: string) => void;
  onClearAllStarred: () => void;
  onShowToast: (msg: string) => void;
}

export const QuizDatabaseScreen: React.FC<QuizDatabaseScreenProps> = ({
  savedQuizzes,
  starredQuestions,
  selectedCategoryFilter,
  searchQuery,
  onSelectCategoryFilter,
  onSearchQueryChange,
  onBackHome,
  onSelectSavedQuiz,
  onDeleteSavedQuiz,
  onClearAllDatabase,
  onImportText,
  onExportOfflineHtml,
  onStartStarredQuiz,
  onRemoveStarredQuestion,
  onClearAllStarred,
  onShowToast,
}) => {
  const [quizPendingDelete, setQuizPendingDelete] = useState<SavedQuiz | null>(null);
  const [showClearAllConfirm, setShowClearAllConfirm] = useState<boolean>(false);
  const [showStarredBankModal, setShowStarredBankModal] = useState<boolean>(false);

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // Compute category counts
  const categoryCounts = useMemo(() => {
    const counts = new Map<string, number>();
    savedQuizzes.forEach((q) => {
      const cat =
        (q.category || 'General').split(/\r?\n/)[0]?.trim().slice(0, 25) ||
        'General';
      counts.set(cat, (counts.get(cat) || 0) + 1);
    });
    return Array.from(counts.entries());
  }, [savedQuizzes]);

  // Filter quizzes by category and search query
  const filteredQuizzes = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    return savedQuizzes.filter((saved) => {
      const cleanCat =
        (saved.category || 'General').split(/\r?\n/)[0]?.trim().slice(0, 25) ||
        'General';
      const matchesCategory =
        selectedCategoryFilter === 'All' ||
        cleanCat.toLowerCase() === selectedCategoryFilter.toLowerCase();
      const matchesQuery =
        !query ||
        saved.name.toLowerCase().includes(query) ||
        cleanCat.toLowerCase().includes(query);
      return matchesCategory && matchesQuery;
    });
  }, [savedQuizzes, selectedCategoryFilter, searchQuery]);

  // Group filtered quizzes by category
  const groupedByCategory = useMemo(() => {
    const groups = new Map<string, SavedQuiz[]>();
    filteredQuizzes.forEach((saved) => {
      const cleanCat =
        (saved.category || 'General').split(/\r?\n/)[0]?.trim().slice(0, 25) ||
        'General';
      const list = groups.get(cleanCat) || [];
      list.push(saved);
      groups.set(cleanCat, list);
    });
    return Array.from(groups.entries());
  }, [filteredQuizzes]);

  // Helper to download a .txt file
  const downloadTxtFile = (filename: string, content: string) => {
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  // Export all quizzes (and starred bank) as editable .txt
  const handleExportAllTxt = () => {
    if (savedQuizzes.length === 0 && starredQuestions.length === 0) return;
    const content = exportQuizzesToEditableText(savedQuizzes, starredQuestions);
    downloadTxtFile('raman_quizzes.txt', content);
    onShowToast('✅ Exported raman_quizzes.txt');
  };

  // Export a single quiz as editable .txt
  const handleExportSingleTxt = (quiz: SavedQuiz) => {
    const content = exportQuizzesToEditableText([quiz], []);
    const safeName =
      quiz.name.replace(/[^a-zA-Z0-9_\-\u0900-\u097F ]/g, '').trim().replace(/\s+/g, '_') ||
      'raman_quiz';
    downloadTxtFile(`${safeName}.txt`, content);
    onShowToast(`✅ Exported ${safeName}.txt`);
  };

  // Share all quizzes (or filtered category) via Web Share API / Clipboard / Download
  const handleShareAll = async () => {
    if (savedQuizzes.length === 0 && starredQuestions.length === 0) return;
    const listToExport =
      selectedCategoryFilter !== 'All'
        ? savedQuizzes.filter(
            (q) =>
              (q.category || 'General').toLowerCase() ===
              selectedCategoryFilter.toLowerCase()
          )
        : savedQuizzes;
    const starredToExport =
      selectedCategoryFilter !== 'All' ? [] : starredQuestions;

    const textContent = exportQuizzesToEditableText(listToExport, starredToExport);
    const fileName =
      selectedCategoryFilter !== 'All'
        ? `raman_quiz_${selectedCategoryFilter.toLowerCase()}.txt`
        : 'raman_all_quizzes.txt';

    if (navigator.share) {
      try {
        const file = new File([textContent], fileName, { type: 'text/plain' });
        if (navigator.canShare && navigator.canShare({ files: [file] })) {
          await navigator.share({
            title: fileName,
            text: 'Raman Quiz Database Export',
            files: [file],
          });
          return;
        }
        await navigator.share({
          title: fileName,
          text: textContent,
        });
        return;
      } catch {
        // Fallback below
      }
    }

    try {
      await navigator.clipboard.writeText(textContent);
      onShowToast('📋 All quizzes copied to clipboard & downloaded!');
    } catch {
      // Ignore clipboard failure
    }
    downloadTxtFile(fileName, textContent);
  };

  // Handle file selection for Import
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      const content = String(event.target?.result || '');
      const fallbackName = file.name.replace(/\.[^/.]+$/, '') || 'Imported Quiz';
      onImportText(content, fallbackName);
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  const formatTimestamp = (ts: number) => {
    try {
      return new Date(ts).toLocaleString('en-IN', {
        day: '2-digit',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit',
        hour12: true,
      });
    } catch {
      return '';
    }
  };

  return (
    <div
      id="quizDatabasePage"
      className="min-h-screen w-full p-4 sm:p-6 flex flex-col items-center"
    >
      {/* Hidden File Input for 1-Click Import */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".txt,.json,text/plain,application/json"
        onChange={handleFileChange}
        className="hidden"
      />

      {/* Starred Questions Bank Modal */}
      {showStarredBankModal && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Starred Questions Bank"
          className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
        >
          <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-xl w-full max-h-[85vh] p-5 shadow-2xl border border-slate-200 dark:border-slate-700 flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-10 h-10 rounded-full bg-amber-500/20 flex items-center justify-center text-amber-500 text-xl">
                  ⭐
                </div>
                <div>
                  <h2 className="text-base font-extrabold text-slate-900 dark:text-white">
                    Starred Questions Bank
                  </h2>
                  <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                    {starredQuestions.length} Important Questions
                  </p>
                </div>
              </div>

              {starredQuestions.length > 0 && (
                <button
                  type="button"
                  onClick={onClearAllStarred}
                  title="Clear All Starred"
                  className="p-2 rounded-full text-red-600 hover:bg-red-50 dark:hover:bg-red-950/40 cursor-pointer"
                >
                  🗑️
                </button>
              )}
            </div>

            {starredQuestions.length === 0 ? (
              <div className="py-10 text-center text-sm text-slate-500 dark:text-slate-400">
                No starred questions yet. Tap the ⭐ Star icon during any quiz or on the Result page to save important questions here.
              </div>
            ) : (
              <>
                <div className="flex-1 overflow-y-auto flex flex-col gap-2.5 pr-1">
                  {starredQuestions.map((q, idx) => (
                    <div
                      key={idx}
                      className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/70 border border-amber-400/50 flex flex-col gap-1.5"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="text-sm font-bold text-slate-900 dark:text-slate-100 flex-1">
                          <span className="mr-1">Q{idx + 1}.</span>
                          <LatexText text={q.q} />
                        </div>
                        <button
                          type="button"
                          onClick={() => onRemoveStarredQuestion(q.q)}
                          className="text-red-600 hover:text-red-700 text-sm px-1.5 cursor-pointer"
                          title="Remove Starred Question"
                        >
                          🗑
                        </button>
                      </div>
                      <div className="text-sm font-bold text-green-600 dark:text-green-400">
                        ✔ Ans: <LatexText text={String(q.o[q.a] ?? '')} />
                      </div>
                    </div>
                  ))}
                </div>

                <div className="flex gap-2 pt-1">
                  <button
                    type="button"
                    onClick={() => {
                      setShowStarredBankModal(false);
                      onStartStarredQuiz();
                    }}
                    className="flex-1 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm cursor-pointer"
                  >
                    ▶ Start Quiz
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      const content = exportQuizzesToEditableText(
                        [],
                        starredQuestions
                      );
                      downloadTxtFile('starred_questions_bank.txt', content);
                      onShowToast('✅ Exported starred_questions_bank.txt');
                    }}
                    className="flex-1 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm cursor-pointer"
                  >
                    📤 Export .txt
                  </button>
                </div>
              </>
            )}

            <button
              type="button"
              onClick={() => setShowStarredBankModal(false)}
              className="w-full py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-sm cursor-pointer"
            >
              Close
            </button>
          </div>
        </div>
      )}

      {/* Single Quiz Delete Confirmation Modal */}
      {quizPendingDelete && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Delete Quiz Confirmation"
          className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
        >
          <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-sm w-full p-5 shadow-2xl border border-slate-200 dark:border-slate-700 flex flex-col items-center gap-3 text-center">
            <div className="w-12 h-12 rounded-full bg-red-600/15 flex items-center justify-center text-red-600 text-2xl">
              🗑
            </div>
            <h3 className="text-lg font-extrabold text-slate-900 dark:text-white">
              Delete Quiz?
            </h3>
            <p className="text-sm text-slate-600 dark:text-slate-300">
              Are you sure you want to delete &ldquo;{quizPendingDelete.name}&rdquo; (
              {quizPendingDelete.questionCount} questions)?
            </p>
            <div className="flex w-full gap-2.5 mt-1">
              <button
                type="button"
                onClick={() => setQuizPendingDelete(null)}
                className="flex-1 py-2.5 rounded-xl bg-green-600 hover:bg-green-700 text-white font-bold text-sm cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onDeleteSavedQuiz(quizPendingDelete.id);
                  setQuizPendingDelete(null);
                }}
                className="flex-1 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-sm cursor-pointer"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Clear All Quiz Data Confirmation Modal */}
      {showClearAllConfirm && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Clear All Quiz Data Confirmation"
          className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
        >
          <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-sm w-full p-5 shadow-2xl border border-slate-200 dark:border-slate-700 flex flex-col items-center gap-3 text-center">
            <div className="w-12 h-12 rounded-full bg-red-600/15 flex items-center justify-center text-red-600 text-2xl">
              ⚠️
            </div>
            <h3 className="text-lg font-extrabold text-slate-900 dark:text-white">
              Clear All Quiz Data?
            </h3>
            <p className="text-sm text-slate-600 dark:text-slate-300">
              Are you sure you want to delete all {savedQuizzes.length} saved quizzes
              and {starredQuestions.length} starred questions from Quiz Database? This
              cannot be undone.
            </p>
            <div className="flex w-full gap-2.5 mt-1">
              <button
                type="button"
                onClick={() => setShowClearAllConfirm(false)}
                className="flex-1 py-2.5 rounded-xl bg-green-600 hover:bg-green-700 text-white font-bold text-sm cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onClearAllDatabase();
                  setShowClearAllConfirm(false);
                }}
                className="flex-1 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-sm cursor-pointer"
              >
                Delete All
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="w-full max-w-3xl flex flex-col gap-3.5">
        {/* Top Header Card */}
        <div className="bg-white dark:bg-slate-800 rounded-2xl p-4 shadow-md border border-slate-200/80 dark:border-slate-700 flex flex-col gap-3">
          {/* Top Row: Back Button, Title & Clear All Icon */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={onBackHome}
                aria-label="Back to Home"
                className="w-10 h-10 rounded-full bg-blue-600 hover:bg-blue-700 text-white font-bold text-lg flex items-center justify-center shadow cursor-pointer transition"
              >
                ←
              </button>
              <div>
                <h1 className="text-xl font-extrabold text-slate-900 dark:text-white leading-tight">
                  Quiz Database
                </h1>
                <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                  {savedQuizzes.length} Quizzes • {categoryCounts.length} Categories
                </p>
              </div>
            </div>

            {(savedQuizzes.length > 0 || starredQuestions.length > 0) && (
              <button
                type="button"
                onClick={() => setShowClearAllConfirm(true)}
                title="Clear All Quiz Data"
                aria-label="Clear All Quiz Data"
                className="w-10 h-10 rounded-full bg-red-600/12 hover:bg-red-600/20 text-red-600 flex items-center justify-center text-lg cursor-pointer transition"
              >
                🗑️
              </button>
            )}
          </div>

          {/* Import / Export / Share Action Buttons Row */}
          <div className="grid grid-cols-3 gap-2">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="h-10.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm flex items-center justify-center gap-1.5 shadow-sm cursor-pointer transition"
            >
              <span>📥</span>
              <span>Import</span>
            </button>

            <button
              type="button"
              disabled={savedQuizzes.length === 0 && starredQuestions.length === 0}
              onClick={handleExportAllTxt}
              className="h-10.5 rounded-xl bg-blue-600 hover:bg-blue-700 disabled:opacity-45 text-white font-bold text-sm flex items-center justify-center gap-1.5 shadow-sm cursor-pointer disabled:cursor-not-allowed transition"
            >
              <span>📤</span>
              <span>Export</span>
            </button>

            <button
              type="button"
              disabled={savedQuizzes.length === 0 && starredQuestions.length === 0}
              onClick={handleShareAll}
              className="h-10.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-45 text-white font-bold text-sm flex items-center justify-center gap-1.5 shadow-sm cursor-pointer disabled:cursor-not-allowed transition"
            >
              <span>🔗</span>
              <span>Share</span>
            </button>
          </div>

          {/* Search Bar */}
          <div className="relative flex items-center">
            <span className="absolute left-3.5 text-slate-400 text-sm pointer-events-none">
              🔍
            </span>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => onSearchQueryChange(e.target.value)}
              placeholder="Search quiz list and cat."
              aria-label="Search quiz list and category"
              className="w-full pl-9 pr-9 py-2.5 rounded-xl border border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-900/60 text-sm text-slate-900 dark:text-slate-100 outline-none focus:border-blue-600"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => onSearchQueryChange('')}
                aria-label="Clear search"
                className="absolute right-3 text-slate-400 hover:text-slate-600 text-sm cursor-pointer"
              >
                ✖
              </button>
            )}
          </div>

          {/* Category Filter Chips Row */}
          {savedQuizzes.length > 0 && (
            <div className="flex items-center gap-2 overflow-x-auto pb-1">
              <button
                type="button"
                onClick={() => onSelectCategoryFilter('All')}
                className={`px-3 py-1.5 rounded-full text-xs font-bold whitespace-nowrap shrink-0 cursor-pointer transition ${
                  selectedCategoryFilter === 'All'
                    ? 'bg-blue-600 text-white'
                    : 'bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-200'
                }`}
              >
                All ({savedQuizzes.length})
              </button>

              {categoryCounts.map(([category, count]) => {
                const isSelected =
                  selectedCategoryFilter.toLowerCase() === category.toLowerCase();
                return (
                  <button
                    key={category}
                    type="button"
                    onClick={() => onSelectCategoryFilter(category)}
                    className={`px-3 py-1.5 rounded-full text-xs font-bold whitespace-nowrap shrink-0 cursor-pointer transition ${
                      isSelected
                        ? 'bg-blue-600 text-white'
                        : 'bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-200'
                    }`}
                  >
                    {category} ({count})
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Dedicated Starred / Bookmarked Questions Bank Card */}
        <div
          onClick={() => setShowStarredBankModal(true)}
          className="bg-amber-50/90 dark:bg-slate-800 rounded-2xl px-4 py-3 border-2 border-amber-400/70 shadow-sm flex items-center justify-between gap-3 cursor-pointer hover:border-amber-500 transition"
        >
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-10 h-10 rounded-full bg-amber-500/20 flex items-center justify-center text-amber-500 text-lg shrink-0">
              ⭐
            </div>
            <div className="min-w-0">
              <div className="text-sm font-extrabold text-slate-900 dark:text-white">
                Starred Questions Bank
              </div>
              <div className="text-xs text-slate-600 dark:text-slate-300 truncate">
                {starredQuestions.length > 0
                  ? `${starredQuestions.length} प्रश्न सेव हैं • Tap to View, Play or Export`
                  : 'क्विज़ के दौरान ⭐ दबाकर महत्वपूर्ण प्रश्न यहाँ सेव करें'}
              </div>
            </div>
          </div>

          {starredQuestions.length > 0 && (
            <div
              className="flex items-center gap-1 shrink-0"
              onClick={(e) => e.stopPropagation()}
            >
              <button
                type="button"
                onClick={onStartStarredQuiz}
                title="Start Starred Quiz"
                className="w-9 h-9 rounded-full hover:bg-blue-100 dark:hover:bg-slate-700 text-blue-600 flex items-center justify-center text-base cursor-pointer"
              >
                ▶
              </button>
              <button
                type="button"
                onClick={() => setShowStarredBankModal(true)}
                title="View Starred Questions"
                className="w-9 h-9 rounded-full hover:bg-amber-100 dark:hover:bg-slate-700 text-amber-600 flex items-center justify-center text-base cursor-pointer"
              >
                👁
              </button>
              <button
                type="button"
                onClick={() => {
                  const content = exportQuizzesToEditableText([], starredQuestions);
                  downloadTxtFile('starred_questions_bank.txt', content);
                  onShowToast('✅ Exported starred_questions_bank.txt');
                }}
                title="Export Starred Questions (.txt)"
                className="w-9 h-9 rounded-full hover:bg-indigo-100 dark:hover:bg-slate-700 text-indigo-600 flex items-center justify-center text-base cursor-pointer"
              >
                📤
              </button>
            </div>
          )}
        </div>

        {/* Category-Wise Quiz List Content */}
        {savedQuizzes.length === 0 ? (
          <div className="bg-white dark:bg-slate-800 rounded-2xl p-8 text-center shadow-sm border border-slate-200/80 dark:border-slate-700 flex flex-col items-center gap-2">
            <div className="text-4xl">📁</div>
            <div className="text-base font-bold text-slate-900 dark:text-white">
              No Saved Quizzes Yet
            </div>
            <p className="text-sm text-slate-500 dark:text-slate-400 max-w-md">
              Save a quiz from the Home page or tap Import above to load a .txt quiz file.
            </p>
          </div>
        ) : filteredQuizzes.length === 0 ? (
          <div className="bg-white dark:bg-slate-800 rounded-2xl p-6 text-center shadow-sm border border-slate-200/80 dark:border-slate-700 text-sm text-slate-500 dark:text-slate-400">
            No quizzes match your search or category filter.
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {groupedByCategory.map(([categoryName, quizzesInCat]) => (
              <div key={categoryName} className="flex flex-col gap-2">
                {/* Category Header Bar */}
                <div className="bg-indigo-100/90 dark:bg-slate-800 rounded-xl px-3.5 py-2 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-base">📁</span>
                    <span className="text-sm font-extrabold text-indigo-950 dark:text-white">
                      {categoryName}
                    </span>
                  </div>
                  <span className="px-2.5 py-0.5 rounded-full bg-indigo-600 text-white text-xs font-bold">
                    {quizzesInCat.length} Quiz{quizzesInCat.length > 1 ? 'zes' : ''}
                  </span>
                </div>

                {/* Quizzes in this Category */}
                {quizzesInCat.map((saved) => {
                  const categoryLabel =
                    (saved.category || 'General')
                      .split(/\r?\n/)[0]
                      ?.trim()
                      .slice(0, 25) || 'General';
                  return (
                    <div
                      key={saved.id}
                      onClick={() => onSelectSavedQuiz(saved, false)}
                      className="bg-white dark:bg-slate-800 rounded-xl px-3.5 py-2.5 shadow-sm border border-slate-200/80 dark:border-slate-700 hover:border-blue-400 flex items-center justify-between gap-3 cursor-pointer transition"
                    >
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-sm font-bold text-slate-900 dark:text-white truncate">
                            {saved.name}
                          </span>
                          <span className="px-2 py-0.5 rounded-md bg-indigo-600/12 text-indigo-600 dark:text-indigo-300 text-xs font-bold">
                            {categoryLabel}
                          </span>
                        </div>
                        <div className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                          {formatTimestamp(saved.timestamp)} • {saved.questionCount} प्रश्न
                        </div>
                      </div>

                      <div
                        className="flex items-center gap-1 shrink-0"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <button
                          type="button"
                          onClick={() => onSelectSavedQuiz(saved, true)}
                          title="Start Quiz"
                          aria-label={`Start ${saved.name}`}
                          className="w-9 h-9 rounded-full hover:bg-blue-50 dark:hover:bg-slate-700 text-blue-600 flex items-center justify-center text-base cursor-pointer"
                        >
                          ▶
                        </button>
                        <button
                          type="button"
                          onClick={() => handleExportSingleTxt(saved)}
                          title="Export Quiz (.txt)"
                          aria-label={`Export ${saved.name} as txt`}
                          className="w-9 h-9 rounded-full hover:bg-indigo-50 dark:hover:bg-slate-700 text-indigo-600 flex items-center justify-center text-base cursor-pointer"
                        >
                          📤
                        </button>
                        <button
                          type="button"
                          onClick={() => onExportOfflineHtml(saved)}
                          title="Download Offline HTML"
                          aria-label={`Download ${saved.name} offline HTML`}
                          className="w-9 h-9 rounded-full hover:bg-sky-50 dark:hover:bg-slate-700 text-sky-600 flex items-center justify-center text-base cursor-pointer"
                        >
                          📥
                        </button>
                        <button
                          type="button"
                          onClick={() => setQuizPendingDelete(saved)}
                          title="Delete Saved Quiz"
                          aria-label={`Delete ${saved.name}`}
                          className="w-9 h-9 rounded-full hover:bg-red-50 dark:hover:bg-slate-700 text-red-600 flex items-center justify-center text-base cursor-pointer"
                        >
                          🗑
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
