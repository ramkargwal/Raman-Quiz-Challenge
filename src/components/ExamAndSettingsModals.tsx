import React, { useState } from 'react';

/* ═══════════════════════════════════════════════
   1. EXAM BOTTOM BAR
═══════════════════════════════════════════════ */
interface ExamBottomBarProps {
  currentIndex: number;
  totalCount: number;
  hasSelectedAnswer: boolean;
  isBookmarked: boolean;
  onClearOption: () => void;
  onPrevQuestion: () => void;
  onNextQuestion: () => void;
  onToggleBookmark: () => void;
  onRequestSubmit: () => void;
}

export const ExamBottomBar: React.FC<ExamBottomBarProps> = ({
  currentIndex,
  totalCount,
  hasSelectedAnswer,
  isBookmarked,
  onClearOption,
  onPrevQuestion,
  onNextQuestion,
  onToggleBookmark,
  onRequestSubmit,
}) => {
  const canGoPrev = currentIndex > 0;
  const canGoNext = currentIndex < totalCount - 1;

  return (
    <div className="w-full border-t border-slate-200 dark:border-slate-700 bg-white/95 dark:bg-slate-800/95 px-6 py-3.5 flex flex-col gap-2.5 rounded-b-3xl">
      {/* Top Utility Row: Clear Response + Submit Exam */}
      <div className="flex items-center justify-between">
        <div>
          {hasSelectedAnswer && (
            <button
              type="button"
              onClick={onClearOption}
              data-testid="exam-clear-option-button"
              className="px-3.5 py-1.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold shadow-sm cursor-pointer transition"
            >
              Clear Response
            </button>
          )}
        </div>

        <button
          type="button"
          onClick={onRequestSubmit}
          data-testid="exam-submit-button"
          className="px-4 py-1.5 rounded-xl bg-green-600 hover:bg-green-700 text-white text-xs font-extrabold flex items-center gap-1.5 shadow-sm cursor-pointer transition"
        >
          <span>Submit Exam</span>
        </button>
      </div>

      {/* Bottom Navigation Row: Previous, Bookmark for Attempt Later, & Next Buttons */}
      <div className="flex items-center gap-2.5">
        <button
          type="button"
          onClick={onPrevQuestion}
          disabled={!canGoPrev}
          data-testid="exam-prev-button"
          className="flex-1 h-11.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-600/40 text-white font-bold text-sm flex items-center justify-center gap-1.5 cursor-pointer disabled:cursor-not-allowed transition"
        >
          <span>Previous</span>
        </button>

        <button
          type="button"
          onClick={onToggleBookmark}
          data-testid="exam-bookmark-question-button"
          title={
            isBookmarked
              ? 'Remove Bookmark (Attempt Later)'
              : 'Bookmark Question for Attempt Later'
          }
          aria-label={
            isBookmarked
              ? 'Remove Bookmark (Attempt Later)'
              : 'Bookmark Question for Attempt Later'
          }
          className={`px-3.5 h-11.5 rounded-xl border-2 flex items-center justify-center gap-1.5 text-xs sm:text-sm font-extrabold cursor-pointer transition shrink-0 ${
            isBookmarked
              ? 'bg-amber-500 border-amber-600 text-white shadow-sm'
              : 'bg-slate-100 dark:bg-slate-800 border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-200 hover:border-amber-500 hover:text-amber-600'
          }`}
        >
          <span>{isBookmarked ? 'Bookmarked' : 'Bookmark'}</span>
        </button>

        <button
          type="button"
          onClick={() => {
            if (canGoNext) {
              onNextQuestion();
            } else {
              onRequestSubmit();
            }
          }}
          data-testid="exam-next-button"
          className={`flex-1 h-11.5 rounded-xl text-white font-bold text-sm flex items-center justify-center gap-1.5 cursor-pointer transition ${
            canGoNext
              ? 'bg-blue-600 hover:bg-blue-700'
              : 'bg-indigo-600 hover:bg-indigo-700'
          }`}
        >
          <span>{canGoNext ? 'Next' : 'Finish & Submit'}</span>
        </button>
      </div>
    </div>
  );
};

