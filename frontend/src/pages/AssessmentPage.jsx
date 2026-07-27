import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import toast from 'react-hot-toast';
import { assessmentAPI, proctoringAPI } from '../api';
import { useAuth } from '../context/AuthContext';

const QUESTION_TYPES = {
  MCQ_SINGLE: 'MCQ_SINGLE',
  MCQ_MULTI: 'MCQ_MULTI',
  CODING: 'CODING',
  SUBJECTIVE: 'SUBJECTIVE',
  WRITTEN: 'WRITTEN',
  READING_COMPREHENSION: 'READING_COMPREHENSION',
  FILE_UPLOAD: 'FILE_UPLOAD',
};

const STARTER_CODE = {
  java: `public class Main {\n  public static void main(String[] args) {\n    System.out.println("hello");\n  }\n}`,
  python: `def solve():\n    print("hello")\n\nsolve()` ,
  cpp: `#include <iostream>\nusing namespace std;\nint main(){ cout << "hello"; }`,
  c: `#include <stdio.h>\nint main(){ puts("hello"); }`,
  javascript: `console.log('hello');`,
};

const LANGUAGES = [
  { value: 'java', label: 'Java', monacoLan: 'java' },
  { value: 'python', label: 'Python', monacoLan: 'python' },
  { value: 'cpp', label: 'C++', monacoLan: 'cpp' },
  { value: 'c', label: 'C', monacoLan: 'c' },
  { value: 'javascript', label: 'JavaScript', monacoLan: 'javascript' },
];

function getInitialState() {
  try {
    return JSON.parse(localStorage.getItem('codesphere-assessment-progress') || '{}');
  } catch {
    return {};
  }
}

function saveProgress(assessmentId, payload) {
  const next = getInitialState();
  next[String(assessmentId)] = payload;
  localStorage.setItem('codesphere-assessment-progress', JSON.stringify(next));
}

