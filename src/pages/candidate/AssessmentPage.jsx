import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import { assessmentsAPI } from '../../api';
import { useAuth } from '../../context/AuthContext';
import Modal from '../../components/ui/Modal';
import toast from 'react-hot-toast';
import {
  Clock, Send, ChevronLeft, ChevronRight,
  Flag, CheckCircle, FileText, UploadCloud, X, Loader2
} from 'lucide-react';

const LANGUAGES = [
  { value: 'java', label: 'Java', monacoLang: 'java' },
  { value: 'python', label: 'Python', monacoLang: 'python' },
  { value: 'cpp', label: 'C++', monacoLang: 'cpp' },
  { value: 'c', label: 'C', monacoLang: 'c' },
  { value: 'javascript', label: 'JavaScript', monacoLang: 'javascript' },
];

const STARTER_CODE = {
  java: `class Solution {\n    // Write your solution here\n}`,
  python: `class Solution:\n    def solve(self):\n        pass`,
  cpp: `class Solution {\npublic:\n    // Write your solution here\n};`,
  c: `// Write your solution here`,
  javascript: `class Solution {\n    // Write your solution here\n}`,
};

// ─── Autosave Hook ───
function useAutosave(saveFn) {
  const timerRef = useRef(null);
  const lastSavedRef = useRef(null);
  const [savedAgo, setSavedAgo] = useState(null);

  useEffect(() => {
    const tick = () => {
      if (lastSavedRef.current) {
        const secs = Math.floor((Date.now() - lastSavedRef.current) / 1000);
        if (secs < 60) setSavedAgo(`${secs}s ago`);
        else setSavedAgo(`${Math.floor(secs / 60)}m ago`);
      }
    };
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, []);

  const trigger = useCallback(() => {
    clearTimeout(timerRef.current);
    timerRef.current = setTimeout(async () => {
      try {
        await saveFn();
        lastSavedRef.current = Date.now();
        setSavedAgo('0s ago');
      } catch { /* autosave failed silently */ }
    }, 1000);
  }, [saveFn]);

  const saveNow = useCallback(async () => {
    clearTimeout(timerRef.current);
    try {
      await saveFn();
      lastSavedRef.current = Date.now();
      setSavedAgo('0s ago');
    } catch { /* ignore */ }
  }, [saveFn]);

  return { trigger, saveNow, savedAgo };
}

// ─── Question Navigator Sidebar ───
function QuestionNavigator({ sections, currentIndex, answers, flagged, onJump }) {
  let qIndex = 0;
  return (
    <div className="qn-sidebar">
      <div className="qn-title">Questions</div>
      {sections.map((section, si) => {
        const startIdx = qIndex;
        return (
          <div key={section.id || si} className="qn-section">
            <div className="qn-section-label">{section.title || `Section ${si + 1}`}</div>
            <div className="qn-grid">
              {(section.questions || []).map((q, qi) => {
                const globalIdx = startIdx + qi;
                const answer = answers[q.id];
                const isAnswered = isQuestionAnswered(q, answer);
                const isFlagged = flagged.has(q.id);
                const isCurrent = globalIdx === currentIndex;
                qIndex++;
                return (
                  <button
                    key={q.id || qi}
                    className={`qn-cell ${isCurrent ? 'qn-current' : ''} ${isAnswered ? 'qn-answered' : 'qn-unanswered'} ${isFlagged ? 'qn-flagged' : ''}`}
                    onClick={() => onJump(globalIdx)}
                    title={`Q${globalIdx + 1}${isAnswered ? ' (answered)' : ''}${isFlagged ? ' (flagged)' : ''}`}
                  >
                    {globalIdx + 1}
                    {isFlagged && <Flag size={8} className="qn-flag-icon" />}
                  </button>
                );
              })}
            </div>
          </div>
        );
      })}
      <div className="qn-legend">
        <span className="qn-legend-item"><span className="qn-dot qn-dot-answered" /> Answered</span>
        <span className="qn-legend-item"><span className="qn-dot qn-dot-unanswered" /> Unanswered</span>
        <span className="qn-legend-item"><span className="qn-dot qn-dot-flagged" /> Flagged</span>
      </div>
    </div>
  );
}

function isQuestionAnswered(q, answer) {
  if (!answer) return false;
  switch (q.type || q.questionType) {
    case 'MCQ_SINGLE':
      return answer.selectedOption != null;
    case 'MCQ_MULTI':
      return answer.selectedOptions?.length > 0;
    case 'CODING':
      return answer.code?.trim().length > 0;
    case 'SUBJECTIVE':
      return answer.text?.trim().length > 0;
    case 'READING_COMPREHENSION':
      return Object.keys(answer.subAnswers || {}).length > 0;
    case 'FILE_UPLOAD':
      return answer.fileId != null || answer.fileName != null;
    default:
      return false;
  }
}

// ─── MCQ Single Renderer ───
function MCQSingleRenderer({ question, value, onChange }) {
  const options = question.options || [];
  return (
    <div className="mcq-options">
      {options.map((opt, i) => (
        <label key={opt.id || i} className={`mcq-option ${value === (opt.id || i) ? 'mcq-selected' : ''}`}>
          <input
            type="radio"
            name={`q-${question.id}`}
            checked={value === (opt.id || i)}
            onChange={() => onChange(opt.id ?? i)}
          />
          <span className="mcq-radio" />
          <span className="mcq-text">{opt.text || opt.content || opt}</span>
        </label>
      ))}
    </div>
  );
}

// ─── MCQ Multi Renderer ───
function MCQMultiRenderer({ question, value = [], onChange }) {
  const options = question.options || [];
  const toggle = (optId) => {
    const id = optId ?? options.indexOf(optId);
    const next = value.includes(id) ? value.filter(v => v !== id) : [...value, id];
    onChange(next);
  };
  return (
    <div className="mcq-options">
      {options.map((opt, i) => {
        const optId = opt.id ?? i;
        return (
          <label key={optId} className={`mcq-option ${value.includes(optId) ? 'mcq-selected' : ''}`}>
            <input
              type="checkbox"
              checked={value.includes(optId)}
              onChange={() => toggle(optId)}
            />
            <span className="mcq-checkbox" />
            <span className="mcq-text">{opt.text || opt.content || opt}</span>
          </label>
        );
      })}
    </div>
  );
}

// ─── Coding Renderer ───
function CodingRenderer({ question, answer, onChange }) {
  const [language, setLanguage] = useState(answer?.language || 'java');
  const [code, setCode] = useState(answer?.code || question.starterCode?.java || STARTER_CODE.java);
  const [runResult, setRunResult] = useState(null);
  const [running, setRunning] = useState(false);

  const handleCodeChange = (val) => {
    setCode(val || '');
    onChange({ code: val || '', language });
  };

  const handleLanguageChange = (lang) => {
    setLanguage(lang);
    const starter = question.starterCode?.[lang] || STARTER_CODE[lang];
    if (!code.trim() || code === (question.starterCode?.[answer?.language] || STARTER_CODE[answer?.language] || '')) {
      setCode(starter);
      onChange({ code: starter, language: lang });
    } else {
      onChange({ code, language: lang });
    }
  };

  const handleRun = async () => {
    setRunning(true);
    try {
      const res = await assessmentsAPI.runCode(question.assessmentId || question.assessmentQuestionId, question.id, {
        code, language,
        // TODO: backend endpoint pending — see AssessmentController
      });
      setRunResult(res.data);
    } catch {
      toast.error('Run failed. The backend endpoint may not be ready yet.');
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="coding-renderer">
      <div className="editor-toolbar">
        <select className="select" value={language} onChange={e => handleLanguageChange(e.target.value)}>
          {LANGUAGES.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
        </select>
        <span style={{ marginLeft: 'auto', fontSize: 12, color: 'var(--text-muted)' }}>
          {question.timeLimit && `${question.timeLimit}ms limit`}
          {question.memoryLimit && ` · ${Math.round(question.memoryLimit / 1024)}MB`}
        </span>
        <button className="btn btn-secondary btn-sm" onClick={handleRun} disabled={running}>
          {running ? <><Loader2 size={14} className="spin" /> Running...</> : '▶ Run'}
        </button>
      </div>
      <div className="editor-wrapper" style={{ minHeight: 300 }}>
        <Editor
          height="300px"
          language={LANGUAGES.find(l => l.value === language)?.monacoLang || 'java'}
          value={code}
          onChange={handleCodeChange}
          theme="vs-dark"
          options={{
            fontSize: 14,
            fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
            fontLigatures: true,
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            wordWrap: 'on',
            padding: { top: 16 },
            lineNumbersMinChars: 3,
            smoothScrolling: true,
            cursorBlinking: 'phase',
            cursorSmoothCaretAnimation: 'on',
          }}
        />
      </div>
      {runResult && (
        <div className="run-result-panel">
          <div style={{ fontSize: 12, fontWeight: 600, textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: 8 }}>
            Output
          </div>
          {runResult.status && (
            <div className={`badge ${runResult.status === 'ACCEPTED' ? 'badge-accepted' : 'badge-wrong'}`} style={{ marginBottom: 8 }}>
              {runResult.status}
            </div>
          )}
          <pre style={{ fontSize: 12, fontFamily: 'JetBrains Mono, monospace', color: 'var(--text-secondary)', whiteSpace: 'pre-wrap' }}>
            {runResult.output || runResult.errorOutput || runResult.errorMessage || 'No output'}
          </pre>
        </div>
      )}
    </div>
  );
}

// ─── Subjective/Written Renderer ───
function SubjectiveRenderer({ question, value = '', onChange }) {
  const wordCount = value.trim() ? value.trim().split(/\s+/).length : 0;
  const minWords = question.minWords || 0;
  const maxWords = question.maxWords || question.wordLimit || 1000;
  const isUnderMin = minWords > 0 && wordCount < minWords;
  const isOverMax = maxWords > 0 && wordCount > maxWords;

  return (
    <div className="subjective-renderer">
      <textarea
        className="input subjective-textarea"
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder="Type your answer here..."
        style={{ minHeight: 200, width: '100%', resize: 'vertical' }}
      />
      <div className="subjective-footer">
        <div className="word-count" style={{ color: isOverMax ? 'var(--red)' : isUnderMin ? 'var(--yellow)' : 'var(--text-muted)' }}>
          {wordCount} / {maxWords} words
          {minWords > 0 && ` (min: ${minWords})`}
        </div>
      </div>
    </div>
  );
}

// ─── Reading Comprehension Renderer ───
function ReadingCompRenderer({ question, answer = {}, onChange }) {
  const [readingDone, setReadingDone] = useState(false);
  const [readingTimeLeft, setReadingTimeLeft] = useState(question.readingTimeSeconds || 120);
  const subQuestions = question.subQuestions || [];

  useEffect(() => {
    if (readingDone || !question.readingTimeSeconds) { setReadingDone(true); return; }
    const timer = setInterval(() => {
      setReadingTimeLeft(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          setReadingDone(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [question.readingTimeSeconds, readingDone]);

  const handleSubAnswer = (subId, val) => {
    onChange({ ...answer, subAnswers: { ...answer.subAnswers, [subId]: val } });
  };

  const formatTime = (s) => `${Math.floor(s / 60)}:${(s % 60).toString().padStart(2, '0')}`;

  return (
    <div className="reading-comp-renderer">
      {/* Passage Panel */}
      <div className="passage-panel">
        <div className="passage-header">
          <FileText size={16} />
          <span>Reading Passage</span>
          {!readingDone && question.readingTimeSeconds && (
            <span className="passage-timer">
              <Clock size={14} /> {formatTime(readingTimeLeft)}
            </span>
          )}
          {!readingDone && (
            <button className="btn btn-secondary btn-sm" style={{ marginLeft: 'auto' }} onClick={() => setReadingDone(true)}>
              I've finished reading
            </button>
          )}
        </div>
        <div className="passage-content">
          {question.passage || question.content}
        </div>
      </div>

      {/* Sub-Questions */}
      {readingDone ? (
        <div className="sub-questions">
          {subQuestions.map((sq, i) => (
            <div key={sq.id || i} className="sub-question">
              <h4 style={{ fontSize: 14, fontWeight: 600, marginBottom: 8 }}>Question {i + 1}</h4>
              <p style={{ fontSize: 14, marginBottom: 12 }}>{sq.question || sq.text || sq.description}</p>
              {sq.type === 'MCQ_SINGLE' || sq.type === 'MCQ' ? (
                <MCQSingleRenderer
                  question={sq}
                  value={answer.subAnswers?.[sq.id || i]}
                  onChange={(val) => handleSubAnswer(sq.id || i, val)}
                />
              ) : (
                <textarea
                  className="input"
                  value={answer.subAnswers?.[sq.id || i] || ''}
                  onChange={e => handleSubAnswer(sq.id || i, e.target.value)}
                  placeholder="Type your answer..."
                  style={{ minHeight: 80, width: '100%', resize: 'vertical' }}
                />
              )}
            </div>
          ))}
        </div>
      ) : (
        <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
          Please read the passage carefully. Questions will appear after the reading phase.
        </div>
      )}
    </div>
  );
}

// ─── File Upload Renderer ───
function FileUploadRenderer({ question, answer = {}, onChange }) {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [dragOver, setDragOver] = useState(false);
  const inputRef = useRef(null);

  const maxSizeMB = question.maxFileSizeMB || 10;
  const maxSizeBytes = maxSizeMB * 1024 * 1024;
  const allowedTypes = question.allowedFileTypes || [];

  const validateFile = (file) => {
    if (file.size > maxSizeBytes) {
      toast.error(`File too large. Max size: ${maxSizeMB}MB`);
      return false;
    }
    if (allowedTypes.length > 0) {
      const ext = file.name.split('.').pop().toLowerCase();
      if (!allowedTypes.some(t => t.toLowerCase().includes(ext) || ext.includes(t.toLowerCase().replace('.', '')))) {
        toast.error(`Invalid file type. Allowed: ${allowedTypes.join(', ')}`);
        return false;
      }
    }
    return true;
  };

  const handleFile = async (file) => {
    if (!validateFile(file)) return;
    setUploading(true);
    setProgress(0);
    try {
      const fd = new FormData();
      fd.append('file', file);
      // TODO: backend endpoint pending — see AssessmentController
      const res = await assessmentsAPI.uploadFile(question.assessmentId, question.id, fd);
      onChange({ fileId: res.data?.id, fileName: file.name, fileSize: file.size });
      toast.success('File uploaded successfully');
    } catch {
      toast.error('Upload failed. The backend endpoint may not be ready yet.');
      onChange({ fileName: file.name, fileSize: file.size });
    } finally {
      setUploading(false);
      setProgress(0);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) handleFile(file);
  };

  return (
    <div className="file-upload-renderer">
      {answer.fileName ? (
        <div className="file-uploaded">
          <FileText size={32} style={{ color: 'var(--green)' }} />
          <div>
            <div style={{ fontWeight: 600 }}>{answer.fileName}</div>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
              {answer.fileSize ? `${(answer.fileSize / 1024).toFixed(1)} KB` : ''}
            </div>
          </div>
          <button className="btn btn-ghost btn-sm" onClick={() => onChange({})}>
            <X size={14} /> Remove
          </button>
        </div>
      ) : (
        <div
          className={`file-drop-zone ${dragOver ? 'file-drop-active' : ''}`}
          onDragOver={e => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          onClick={() => inputRef.current?.click()}
        >
          <input
            ref={inputRef}
            type="file"
            style={{ display: 'none' }}
            accept={allowedTypes.join(',')}
            onChange={e => e.target.files?.[0] && handleFile(e.target.files[0])}
          />
          {uploading ? (
            <div style={{ textAlign: 'center' }}>
              <Loader2 size={32} className="spin" style={{ color: 'var(--accent-light)' }} />
              <div style={{ marginTop: 8, fontSize: 14 }}>Uploading...</div>
              {progress > 0 && (
                <div className="upload-progress-bar">
                  <div className="upload-progress-fill" style={{ width: `${progress}%` }} />
                </div>
              )}
            </div>
          ) : (
            <div style={{ textAlign: 'center' }}>
              <UploadCloud size={32} style={{ color: 'var(--text-muted)' }} />
              <div style={{ marginTop: 8, fontSize: 14, color: 'var(--text-secondary)' }}>
                Drag & drop a file here, or <span style={{ color: 'var(--accent-light)' }}>browse</span>
              </div>
              <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>
                Max size: {maxSizeMB}MB{allowedTypes.length > 0 ? ` · Allowed: ${allowedTypes.join(', ')}` : ''}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ─── Submit Confirmation Modal ───
function SubmitModal({ isOpen, onClose, onConfirm, answered, total, submitting }) {
  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Submit Assessment"
      footer={
        <>
          <button className="btn btn-secondary" onClick={onClose} disabled={submitting}>Cancel</button>
          <button className="btn btn-success" onClick={onConfirm} disabled={submitting}>
            {submitting ? <><Loader2 size={14} className="spin" /> Submitting...</> : <><Send size={14} /> Confirm Submit</>}
          </button>
        </>
      }
    >
      <div style={{ textAlign: 'center', padding: '12px 0' }}>
        <div style={{ fontSize: 48, marginBottom: 12 }}>
          {answered === total ? '🎉' : answered > total / 2 ? '📝' : '⚠️'}
        </div>
        <p style={{ fontSize: 15, marginBottom: 16 }}>
          You have answered <strong style={{ color: 'var(--green)' }}>{answered}</strong> of <strong>{total}</strong> questions.
        </p>
        {answered < total && (
          <p style={{ fontSize: 14, color: 'var(--yellow)', marginBottom: 12 }}>
            {total - answered} question{total - answered !== 1 ? 's' : ''} remain unanswered.
          </p>
        )}
        <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
          The server enforces the real deadline. This confirmation is a client-side safeguard only.
        </p>
      </div>
    </Modal>
  );
}

// ─── Flatten helpers ───
function flattenQuestions(sections) {
  const flat = [];
  (sections || []).forEach(section => {
    (section.questions || []).forEach(q => {
      flat.push({ ...q, sectionTitle: section.title });
    });
  });
  return flat;
}

// ─── Main Component ───
export default function AssessmentPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();

  const [assessment, setAssessment] = useState(null);
  const [sections, setSections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sessionStarted, setSessionStarted] = useState(false);
  const [activeSection, setActiveSection] = useState(0);

  const [answers, setAnswers] = useState({});
  const [flagged, setFlagged] = useState(new Set());
  const [currentIndex, setCurrentIndex] = useState(0);
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [deadline, setDeadline] = useState(null);
  const [timeLeft, setTimeLeft] = useState(null);

  const allQuestions = flattenQuestions(sections);
  const currentQuestion = allQuestions[currentIndex];

  // ─── Session start/resume ───
  useEffect(() => {
    if (!isLoggedIn) { navigate('/login'); return; }
    initSession();
  }, [id, isLoggedIn]); // eslint-disable-line react-hooks/exhaustive-deps

  const initSession = async () => {
    setLoading(true);
    try {
      const assessmentRes = await assessmentsAPI.getById(id);
      const asm = assessmentRes.data;
      setAssessment(asm);
      setSections(asm.sections || []);

      try {
        const sessionRes = await assessmentsAPI.getSession(id);
        const session = sessionRes.data;
        if (session) {
          setSessionStarted(true);
          setAnswers(session.answers || {});
          if (session.deadline) setDeadline(new Date(session.deadline));
          // Resume from last answered question
          if (session.lastQuestionIndex != null) setCurrentIndex(session.lastQuestionIndex);
        }
      } catch {
        // No existing session — start one
        const startRes = await assessmentsAPI.startSession(id);
        const session = startRes.data;
        setSessionStarted(true);
        if (session?.deadline) setDeadline(new Date(session.deadline));
        if (session?.answers) setAnswers(session.answers);
      }
    } catch {
      toast.error('Could not load assessment. Showing demo data.');
      setAssessment(MOCK_ASSESSMENT);
      setSections(MOCK_ASSESSMENT.sections);
      setSessionStarted(true);
      setDeadline(new Date(Date.now() + 60 * 60 * 1000));
    } finally {
      setLoading(false);
    }
  };

  // ─── Countdown timer (client-side display only — server enforces the real deadline) ───
  useEffect(() => {
    if (!deadline) return;
    const interval = setInterval(() => {
      const diff = deadline - new Date();
      if (diff <= 0) {
        setTimeLeft(0);
        clearInterval(interval);
        toast.error('Time is up! Auto-submitting...');
        handleSubmit(true);
      } else {
        setTimeLeft(diff);
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [deadline]); // eslint-disable-line react-hooks/exhaustive-deps

  // ─── Autosave ───
  const saveFn = useCallback(async () => {
    if (!sessionStarted || !assessment) return;
    await assessmentsAPI.autosaveAll(id, answers);
  }, [id, answers, sessionStarted, assessment]);

  const { trigger: triggerAutosave, savedAgo } = useAutosave(saveFn);

  useEffect(() => { triggerAutosave(); }, [answers]); // eslint-disable-line react-hooks/exhaustive-deps

  // ─── Answer update ───
  const updateAnswer = (questionId, data) => {
    setAnswers(prev => ({
      ...prev,
      [questionId]: { ...prev[questionId], ...data },
    }));
  };

  // ─── Navigation ───
  const goNext = () => {
    if (currentIndex < allQuestions.length - 1) setCurrentIndex(currentIndex + 1);
  };
  const goPrev = () => {
    if (currentIndex > 0) setCurrentIndex(currentIndex - 1);
  };
  const toggleFlag = () => {
    if (!currentQuestion) return;
    setFlagged(prev => {
      const next = new Set(prev);
      if (next.has(currentQuestion.id)) next.delete(currentQuestion.id);
      else next.add(currentQuestion.id);
      return next;
    });
  };

  // ─── Submit ───
  const handleSubmit = async (forced = false) => {
    if (!forced) {
      setShowSubmitModal(true);
      return;
    }
    setSubmitting(true);
    setShowSubmitModal(false);
    try {
      await assessmentsAPI.submit(id);
      toast.success('Assessment submitted successfully!');
      navigate(`/assessments/${id}/result`);
    } catch {
      toast.error('Submit failed. The server may not be ready yet.');
      navigate(`/assessments/${id}/result`);
    } finally {
      setSubmitting(false);
    }
  };

  // ─── Format time ───
  const formatTimeLeft = (ms) => {
    if (ms == null) return '--:--';
    const totalSecs = Math.floor(ms / 1000);
    const h = Math.floor(totalSecs / 3600);
    const m = Math.floor((totalSecs % 3600) / 60);
    const s = totalSecs % 60;
    if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  // ─── Counters ───
  const answeredCount = allQuestions.filter(q => isQuestionAnswered(q, answers[q.id])).length;
  const totalCount = allQuestions.length;

  if (loading) {
    return (
      <div className="assessment-layout-loading">
        <div className="spinner" style={{ width: 40, height: 40 }} />
        <div style={{ marginTop: 16, color: 'var(--text-secondary)' }}>Loading assessment...</div>
      </div>
    );
  }

  if (!assessment || !currentQuestion) {
    return (
      <div className="assessment-layout-loading">
        <div style={{ color: 'var(--text-secondary)' }}>No questions available.</div>
      </div>
    );
  }

  // Find current section
  let runningIdx = 0;
  for (let si = 0; si < sections.length; si++) {
    const sqs = sections[si].questions || [];
    if (currentIndex < runningIdx + sqs.length) {
      break;
    }
    runningIdx += sqs.length;
  }

  return (
    <div className="assessment-layout">
      {/* ─── Top Bar ─── */}
      <div className="assessment-topbar">
        <div className="assessment-topbar-left">
          <div className="assessment-live-dot" />
          <span className="assessment-topbar-title">{assessment.title}</span>
        </div>
        <div className="assessment-topbar-center">
          <span className="assessment-topbar-progress">
            {answeredCount}/{totalCount} answered
          </span>
          {savedAgo && (
            <span className="assessment-autosave">Saved {savedAgo}</span>
          )}
        </div>
        <div className="assessment-topbar-right">
          <div className={`assessment-countdown ${timeLeft != null && timeLeft < 300000 ? 'assessment-countdown-danger' : ''}`}>
            <Clock size={16} />
            <span>{formatTimeLeft(timeLeft)}</span>
          </div>
          <button className="btn btn-primary btn-sm" onClick={() => handleSubmit(false)} disabled={submitting}>
            <Send size={14} /> Submit
          </button>
        </div>
      </div>

      <div className="assessment-body">
        {/* ─── Left: Section Tabs ─── */}
        <div className="assessment-main">
          <div className="assessment-section-tabs">
            {sections.map((sec, si) => {
              const secQs = flattenQuestions([sec]);
              const secAnswered = secQs.filter(q => isQuestionAnswered(q, answers[q.id])).length;
              const isComplete = secAnswered === secQs.length;
              return (
                <button
                  key={sec.id || si}
                  className={`assessment-section-tab ${si === activeSection ? 'active' : ''}`}
                  onClick={() => {
                    setActiveSection(si);
                    // Jump to first question of this section
                    let idx = 0;
                    for (let j = 0; j < si; j++) idx += (sections[j].questions || []).length;
                    setCurrentIndex(idx);
                  }}
                >
                  {isComplete && <CheckCircle size={14} />}
                  {sec.title || `Section ${si + 1}`}
                  <span className="section-tab-count">{secAnswered}/{secQs.length}</span>
                </button>
              );
            })}
          </div>

          {/* ─── Question Content ─── */}
          <div className="assessment-question-area">
            <div className="assessment-question-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                <span className="assessment-question-number">Q{currentIndex + 1}</span>
                <span className="badge badge-tag">{currentQuestion.type || currentQuestion.questionType}</span>
                {currentQuestion.points && (
                  <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{currentQuestion.points} pts</span>
                )}
              </div>
              <button
                className={`btn btn-ghost btn-sm ${flagged.has(currentQuestion.id) ? 'flagged-active' : ''}`}
                onClick={toggleFlag}
              >
                <Flag size={14} fill={flagged.has(currentQuestion.id) ? 'var(--yellow)' : 'none'} color={flagged.has(currentQuestion.id) ? 'var(--yellow)' : undefined} />
                {flagged.has(currentQuestion.id) ? 'Flagged' : 'Flag'}
              </button>
            </div>

            <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 12 }}>{currentQuestion.title || currentQuestion.questionTitle}</h3>
            <div style={{ fontSize: 14, lineHeight: 1.7, color: 'var(--text-secondary)', marginBottom: 20, whiteSpace: 'pre-wrap' }}>
              {currentQuestion.description || currentQuestion.text || currentQuestion.questionDescription}
            </div>

            {/* ─── Question Type Renderer ─── */}
            {(() => {
              const qType = currentQuestion.type || currentQuestion.questionType;
              const ans = answers[currentQuestion.id];
              switch (qType) {
                case 'MCQ_SINGLE':
                  return (
                    <MCQSingleRenderer
                      question={currentQuestion}
                      value={ans?.selectedOption}
                      onChange={(val) => updateAnswer(currentQuestion.id, { selectedOption: val })}
                    />
                  );
                case 'MCQ_MULTI':
                  return (
                    <MCQMultiRenderer
                      question={currentQuestion}
                      value={ans?.selectedOptions || []}
                      onChange={(val) => updateAnswer(currentQuestion.id, { selectedOptions: val })}
                    />
                  );
                case 'CODING':
                  return (
                    <CodingRenderer
                      question={currentQuestion}
                      answer={ans}
                      onChange={(data) => updateAnswer(currentQuestion.id, data)}
                    />
                  );
                case 'SUBJECTIVE':
                case 'WRITTEN':
                  return (
                    <SubjectiveRenderer
                      question={currentQuestion}
                      value={ans?.text || ''}
                      onChange={(val) => updateAnswer(currentQuestion.id, { text: val })}
                    />
                  );
                case 'READING_COMPREHENSION':
                  return (
                    <ReadingCompRenderer
                      question={currentQuestion}
                      answer={ans || {}}
                      onChange={(data) => updateAnswer(currentQuestion.id, data)}
                    />
                  );
                case 'FILE_UPLOAD':
                  return (
                    <FileUploadRenderer
                      question={currentQuestion}
                      answer={ans || {}}
                      onChange={(data) => updateAnswer(currentQuestion.id, data)}
                    />
                  );
                default:
                  return (
                    <div style={{ padding: 24, color: 'var(--text-muted)', textAlign: 'center' }}>
                      Unsupported question type: {qType}
                    </div>
                  );
              }
            })()}
          </div>

          {/* ─── Navigation Footer ─── */}
          <div className="assessment-nav-footer">
            <button className="btn btn-secondary btn-sm" onClick={goPrev} disabled={currentIndex === 0}>
              <ChevronLeft size={14} /> Previous
            </button>
            <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>
              {currentIndex + 1} of {totalCount}
            </span>
            {currentIndex < totalCount - 1 ? (
              <button className="btn btn-primary btn-sm" onClick={goNext}>
                Next <ChevronRight size={14} />
              </button>
            ) : (
              <button className="btn btn-success btn-sm" onClick={() => handleSubmit(false)}>
                <Send size={14} /> Submit Assessment
              </button>
            )}
          </div>
        </div>

        {/* ─── Right: Question Navigator ─── */}
        <QuestionNavigator
          sections={sections}
          currentIndex={currentIndex}
          answers={answers}
          flagged={flagged}
          onJump={(idx) => setCurrentIndex(idx)}
        />
      </div>

      {/* ─── Submit Modal ─── */}
      <SubmitModal
        isOpen={showSubmitModal}
        onClose={() => setShowSubmitModal(false)}
        onConfirm={() => handleSubmit(true)}
        answered={answeredCount}
        total={totalCount}
        submitting={submitting}
      />
    </div>
  );
}

// ─── Mock Data ───
const MOCK_ASSESSMENT = {
  id: 1, title: 'Frontend Engineer Assessment', durationMinutes: 120,
  sections: [
    {
      id: 1, title: 'Data Structures (MCQ)', questions: [
        {
          id: 101, type: 'MCQ_SINGLE', title: 'Time Complexity of HashMap',
          description: 'What is the average time complexity for lookup in a HashMap?',
          points: 5,
          options: [
            { id: 1, text: 'O(1)' },
            { id: 2, text: 'O(n)' },
            { id: 3, text: 'O(log n)' },
            { id: 4, text: 'O(n log n)' },
          ],
        },
        {
          id: 102, type: 'MCQ_MULTI', title: 'Characteristics of Trees',
          description: 'Which of the following are properties of a binary search tree? (Select all that apply)',
          points: 5,
          options: [
            { id: 1, text: 'Left child is less than parent' },
            { id: 2, text: 'Right child is greater than parent' },
            { id: 3, text: 'Each node has exactly two children' },
            { id: 4, text: 'In-order traversal yields sorted order' },
          ],
        },
      ],
    },
    {
      id: 2, title: 'Coding Challenge', questions: [
        {
          id: 201, type: 'CODING', title: 'Two Sum', assessmentId: 1,
          description: 'Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.',
          points: 20, timeLimit: 2000, memoryLimit: 262144,
          starterCode: { java: 'class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        \n    }\n}' },
        },
      ],
    },
    {
      id: 3, title: 'Written Response', questions: [
        {
          id: 301, type: 'SUBJECTIVE', title: 'Design a URL Shortener',
          description: 'Describe your approach to designing a URL shortener service like bit.ly. Include considerations for scalability, storage, and the encoding algorithm.',
          points: 15, minWords: 100, maxWords: 500,
        },
      ],
    },
    {
      id: 4, title: 'Reading & Analysis', questions: [
        {
          id: 401, type: 'READING_COMPREHENSION', title: 'Technical Document Analysis',
          description: 'Read the passage below and answer the questions that follow.',
          points: 10, readingTimeSeconds: 60,
          passage: 'Microservices architecture structures an application as a collection of loosely coupled, independently deployable services. Each service runs its own process and communicates through lightweight protocols, often HTTP/REST or message queues.\n\nKey benefits include: technology heterogeneity (each service can use different tech stacks), independent deployability, fault isolation, and scalability at the service level. However, microservices also introduce complexity in areas like distributed transactions, data consistency, network latency, and operational overhead.\n\nA common pattern is the API Gateway, which acts as a single entry point for all client requests, handling routing, authentication, and rate limiting. Service discovery enables services to find each other without hard-coded addresses, while circuit breakers prevent cascading failures.',
          subQuestions: [
            { id: '401a', type: 'MCQ_SINGLE', question: 'What is the primary purpose of an API Gateway?', options: [
              { id: 1, text: 'Load balancing only' },
              { id: 2, text: 'Single entry point for client requests' },
              { id: 3, text: 'Database management' },
              { id: 4, text: 'Code compilation' },
            ]},
            { id: '401b', type: 'SUBJECTIVE', question: 'Explain how circuit breakers prevent cascading failures in a microservices architecture.' },
          ],
        },
      ],
    },
    {
      id: 5, title: 'File Submission', questions: [
        {
          id: 501, type: 'FILE_UPLOAD', title: 'Upload Your Portfolio',
          description: 'Upload a PDF document containing your project portfolio or resume.',
          points: 5, maxFileSizeMB: 10, allowedFileTypes: ['.pdf', '.doc', '.docx'],
        },
      ],
    },
  ],
};