/* ═══════════════════════════════════════════════
   2. EXAM PALETTE DIALOG
═══════════════════════════════════════════════ */
interface ExamPaletteDialogProps {
  totalCount: number;
  currentIndex: number;
  examAnswers: Record<number, number>;
  bookmarkedIndices: Set<number>;
  onGoToQuestion: (index: number) => void;
  onDismiss: () => void;
  onRequestSubmit: () => void;
}

export const ExamPaletteDialog: React.FC<ExamPaletteDialogProps> = ({
  totalCount,
  currentIndex,
  examAnswers,
  bookmarkedIndices,
  onGoToQuestion,
  onDismiss,
  onRequestSubmit,
}) => {
  const answeredCount = Object.keys(examAnswers).length;
  const bookmarkedCount = bookmarkedIndices.size;
  const unansweredCount = Math.max(0, totalCount - answeredCount);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Question Palette"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4"
    >
      <div className="bg-white dark:bg-slate-900 rounded-2xl max-w-4xl w-full p-5 shadow-2xl border border-slate-200 dark:border-slate-700 flex flex-col items-center">
        {/* Header */}
        <div className="flex items-center justify-between w-full mb-3.5 pb-2.5 border-b border-slate-200 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <span className="w-8 h-8 rounded-xl bg-blue-600/10 dark:bg-blue-500/20 text-blue-600 dark:text-blue-400 text-sm flex items-center justify-center font-bold">
              ▦
            </span>
            <div>
              <h2 className="text-base font-extrabold text-slate-900 dark:text-white leading-tight">
                Question Palette
              </h2>
              <p className="text-[11px] font-medium text-slate-500 dark:text-slate-400">
                Click any question number to jump directly to it
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onDismiss}
            aria-label="Close Question Palette"
            className="px-2.5 h-8 rounded-lg bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300 font-bold text-xs flex items-center justify-center cursor-pointer transition"
          >
            Close
          </button>
        </div>

        {/* Status Legend Cards */}
        <div className="grid grid-cols-3 gap-2.5 w-full mb-3.5">
          <div className="flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl bg-green-50 dark:bg-green-950/40 border border-green-200 dark:border-green-800/60">
            <span className="w-2.5 h-2.5 rounded-full bg-green-600 inline-block shrink-0" />
            <span className="text-xs font-bold text-green-800 dark:text-green-300">
              Answered ({answeredCount})
            </span>
          </div>

          <div className="flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60">
            <span className="w-2.5 h-2.5 rounded-full bg-amber-500 inline-block shrink-0" />
            <span className="text-xs font-bold text-amber-800 dark:text-amber-300">
              Bookmarked ({bookmarkedCount})
            </span>
          </div>

          <div className="flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-100 dark:bg-slate-800 border border-slate-300 dark:border-slate-700">
            <span className="w-2.5 h-2.5 rounded-full bg-slate-500 inline-block shrink-0" />
            <span className="text-xs font-bold text-slate-700 dark:text-slate-300">
              Left ({unansweredCount})
            </span>
          </div>
        </div>

        {/* Grid of Question Numbers (15 boxes per single line on desktop) */}
        <div
          className={`w-full max-h-[48vh] min-h-[120px] overflow-y-auto p-3 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-200 dark:border-slate-700/80 mb-4 ${
            totalCount <= 15
              ? 'flex flex-wrap items-center justify-center gap-2'
              : 'grid grid-cols-6 sm:grid-cols-10 md:grid-cols-15 gap-2 justify-items-center'
          }`}
        >
          {Array.from({ length: totalCount }).map((_, idx) => {
            const isCurrent = idx === currentIndex;
            const isAnswered = examAnswers[idx] !== undefined;
            const isBookmarked = bookmarkedIndices.has(idx);

            let bgClass =
              'bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-200 hover:bg-blue-50 dark:hover:bg-slate-700';
            if (isBookmarked) {
              bgClass = 'bg-amber-500 hover:bg-amber-600 text-white';
            } else if (isAnswered) {
              bgClass = 'bg-green-600 hover:bg-green-700 text-white';
            }

            return (
              <button
                key={idx}
                type="button"
                onClick={() => {
                  onGoToQuestion(idx);
                  onDismiss();
                }}
                className={`w-9 h-9 rounded-lg font-bold text-xs flex items-center justify-center cursor-pointer transition ${bgClass} ${
                  isCurrent
                    ? 'ring-2 ring-blue-600 border-2 border-white dark:border-slate-900'
                    : 'border border-slate-300 dark:border-slate-600'
                }`}
              >
                {idx + 1}
              </button>
            );
          })}
        </div>

        {/* Actions */}
        <div className="flex w-full gap-2.5">
          <button
            type="button"
            onClick={onDismiss}
            className="flex-1 h-10 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs sm:text-sm cursor-pointer transition shadow-xs"
          >
            Close
          </button>
          <button
            type="button"
            onClick={() => {
              onDismiss();
              onRequestSubmit();
            }}
            className="flex-[1.3] h-10 rounded-xl bg-green-600 hover:bg-green-700 text-white font-bold text-xs sm:text-sm cursor-pointer transition shadow-xs"
          >
            Submit Exam
          </button>
        </div>
      </div>
    </div>
  );
};