function formatTime(seconds) {
  const mins = Math.max(0, Math.floor(seconds / 60));
  const secs = Math.max(0, seconds % 60);
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function LoadingSkeleton() {
  return (
    <div className="container fade-in" style={{ paddingBottom: 40 }}>
      <div className="card" style={{ padding: 24 }}>
        <div className="assessment-skeleton" />
        <div className="assessment-skeleton" />
        <div className="assessment-skeleton" />
      </div>
    </div>
  );
}

export default function AssessmentPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();
  const [assessment, setAssessment] = useState(null);
  const [session, setSession] = useState(null);
  const [sections, setSections] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeSection, setActiveSection] = useState(0);
  const [activeQuestion, setActiveQuestion] = useState(0);
  const [answers, setAnswers] = useState({});
  const [saveState, setSaveState] = useState('idle');
  const [timeLeft, setTimeLeft] = useState(0);
  const [showConfirm, setShowConfirm] = useState(false);
  const [fullscreenWarning, setFullscreenWarning] = useState(false);
  const [submissionPending, setSubmissionPending] = useState(false);
  const [submissionResult, setSubmissionResult] = useState(null);
  const [language, setLanguage] = useState('java');
  const [code, setCode] = useState(STARTER_CODE.java);
  const [uploading, setUploading] = useState(false);
  const [fileName, setFileName] = useState('');
  const [dragActive, setDragActive] = useState(false);
  const [codeVerdict, setCodeVerdict] = useState(null);
  const [codeOutput, setCodeOutput] = useState('');
  const saveTimerRef = useRef(null);
  const sessionRef = useRef(null);

  useEffect(() => {
    if (!isLoggedIn) {
      navigate('/login');
      return;
    }

    let active = true;
    const bootstrap = async () => {
      setLoading(true);
      setError('');
      try {
        const [assessmentRes, sectionsRes] = await Promise.all([
          assessmentAPI.getById(id),
          assessmentAPI.getSections(id),
        ]);

        if (!active) return;
        setAssessment(assessmentRes.data || null);
        setSections(sectionsRes.data || []);
        const saved = getInitialState()[String(id)] || {};
        setAnswers(saved.answers || {});
        setActiveSection(saved.activeSection || 0);
        setActiveQuestion(saved.activeQuestion || 0);
        setTimeLeft((assessmentRes.data?.durationMinutes || 60) * 60);

        try {
          const sessionRes = await assessmentAPI.startSession(id);
          if (!active) return;
          sessionRef.current = sessionRes.data || { id: null };
          setSession(sessionRes.data || { id: null });
        } catch (startErr) {
          // TODO: replace with real resume endpoint once backend exposes it.
          const fallback = await assessmentAPI.getSession(id).catch(() => null);
          if (active) {
            if (fallback?.data) {
              sessionRef.current = fallback.data;
              setSession(fallback.data);
            } else {
              throw startErr;
            }
          }
        }
      } catch (err) {
        if (active) {
          setError('We could not initialize the assessment session.');
          toast.error('Assessment session failed to load.');
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    bootstrap();
    return () => { active = false; };
  }, [id, isLoggedIn, navigate]);

  useEffect(() => {
    if (!session?.id && !assessment?.id) return;
    const tick = setInterval(() => {
      setTimeLeft((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(tick);
  }, [session, assessment]);

  useEffect(() => {
    if (!session?.id) return;
    const progressPayload = {
      assessmentId: id,
      started: true,
      activeSection,
      activeQuestion,
      answers,
      completed: false,
    };
    saveProgress(id, progressPayload);
  }, [activeSection, activeQuestion, answers, id, session]);

  useEffect(() => {
    const handleFullscreenChange = () => {
      if (!document.fullscreenElement) {
        setFullscreenWarning(true);
      }
    };
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange);
  }, []);

  useEffect(() => {
    if (assessment?.isFullscreenRequired) {
      document.documentElement.requestFullscreen?.().catch(() => {});
    }
  }, [assessment]);

  const persistAnswer = async (questionId, value) => {
    if (!session?.id) return;
    setSaveState('saving');
    try {
      await assessmentAPI.saveAnswer(session.id, questionId, value);
      setSaveState('saved');
    } catch {
      setSaveState('error');
    }
  };

  const debouncedSave = useMemo(() => {
    return (questionId, value) => {
      if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
      saveTimerRef.current = setTimeout(() => persistAnswer(questionId, value), 600);
    };
  }, [session]);

  const handleAnswerChange = (questionId, value) => {
    setAnswers((prev) => ({ ...prev, [questionId]: value }));
    if (session?.id) debouncedSave(questionId, value);
  };

  const navigateSection = (nextIndex) => {
    const target = sections[nextIndex];
    if (!target) return;
    const previousSection = sections[activeSection];
    const isSequential = previousSection?.navigationMode === 'SEQUENTIAL';
    if (isSequential && nextIndex > activeSection) {
      toast.error('This section is locked until the current one is completed.');
      return;
    }
    setActiveSection(nextIndex);
    setActiveQuestion(0);
    persistAnswer('__navigation__', JSON.stringify({ section: nextIndex, question: 0 }));
  };

  const handleSubmitSection = async () => {
    if (!session?.id) return;
    setSaveState('saving');
    try {
      await assessmentAPI.submitSession(id);
      toast.success('Section submitted.');
      setSaveState('saved');
    } catch {
      toast.error('Section submit is not supported by the current backend.');
    }
  };

  const handleSubmitAssessment = async () => {
    if (!session?.id) return;
    setSubmissionPending(true);
    try {
      const res = await assessmentAPI.submitAssessment(id, { answers });
      setSubmissionResult(res.data || { status: 'PENDING' });
      toast.success('Assessment submitted successfully.');
      navigate(`/assessments/${id}/result`);
    } catch {
      toast.error('Submission failed. The backend does not expose a full submission endpoint yet.');
      setSubmissionResult({ status: 'PENDING' });
      navigate(`/assessments/${id}/result`);
    } finally {
      setSubmissionPending(false);
    }
  };

  const handleCodingSubmit = async () => {
    if (!session?.id) return;
    try {
      const question = questions[activeQuestion];
      const res = await assessmentAPI.submitCodingAnswer(session.id, question?.id, { language, code });
      setCodeVerdict(res.data?.status || 'PENDING');
      setCodeOutput(res.data?.output || '');
    } catch {
      setCodeVerdict('ERROR');
      setCodeOutput('Coding submission is not supported by the current backend contract.');
    }
  };

  const handleUpload = async (file) => {
    if (!session?.id) return;
    const question = questions[activeQuestion];
    const formData = new FormData();
    formData.append('file', file);
    setUploading(true);
    try {
      const res = await assessmentAPI.uploadFile(session.id, question?.id, formData);
      setFileName(res.data?.fileName || file.name);
      toast.success('File uploaded.');
    } catch {
      toast.error('Upload endpoint is not available yet.');
    } finally {
      setUploading(false);
    }
  };

  const currentSection = sections[activeSection] || null;
  const currentQuestion = questions[activeQuestion] || null;

  if (loading) return <LoadingSkeleton />;
  if (error) {
    return (
      <div className="container fade-in">
        <div className="card empty-state">
          <div className="empty-icon">⚠️</div>
          <div className="empty-title">Assessment could not be loaded</div>
          <div className="empty-subtitle">{error}</div>
          <button className="btn btn-primary mt-4" onClick={() => window.location.reload()}>Retry</button>
        </div>
      </div>
    );
  }

  return (
    <div className="container fade-in" style={{ paddingBottom: 60 }}>
      {fullscreenWarning && (
        <div className="card" style={{ borderColor: 'rgba(245, 158, 11, 0.45)', marginBottom: 16 }}>
          <strong>Fullscreen warning:</strong> Please stay in fullscreen mode while the assessment is active.
        </div>
      )}

      <div className="card" style={{ padding: 20, marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <div>
            <div className="badge badge-tag">{assessment?.assessmentType || 'MIXED'}</div>
            <h1 style={{ fontSize: 24, fontWeight: 800, marginTop: 8 }}>{assessment?.title || 'Assessment'}</h1>
          </div>
          <div style={{ fontSize: 16, fontWeight: 700, color: 'var(--accent-light)' }}>
            ⏱ {formatTime(timeLeft)}
          </div>
        </div>
        <div style={{ marginTop: 12, color: 'var(--text-secondary)' }}>
          This is a client-side countdown display only. The server enforces the real deadline.
        </div>
      </div>

      <div className="assessment-shell">
        <aside className="assessment-sidebar">
          <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12 }}>Sections</h3>
          <div className="section-tabs">
            {sections.map((section, index) => (
              <button
                key={section.id}
                className={`section-tab ${index === activeSection ? 'active' : ''}`}
                onClick={() => navigateSection(index)}
                disabled={section.navigationMode === 'SEQUENTIAL' && index > activeSection}
              >
                <span>{section.title}</span>
                <span>{index < activeSection ? '✓' : '•'}</span>
              </button>
            ))}
          </div>

          <h3 style={{ fontSize: 15, fontWeight: 700, marginTop: 20, marginBottom: 12 }}>Questions</h3>
          <div className="question-grid">
            {questions.map((q, index) => {
              const value = answers[q.id];
              const state = value ? 'answered' : 'unanswered';
              return (
                <button
                  key={q.id}
                  className={`question-pill ${state}`}
                  onClick={() => setActiveQuestion(index)}
                >
                  {index + 1}
                </button>
              );
            })}
          </div>
        </aside>

        <section className="assessment-main">
          <div className="card" style={{ padding: 24 }}>
            <div className="card-header" style={{ marginBottom: 16 }}>
              <div>
                <div className="text-muted" style={{ fontSize: 12, textTransform: 'uppercase' }}>Section</div>
                <h2 style={{ fontSize: 20, fontWeight: 700 }}>{currentSection?.title || 'Section'}</h2>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-secondary btn-sm" onClick={() => setShowConfirm(true)}>Submit assessment</button>
                <button className="btn btn-primary btn-sm" onClick={handleSubmitSection}>Submit section</button>
              </div>
            </div>

            <div style={{ color: 'var(--text-secondary)', marginBottom: 12 }}>
              {saveState === 'saving' && 'Saving…'}
              {saveState === 'saved' && 'Saved just now'}
              {saveState === 'error' && 'Autosave failed'}
            </div>

            {currentQuestion ? (
              <div>
                <div style={{ marginBottom: 16 }}>
                  <div className="text-muted" style={{ fontSize: 12, textTransform: 'uppercase' }}>Question {activeQuestion + 1}</div>
                  <div style={{ fontSize: 18, fontWeight: 700, marginTop: 4 }}>{currentQuestion.prompt || currentQuestion.content || 'Question prompt'}</div>
                </div>

                {renderQuestion(currentQuestion, answers, handleAnswerChange, language, setLanguage, code, setCode, handleCodingSubmit, codeVerdict, codeOutput, uploading, fileName, dragActive, setDragActive, handleUpload)}
              </div>
            ) : (
              <div className="empty-state">
                <div className="empty-icon">📋</div>
                <div className="empty-title">No questions available</div>
                <div className="empty-subtitle">The backend does not expose a session snapshot yet.</div>
              </div>
            )}
          </div>
        </section>
      </div>

      {showConfirm && (
        <div className="modal-backdrop">
          <div className="card" style={{ maxWidth: 480, width: '100%' }}>
            <h3 style={{ fontSize: 20, fontWeight: 700, marginBottom: 12 }}>Submit assessment?</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: 16 }}>
              You have answered {Object.keys(answers).length} of {questions.length} questions. Are you sure you want to finish this assessment?
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button className="btn btn-secondary" onClick={() => setShowConfirm(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSubmitAssessment} disabled={submissionPending}>
                {submissionPending ? 'Submitting…' : 'Confirm submit'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function renderQuestion(currentQuestion, answers, onAnswerChange, language, setLanguage, code, setCode, onCodingSubmit, codeVerdict, codeOutput, uploading, fileName, dragActive, setDragActive, onUpload) {
  const type = currentQuestion?.questionType || currentQuestion?.type || 'MCQ_SINGLE';
  const value = answers[currentQuestion?.id] || '';
  const options = currentQuestion?.options || [];

  switch (type) {
    case QUESTION_TYPES.MCQ_SINGLE:
      return (
        <div className="question-stack">
          {options.map((option, index) => (
            <label key={option + index} className="choice-option">
              <input
                type="radio"
                name={`q-${currentQuestion.id}`}
                checked={value === option}
                onChange={() => onAnswerChange(currentQuestion.id, option)}
              />
              <span>{option}</span>
            </label>
          ))}
        </div>
      );
    case QUESTION_TYPES.MCQ_MULTI:
      return (
        <div className="question-stack">
          {(options || []).map((option, index) => {
            const selected = Array.isArray(value) ? value.includes(option) : false;
            return (
              <label key={option + index} className="choice-option">
                <input
                  type="checkbox"
                  checked={selected}
                  onChange={() => {
                    const next = Array.isArray(value) ? [...value] : [];
                    const exists = next.includes(option);
                    const updated = exists ? next.filter((entry) => entry !== option) : [...next, option];
                    onAnswerChange(currentQuestion.id, updated);
                  }}
                />
                <span>{option}</span>
              </label>
            );
          })}
        </div>
      );
    case QUESTION_TYPES.CODING:
      return (
        <div>
          <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
            <select className="select" value={language} onChange={(event) => setLanguage(event.target.value)}>
              {LANGUAGES.map((lang) => <option key={lang.value} value={lang.value}>{lang.label}</option>)}
            </select>
            <button className="btn btn-secondary btn-sm" onClick={() => setCode(STARTER_CODE[language] || '')}>Reset</button>
            <button className="btn btn-secondary btn-sm" onClick={onCodingSubmit}>Submit</button>
          </div>
          <div className="coding-container">
            <Editor
              height="320px"
              language={LANGUAGES.find((entry) => entry.value === language)?.monacoLan || 'javascript'}
              value={code}
              onChange={(next) => setCode(next || '')}
              theme="vs-dark"
            />
          </div>
          {codeVerdict && <div className="feedback-box correct">{codeVerdict}</div>}
          {codeOutput && <pre className="sample-code" style={{ marginTop: 12 }}>{codeOutput}</pre>}
        </div>
      );
    case QUESTION_TYPES.SUBJECTIVE:
    case QUESTION_TYPES.WRITTEN:
      return (
        <div>
          <textarea
            className="textarea"
            value={value}
            onChange={(event) => onAnswerChange(currentQuestion.id, event.target.value)}
            rows={10}
          />
          <div style={{ marginTop: 8, color: 'var(--text-secondary)' }}>
            Word count: {String(value || '').trim().split(/\s+/).filter(Boolean).length}
          </div>
        </div>
      );
    case QUESTION_TYPES.READING_COMPREHENSION:
      return (
        <div>
          <div className="card" style={{ background: 'rgba(10,10,18,0.7)', marginBottom: 16 }}>
            <div style={{ fontWeight: 700, marginBottom: 8 }}>Passage</div>
            <div style={{ color: 'var(--text-secondary)' }}>{currentQuestion.passageText || 'Reading passage goes here.'}</div>
          </div>
          <div className="question-stack">
            {(currentQuestion.subQuestions || []).map((subQuestion) => (
              <div key={subQuestion.id} className="card" style={{ padding: 16, marginBottom: 12 }}>
                <div style={{ fontWeight: 600, marginBottom: 8 }}>{subQuestion.prompt}</div>
                {renderQuestion(subQuestion, answers, onAnswerChange, language, setLanguage, code, setCode, onCodingSubmit, codeVerdict, codeOutput, uploading, fileName, dragActive, setDragActive, onUpload)}
              </div>
            ))}
          </div>
        </div>
      );
    case QUESTION_TYPES.FILE_UPLOAD:
      return (
        <div>
          <label
            className={`upload-zone ${dragActive ? 'active' : ''}`}
            onDragOver={(event) => { event.preventDefault(); setDragActive(true); }}
            onDragLeave={() => setDragActive(false)}
            onDrop={(event) => {
              event.preventDefault();
              setDragActive(false);
              const file = event.dataTransfer.files?.[0];
              if (file) onUpload(file);
            }}
          >
            <input type="file" style={{ display: 'none' }} onChange={(event) => event.target.files?.[0] && onUpload(event.target.files[0])} />
            <div style={{ fontWeight: 700, marginBottom: 8 }}>Drop a file here</div>
            <div style={{ color: 'var(--text-secondary)' }}>Upload a supported file for this question.</div>
          </label>
          {uploading && <div className="mt-4">Uploading…</div>}
          {fileName && <div className="mt-4">Uploaded: {fileName}</div>}
        </div>
      );
    default:
      return <div className="empty-state">TODO: render this question type.</div>;
  }
}