/* ═══════════════════════════════════════════════
   3. EXAM SUBMIT CONFIRM DIALOG
═══════════════════════════════════════════════ */
interface ExamSubmitConfirmDialogProps {
  totalCount: number;
  answeredCount: number;
  bookmarkedCount: number;
  onDismiss: () => void;
  onConfirmSubmit: () => void;
}

export const ExamSubmitConfirmDialog: React.FC<ExamSubmitConfirmDialogProps> = ({
  totalCount,
  answeredCount,
  bookmarkedCount,
  onDismiss,
  onConfirmSubmit,
}) => {
  const unansweredCount = Math.max(0, totalCount - answeredCount);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Submit Exam Confirmation"
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
    >
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-sm w-full p-5 shadow-2xl border border-slate-200 dark:border-slate-700 flex flex-col items-center text-center">
        <h2 className="text-xl font-extrabold text-slate-900 dark:text-white">
          Submit Exam?
        </h2>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1.5 mb-4">
          Check your exam summary before final submission:
        </p>

        <div className="grid grid-cols-4 gap-2 w-full mb-5">
          <div className="p-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
            <div className="text-[11px] font-semibold text-slate-500 dark:text-slate-400">
              Total
            </div>
            <div className="text-lg font-extrabold text-blue-600">
              {totalCount}
            </div>
          </div>
          <div className="p-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
            <div className="text-[11px] font-semibold text-slate-500 dark:text-slate-400">
              Done
            </div>
            <div className="text-lg font-extrabold text-green-600">
              {answeredCount}
            </div>
          </div>
          <div className="p-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
            <div className="text-[11px] font-semibold text-slate-500 dark:text-slate-400">
              Skipped
            </div>
            <div className="text-lg font-extrabold text-amber-500">
              {unansweredCount}
            </div>
          </div>
          <div className="p-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
            <div className="text-[11px] font-semibold text-slate-500 dark:text-slate-400">
              Bookmarked
            </div>
            <div className="text-lg font-extrabold text-amber-500">
              {bookmarkedCount}
            </div>
          </div>
        </div>

        <div className="flex w-full gap-2.5">
          <button
            type="button"
            onClick={onDismiss}
            className="flex-1 h-12 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-sm cursor-pointer transition"
          >
            Back
          </button>
          <button
            type="button"
            onClick={onConfirmSubmit}
            data-testid="confirm-submit-exam-button"
            className="flex-[1.3] h-12 rounded-xl bg-green-600 hover:bg-green-700 text-white font-extrabold text-sm cursor-pointer transition"
          >
            Submit Now
          </button>
        </div>
      </div>
    </div>
  );
};

/* ═══════════════════════════════════════════════
   3B. EXIT QUIZ / EXAM CONFIRMATION DIALOG
═══════════════════════════════════════════════ */
interface ExitQuizConfirmDialogProps {
  isExamMode: boolean;
  currentIndex: number;
  totalCount: number;
  onDismiss: () => void;
  onConfirmExit: () => void;
}

export const ExitQuizConfirmDialog: React.FC<ExitQuizConfirmDialogProps> = ({
  isExamMode,
  currentIndex,
  totalCount,
  onDismiss,
  onConfirmExit,
}) => {
  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={
        isExamMode ? 'Exit Exam Confirmation' : 'Exit Quiz Confirmation'
      }
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4"
    >
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-sm w-full p-5.5 shadow-2xl border border-slate-200 dark:border-slate-700 flex flex-col items-center text-center">
        <div className="w-12 h-12 rounded-full bg-red-100 dark:bg-red-950/60 text-red-600 dark:text-red-400 flex items-center justify-center text-2xl mb-3 shadow-xs">
          🏠
        </div>

        <h2 className="text-xl font-extrabold text-slate-900 dark:text-white">
          {isExamMode ? 'Exit Exam?' : 'Exit Quiz?'}
        </h2>
        <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1.5 mb-4">
          {isExamMode
            ? `Are you sure you want to exit the Exam (Question ${
                currentIndex + 1
              } of ${totalCount}) and return to Home? Your current exam progress will be lost.`
            : `Are you sure you want to exit the Quiz (Question ${
                currentIndex + 1
              } of ${totalCount}) and return to Home? Your current progress will be lost.`}
        </p>

        <div className="flex w-full gap-2.5">
          <button
            type="button"
            onClick={onDismiss}
            data-testid="cancel-exit-quiz-button"
            className="flex-1 h-11 rounded-xl bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-bold text-sm cursor-pointer transition"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={() => {
              onDismiss();
              onConfirmExit();
            }}
            data-testid="confirm-exit-quiz-button"
            className="flex-[1.3] h-11 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-sm flex items-center justify-center gap-1.5 cursor-pointer transition shadow-sm"
          >
            <span>Exit to Home</span>
          </button>
        </div>
      </div>
    </div>
  );
};

/* ═══════════════════════════════════════════════
   4. QUESTION TIMING DIALOG
═══════════════════════════════════════════════ */
interface QuestionTimingDialogProps {
  initialSeconds: number;
  onDismiss: () => void;
  onApply: (seconds: number) => void;
}

export const QuestionTimingDialog: React.FC<QuestionTimingDialogProps> = ({
  initialSeconds,
  onDismiss,
  onApply,
}) => {
  const [tempTimeSeconds, setTempTimeSeconds] = useState<number>(initialSeconds);
  const [customTimeInput, setCustomTimeInput] = useState<string>(
    String(initialSeconds)
  );
  const timePresets = [15, 30, 45, 60, 90, 120];
  const sliderMax = 180;

  const canDecrease = tempTimeSeconds > 5;
  const canIncrease = tempTimeSeconds < 600;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Question Timing"
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
    >
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-5.5 shadow-2xl border border-slate-200 dark:border-slate-700 flex flex-col items-center">
        <div className="w-12 h-12 rounded-full bg-gradient-to-br from-blue-600 to-indigo-600 text-white flex items-center justify-center text-2xl shadow mb-2.5">
          ⏱️
        </div>
        <h2 className="text-xl font-extrabold text-slate-900 dark:text-white">
          Question Timing
        </h2>
        <p className="text-xs font-medium text-slate-500 dark:text-slate-400 mb-4">
          Set time per question (Default: 30s)
        </p>

        {/* Hero Counter Card with - / + Stepper */}
        <div className="w-full rounded-2xl bg-blue-50/90 dark:bg-slate-800 border border-blue-200 dark:border-slate-700 px-4 py-3 flex items-center justify-between mb-3.5">
          <button
            type="button"
            disabled={!canDecrease}
            onClick={() => {
              const next = Math.max(5, tempTimeSeconds - 5);
              setTempTimeSeconds(next);
              setCustomTimeInput(String(next));
            }}
            aria-label="Decrease Time"
            className="w-10.5 h-10.5 rounded-full bg-white dark:bg-slate-700 text-blue-600 dark:text-blue-400 font-extrabold text-xl shadow-sm disabled:opacity-40 cursor-pointer disabled:cursor-not-allowed flex items-center justify-center"
          >
            −
          </button>

          <div className="text-center">
            <div className="text-3xl font-extrabold text-blue-600 dark:text-blue-400">
              {tempTimeSeconds}s
            </div>
            <div className="text-xs font-semibold text-slate-500 dark:text-slate-400">
              Seconds per Question
            </div>
          </div>

          <button
            type="button"
            disabled={!canIncrease}
            onClick={() => {
              const next = Math.min(600, tempTimeSeconds + 5);
              setTempTimeSeconds(next);
              setCustomTimeInput(String(next));
            }}
            aria-label="Increase Time"
            className="w-10.5 h-10.5 rounded-full bg-white dark:bg-slate-700 text-blue-600 dark:text-blue-400 font-extrabold text-xl shadow-sm disabled:opacity-40 cursor-pointer disabled:cursor-not-allowed flex items-center justify-center"
          >
            +
          </button>
        </div>

        {/* Range Slider */}
        <div className="w-full mb-3.5">
          <input
            type="range"
            min={5}
            max={sliderMax}
            value={Math.min(sliderMax, Math.max(5, tempTimeSeconds))}
            onChange={(e) => {
              const v = Number.parseInt(e.target.value, 10) || 30;
              setTempTimeSeconds(v);
              setCustomTimeInput(String(v));
            }}
            aria-label="Question Time Slider"
            className="w-full accent-blue-600 cursor-pointer h-2.5 rounded-lg"
          />
          <div className="flex justify-between text-[11px] font-semibold text-slate-500 dark:text-slate-400 px-1 mt-1">
            <span>5s</span>
            <span>180s</span>
          </div>
        </div>

        {/* Quick Select Preset Chips */}
        <div className="w-full mb-3.5">
          <div className="text-xs font-bold text-slate-600 dark:text-slate-300 mb-2">
            Quick Select
          </div>
          <div className="flex flex-wrap gap-2">
            {timePresets.map((preset) => {
              const isSelected = tempTimeSeconds === preset;
              return (
                <button
                  key={preset}
                  type="button"
                  onClick={() => {
                    setTempTimeSeconds(preset);
                    setCustomTimeInput(String(preset));
                  }}
                  className={`px-3.5 py-2 rounded-xl text-xs font-bold border cursor-pointer transition ${
                    isSelected
                      ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 border-slate-300 dark:border-slate-700 hover:bg-slate-200'
                  }`}
                >
                  {preset === 30 ? '30s (Default)' : `${preset}s`}
                </button>
              );
            })}
          </div>
        </div>

        {/* Custom Timing Input Box */}
        <div className="w-full mb-4">
          <label className="block text-xs font-bold text-slate-600 dark:text-slate-300 mb-1">
            Custom Timing (Seconds)
          </label>
          <input
            type="text"
            inputMode="numeric"
            value={customTimeInput}
            onChange={(e) => {
              const digitsOnly = e.target.value.replace(/\D/g, '').slice(0, 3);
              setCustomTimeInput(digitsOnly);
              const parsed = Number.parseInt(digitsOnly, 10);
              if (!isNaN(parsed) && parsed > 0) {
                setTempTimeSeconds(Math.min(600, Math.max(5, parsed)));
              }
            }}
            placeholder="e.g. 45, 75, 150"
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm font-semibold text-slate-900 dark:text-white outline-none focus:border-blue-600"
          />
        </div>

        {/* Action Buttons */}
        <div className="flex w-full gap-2.5">
          <button
            type="button"
            onClick={onDismiss}
            className="flex-1 h-12 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-sm cursor-pointer transition"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={() => {
              const parsed = Number.parseInt(customTimeInput, 10);
              const finalSec = !isNaN(parsed)
                ? Math.min(600, Math.max(5, parsed))
                : Math.min(600, Math.max(5, tempTimeSeconds));
              onApply(finalSec);
              onDismiss();
            }}
            data-testid="apply-time-button"
            className="flex-[1.3] h-12 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm flex items-center justify-center gap-1.5 cursor-pointer transition"
          >
            <span>Apply</span>
          </button>
        </div>
      </div>
    </div>
  );
};

/* ═══════════════════════════════════════════════
   5. QUESTION QUANTITY SELECTOR DIALOG
═══════════════════════════════════════════════ */
interface QuestionQtyDialogProps {
  currentQty: number;
  maxQuestions: number;
  onDismiss: () => void;
  onApply: (qty: number) => void;
}

export const QuestionQtyDialog: React.FC<QuestionQtyDialogProps> = ({
  currentQty,
  maxQuestions,
  onDismiss,
  onApply,
}) => {
  const maxQ = Math.max(1, maxQuestions);
  const [tempQty, setTempQty] = useState<number>(
    Math.min(maxQ, Math.max(1, currentQty))
  );
  const defaultPresets = [5, 10, 15, 20, 30];

  const canDecrease = tempQty > 1;
  const canIncrease = tempQty < maxQ;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Select Number of Questions"
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
    >
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-5.5 shadow-2xl border border-slate-200 dark:border-slate-700 flex flex-col items-center">
        <div className="w-12 h-12 rounded-full bg-gradient-to-br from-blue-600 to-indigo-600 text-white flex items-center justify-center text-2xl shadow mb-2.5">
          🎛️
        </div>
        <h2 className="text-xl font-extrabold text-slate-900 dark:text-white mb-4">
          Select No. of Questions
        </h2>

        {/* Hero Counter Card with - / + Stepper */}
        <div className="w-full rounded-2xl bg-blue-50/90 dark:bg-slate-800 border border-blue-200 dark:border-slate-700 px-4 py-3.5 flex items-center justify-between mb-4">
          <button
            type="button"
            disabled={!canDecrease}
            onClick={() => setTempQty((q) => Math.max(1, q - 1))}
            aria-label="Decrease Questions"
            className="w-10.5 h-10.5 rounded-full bg-white dark:bg-slate-700 text-blue-600 dark:text-blue-400 font-extrabold text-xl shadow-sm disabled:opacity-40 cursor-pointer disabled:cursor-not-allowed flex items-center justify-center"
          >
            −
          </button>

          <div className="text-center">
            <div className="text-3xl font-extrabold text-blue-600 dark:text-blue-400">
              {tempQty}
            </div>
            <div className="text-xs font-semibold text-slate-500 dark:text-slate-400">
              out of {maxQ} Questions
            </div>
          </div>

          <button
            type="button"
            disabled={!canIncrease}
            onClick={() => setTempQty((q) => Math.min(maxQ, q + 1))}
            aria-label="Increase Questions"
            className="w-10.5 h-10.5 rounded-full bg-white dark:bg-slate-700 text-blue-600 dark:text-blue-400 font-extrabold text-xl shadow-sm disabled:opacity-40 cursor-pointer disabled:cursor-not-allowed flex items-center justify-center"
          >
            +
          </button>
        </div>

        {/* Slider */}
        <div className="w-full mb-4">
          <input
            type="range"
            min={1}
            max={Math.max(2, maxQ)}
            disabled={maxQ <= 1}
            value={tempQty}
            onChange={(e) => {
              const v = Number.parseInt(e.target.value, 10) || 1;
              setTempQty(Math.min(maxQ, Math.max(1, v)));
            }}
            aria-label="Question Quantity Slider"
            className="w-full accent-blue-600 cursor-pointer h-2.5 rounded-lg"
          />
          <div className="flex justify-between text-[11px] font-semibold text-slate-500 dark:text-slate-400 px-1 mt-1">
            <span>Min: 1</span>
            <span>Max: {maxQ}</span>
          </div>
        </div>

        {/* Quick Select Preset Chips */}
        <div className="w-full mb-5">
          <div className="text-xs font-bold text-slate-600 dark:text-slate-300 mb-2">
            Quick Select
          </div>
          <div className="flex flex-wrap gap-2">
            {defaultPresets.map((preset) => {
              const isAvailable = preset <= maxQ;
              const isSelected = tempQty === preset && isAvailable;
              return (
                <button
                  key={preset}
                  type="button"
                  onClick={() => setTempQty(Math.min(maxQ, preset))}
                  className={`px-4 py-2 rounded-xl text-sm font-bold border cursor-pointer transition ${
                    isSelected
                      ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                      : isAvailable
                      ? 'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 border-slate-300 dark:border-slate-700 hover:bg-slate-200'
                      : 'bg-slate-50 dark:bg-slate-800/40 text-slate-400 border-slate-200 dark:border-slate-800'
                  }`}
                >
                  {preset}
                </button>
              );
            })}

            <button
              type="button"
              onClick={() => setTempQty(maxQ)}
              className={`px-4 py-2 rounded-xl text-sm font-bold border cursor-pointer transition ${
                tempQty === maxQ
                  ? 'bg-indigo-600 text-white border-indigo-600 shadow-sm'
                  : 'bg-indigo-50 dark:bg-indigo-950/50 text-indigo-700 dark:text-indigo-300 border-indigo-200 dark:border-indigo-800'
              }`}
            >
              All ({maxQ})
            </button>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex w-full gap-2.5">
          <button
            type="button"
            onClick={onDismiss}
            className="flex-1 h-12 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-sm cursor-pointer transition"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={() => {
              onApply(tempQty);
              onDismiss();
            }}
            data-testid="apply-qty-button"
            className="flex-[1.3] h-12 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm flex items-center justify-center gap-1.5 cursor-pointer transition"
          >
            <span>Apply</span>
          </button>
        </div>
      </div>
    </div>
  );
};

/* ═══════════════════════════════════════════════
   6. COPY AI PROMPT DIALOG
═══════════════════════════════════════════════ */
interface CopyPromptDialogProps {
  onDismiss: () => void;
  onCopyPrompt: (options: {
    topicOrNotes: string;
    questionCount: number;
    difficulty: string;
    languageStyle: string;
  }) => void;
}

export const CopyPromptDialog: React.FC<CopyPromptDialogProps> = ({
  onDismiss,
  onCopyPrompt,
}) => {
  const [topicText, setTopicText] = useState<string>('');
  const [selectedCount, setSelectedCount] = useState<number>(20);
  const [customCountInput, setCustomCountInput] = useState<string>('20');
  const [selectedDifficulty, setSelectedDifficulty] =
    useState<string>('Medium');
  const [selectedLanguage, setSelectedLanguage] = useState<string>('Hinglish');

  const countOptions = [10, 20, 30, 50, 100];
  const difficultyOptions = ['Easy', 'Medium', 'Hard', 'Exam'];
  const languageOptions = ['Hinglish', 'Hindi', 'English'];

  const handleSelectCount = (count: number) => {
    setSelectedCount(count);
    setCustomCountInput(String(count));
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Copy AI Prompt"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4"
    >
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-5.5 shadow-2xl border border-slate-200 dark:border-slate-700 flex flex-col items-center max-h-[92vh] overflow-y-auto">
        {/* Header Icon */}
        <div className="w-12 h-12 rounded-full bg-gradient-to-br from-indigo-600 to-blue-600 text-white flex items-center justify-center text-2xl shadow mb-2.5 shrink-0">
          📋
        </div>

        <h2 className="text-xl font-extrabold text-slate-900 dark:text-white mb-3.5 text-center">
          Copy AI Prompt
        </h2>

        {/* Topic / Study Notes Input */}
        <div className="w-full mb-3.5">
          <label
            htmlFor="promptTopicInput"
            className="block text-xs font-bold text-slate-600 dark:text-slate-300 mb-1.5"
          >
            Topic or Study Notes
          </label>
          <textarea
            id="promptTopicInput"
            data-testid="prompt-topic-input"
            rows={3}
            value={topicText}
            onChange={(e) => setTopicText(e.target.value)}
            placeholder="e.g. Trigonometry, HTML tags, or leave blank to paste topic later in AI..."
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs sm:text-sm font-medium outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 resize-none"
          />
        </div>

        {/* Number of Questions */}
        <div className="w-full mb-3.5">
          <div className="flex items-center justify-between mb-1.5">
            <label
              htmlFor="promptCustomCountInput"
              className="text-xs font-bold text-slate-600 dark:text-slate-300"
            >
              Number of Questions
            </label>
            <input
              id="promptCustomCountInput"
              data-testid="prompt-custom-count-input"
              type="number"
              min={1}
              max={200}
              value={customCountInput}
              onChange={(e) => {
                const raw = e.target.value;
                setCustomCountInput(raw);
                const parsed = Number.parseInt(raw, 10);
                if (!Number.isNaN(parsed) && parsed >= 1) {
                  setSelectedCount(Math.min(200, Math.max(1, parsed)));
                }
              }}
              className="w-20 px-2.5 py-1 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs font-extrabold text-center outline-none focus:border-blue-500"
            />
          </div>

          <div className="flex flex-wrap gap-2">
            {countOptions.map((count) => {
              const isSelected = selectedCount === count;
              return (
                <button
                  key={count}
                  type="button"
                  data-testid={`prompt-qty-${count}`}
                  onClick={() => handleSelectCount(count)}
                  className={`px-3.5 py-1.5 rounded-xl text-xs font-bold border cursor-pointer transition ${
                    isSelected
                      ? 'bg-blue-600 text-white border-blue-600 shadow-xs'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 border-slate-300 dark:border-slate-700 hover:bg-slate-200'
                  }`}
                >
                  {count}
                </button>
              );
            })}
          </div>
        </div>

        {/* Difficulty Level */}
        <div className="w-full mb-3.5">
          <div className="text-xs font-bold text-slate-600 dark:text-slate-300 mb-1.5">
            Difficulty Level
          </div>
          <div className="flex flex-wrap gap-2">
            {difficultyOptions.map((diff) => {
              const isSelected = selectedDifficulty === diff;
              return (
                <button
                  key={diff}
                  type="button"
                  data-testid={`prompt-diff-${diff}`}
                  onClick={() => setSelectedDifficulty(diff)}
                  className={`px-3.5 py-1.5 rounded-xl text-xs font-bold border cursor-pointer transition ${
                    isSelected
                      ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 border-slate-300 dark:border-slate-700 hover:bg-slate-200'
                  }`}
                >
                  {diff}
                </button>
              );
            })}
          </div>
        </div>

        {/* Language Selector */}
        <div className="w-full mb-5">
          <div className="text-xs font-bold text-slate-600 dark:text-slate-300 mb-1.5">
            Language
          </div>
          <div className="flex flex-wrap gap-2">
            {languageOptions.map((lang) => {
              const isSelected = selectedLanguage === lang;
              return (
                <button
                  key={lang}
                  type="button"
                  data-testid={`prompt-lang-${lang}`}
                  onClick={() => setSelectedLanguage(lang)}
                  className={`px-3.5 py-1.5 rounded-xl text-xs font-bold border cursor-pointer transition ${
                    isSelected
                      ? 'bg-violet-600 text-white border-violet-600 shadow-xs'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 border-slate-300 dark:border-slate-700 hover:bg-slate-200'
                  }`}
                >
                  {lang}
                </button>
              );
            })}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex w-full gap-2.5">
          <button
            type="button"
            onClick={onDismiss}
            className="flex-1 h-11 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-sm cursor-pointer transition"
          >
            Cancel
          </button>
          <button
            type="button"
            data-testid="generate-copy-prompt-button"
            onClick={() => {
              onCopyPrompt({
                topicOrNotes: topicText,
                questionCount: selectedCount,
                difficulty: selectedDifficulty,
                languageStyle: selectedLanguage,
              });
              onDismiss();
            }}
            className="flex-[1.4] h-11 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm flex items-center justify-center gap-1.5 cursor-pointer transition shadow-sm"
          >
            <span>Copy</span>
          </button>
        </div>
      </div>
    </div>
  );
};

